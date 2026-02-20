<template>
  <div class="page-wrapper routes-page">
    <div class="container">

      <!-- Hero -->
      <section class="routes-hero">
        <p class="text-mono" style="color:var(--accent)">◈ Исследование города</p>
        <h1>Исторические<br />маршруты</h1>
        <p class="routes-subtitle">Готовые экскурсионные маршруты по историческим местам Астрахани — пешеходные, автомобильные и тематические.</p>
      </section>

      <!-- Filter bar -->
      <div class="routes-filter-bar">
        <div class="filter-group">
          <button
            v-for="cat in categories"
            :key="cat"
            :class="['filter-chip', { active: activeCategory === cat }]"
            @click="activeCategory = activeCategory === cat ? null : cat"
          >
            {{ cat }}
          </button>
        </div>
        <div class="filter-group">
          <button
            :class="['filter-chip', { active: showFree }]"
            @click="showFree = !showFree"
          >Бесплатные</button>
          <button
            :class="['filter-chip', { active: showPaid }]"
            @click="showPaid = !showPaid"
          >Платные</button>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="isLoading" class="routes-loading">
        <div class="spinner" />
        <span>Загрузка маршрутов…</span>
      </div>

      <!-- Routes grid -->
      <transition-group v-else name="slide-up" tag="div" class="routes-grid">
        <RouteCard
          v-for="r in filteredRoutes"
          :key="r.id"
          :route="r"
          @open="openRoute(r)"
        />
      </transition-group>

      <div v-if="!isLoading && !filteredRoutes.length" class="empty-state">
        <p>Маршруты не найдены</p>
      </div>

    </div>

    <!-- Route detail modal -->
    <transition name="fade">
      <div v-if="selected" class="modal-backdrop" @click.self="selected = null">
        <div class="modal-box route-modal">
          <button class="modal-close-btn" @click="selected = null">✕</button>

          <span :class="['tag', selected.isPaid ? '' : 'tag-accent']">
            {{ selected.isPaid ? `${selected.price} ₽` : 'Бесплатно' }}
          </span>
          <h2 style="margin: 0.75rem 0 0.5rem">{{ selected.title }}</h2>
          <div class="route-meta-row">
            <span>{{ selected.category }}</span>
            <span>·</span>
            <span>{{ selected.duration }}</span>
            <span>·</span>
            <span>{{ selected.distance }}</span>
            <span>·</span>
            <span>{{ selected.stops?.length || 0 }} остановок</span>
          </div>

          <div class="divider" />
          <p class="route-desc">{{ selected.description }}</p>

          <!-- Stops list -->
          <div v-if="selected.stops?.length" class="stops-list">
            <p class="text-mono" style="color:var(--gray-400);margin-bottom:0.75rem">Остановки маршрута</p>
            <div class="stops-timeline">
              <div
                v-for="(stop, i) in selected.stops"
                :key="i"
                class="stop-item"
              >
                <div class="stop-dot">{{ i + 1 }}</div>
                <div>
                  <p class="stop-name">{{ stop.name }}</p>
                  <p class="stop-desc">{{ stop.description }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="route-modal-actions">
            <button v-if="selected.isPaid" class="btn btn-primary btn-lg">
              Купить за {{ selected.price }} ₽
            </button>
            <button v-else class="btn btn-accent btn-lg">
              Начать маршрут
            </button>
            <button class="btn btn-ghost" @click="selected = null">Закрыть</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import RouteCard from '@/components/routes/RouteCard.vue'
import { useToastStore } from '@/store/index.js'

const toastStore = useToastStore()
const routes = ref([])
const selected = ref(null)
const isLoading = ref(false)
const activeCategory = ref(null)
const showFree = ref(false)
const showPaid = ref(false)

// 🔹 categories: защита от не-массива
const categories = computed(() => {
  if (!Array.isArray(routes.value)) return []
  return [...new Set(routes.value.map(r => r.category).filter(Boolean))]
})

// 🔹 filteredRoutes: защита от не-массива
const filteredRoutes = computed(() => {
  if (!Array.isArray(routes.value)) return []

  let list = routes.value
  if (activeCategory.value) list = list.filter(r => r.category === activeCategory.value)
  if (showFree.value && !showPaid.value) list = list.filter(r => !r.isPaid)
  if (showPaid.value && !showFree.value) list = list.filter(r => r.isPaid)
  return list
})

onMounted(async () => {
  isLoading.value = true
  try {
    // Try Java API first
    const res = await fetch('/java-api/api/v1/routes?published=true')

    if (res.ok) {
      const json = await res.json()

      // 🔹 Java API возвращает { status, data } — извлекаем data!
      if (json?.status === 'success' && Array.isArray(json.data)) {
        routes.value = json.data
      } else if (Array.isArray(json)) {
        // Если вдруг вернули сразу массив
        routes.value = json
      } else {
        console.warn('Unexpected API response format:', json)
        routes.value = getMockRoutes()
      }
    } else {
      throw new Error('Java API unavailable')
    }
  } catch (err) {
    console.warn('Java API failed, trying fallback:', err)
    try {
      const res = await fetch('/api/routes')
      if (res.ok) {
        const json = await res.json()
        routes.value = Array.isArray(json) ? json : getMockRoutes()
        return
      }
    } catch {}
    // Fallback на mock-данные
    routes.value = getMockRoutes()
  } finally {
    isLoading.value = false
  }
})

function openRoute(r) { selected.value = r }

function getMockRoutes() {
  return [
    {
      id: 1,
      title: 'Кремль и его окрестности',
      category: 'Архитектура',
      description: 'Прогулка по историческому центру Астрахани: Кремль, Успенский и Троицкий соборы, Пречистенские ворота. Узнайте о многовековой истории главной цитадели Поволжья.',
      duration: '2–3 часа',
      distance: '2.5 км',
      isPaid: false,
      price: null,
      coverImage: null,
      rating: 4.8,
      stops: [
        { name: 'Астраханский Кремль', description: 'Главная достопримечательность города' },
        { name: 'Успенский собор', description: 'Шедевр русского барокко XVII в.' },
        { name: 'Троицкий собор', description: 'Один из старейших храмов края' },
        { name: 'Пречистенские ворота', description: 'Главный въезд в кремль' }
      ]
    },
    {
      id: 2,
      title: 'Купеческая Астрахань',
      category: 'История',
      description: 'Маршрут по особнякам купцов XIX века, армянским и индийским торговым рядам. Откройте для себя Астрахань как перекрёсток торговых путей Востока и Запада.',
      duration: '3–4 часа',
      distance: '4 км',
      isPaid: true,
      price: 450,
      coverImage: null,
      rating: 4.6,
      stops: [
        { name: 'Гостиный двор', description: 'Центр торговой жизни XIX века' },
        { name: 'Индийское подворье', description: 'След Великого Шёлкового пути' },
        { name: 'Армянский квартал', description: 'Диаспора купцов-меценатов' },
        { name: 'Особняк Губина', description: 'Жемчужина купеческого модерна' }
      ]
    },
    {
      id: 3,
      title: 'Зодчество Астраханского края',
      category: 'Архитектура',
      description: 'Специализированный маршрут для ценителей архитектуры: деревянное зодчество, каменные палаты, советский конструктивизм и постройки эпохи эклектики.',
      duration: '4–5 часов',
      distance: '5.5 км',
      isPaid: true,
      price: 800,
      coverImage: null,
      rating: 4.9,
      stops: [
        { name: 'Деревянные дома Криуши', description: 'Уникальная резьба XIX века' },
        { name: 'Здание Белого города', description: 'Образец советского ар-деко' },
        { name: 'Дом Тетюшинова', description: 'Памятник деревянного зодчества' }
      ]
    },
    {
      id: 4,
      title: 'Природа дельты Волги',
      category: 'Природа',
      description: 'Пешеходно-водная прогулка по уникальной экосистеме дельты Волги — лотосовые поля, птичьи колонии и рыбацкие сёла.',
      duration: '5–6 часов',
      distance: '8 км',
      isPaid: true,
      price: 1200,
      coverImage: null,
      rating: 4.7,
      stops: [
        { name: 'Лотосовые поля', description: 'Крупнейшие в Европе заросли лотоса' },
        { name: 'Птичья колония', description: 'Более 200 видов птиц' },
        { name: 'Рыбацкое село', description: 'Традиционный уклад жизни' }
      ]
    },
    {
      id: 5,
      title: 'Советская Астрахань',
      category: 'История',
      description: 'Конструктивизм, монументальная скульптура и градостроительство советской эпохи в исторической перспективе.',
      duration: '2 часа',
      distance: '3 км',
      isPaid: false,
      price: null,
      coverImage: null,
      rating: 4.3,
      stops: [
        { name: 'Площадь Ленина', description: 'Центр советского города' },
        { name: 'Здание ОГПУ', description: 'Конструктивизм 1930-х' },
        { name: 'Речной вокзал', description: 'Ворота Каспия' }
      ]
    },
    {
      id: 6,
      title: 'Религиозное многообразие',
      category: 'Культура',
      description: 'Мечети, православные храмы, армянская церковь, буддийский хурул и синагога в одном городе — Астрахань как символ межконфессионального диалога.',
      duration: '3 часа',
      distance: '3.5 км',
      isPaid: false,
      price: null,
      coverImage: null,
      rating: 4.5,
      stops: [
        { name: 'Белая мечеть', description: 'Главная мечеть Астрахани' },
        { name: 'Армянская церковь', description: 'XVIII век' },
        { name: 'Буддийский хурул', description: 'Единственный в Поволжье' }
      ]
    }
  ]
}
</script>

<style scoped>
.routes-page { padding-bottom: var(--spacing-2xl); }

.routes-hero {
  padding: var(--spacing-2xl) 0 var(--spacing-xl);
  max-width: 640px;
}

.routes-subtitle {
  margin-top: var(--spacing-md);
  color: var(--gray-400);
  font-size: 0.9rem;
  line-height: 1.8;
}

/* Filter bar */
.routes-filter-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  flex-wrap: wrap;
  padding: var(--spacing-lg) 0;
  border-top: 1px solid var(--gray-800);
  border-bottom: 1px solid var(--gray-800);
  margin-bottom: var(--spacing-xl);
}

.filter-group { display: flex; flex-wrap: wrap; gap: var(--spacing-xs); }

.filter-chip {
  padding: 0.35rem 0.8rem;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.06em;
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--gray-400);
  cursor: pointer;
  transition: all var(--transition);
}

.filter-chip:hover { border-color: var(--gray-400); color: var(--paper); }
.filter-chip.active { border-color: var(--accent); color: var(--accent); background: rgba(200,169,110,0.08); }

/* Grid */
.routes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-lg);
}

.routes-loading {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  color: var(--gray-400);
  padding: var(--spacing-xl) 0;
}

.empty-state {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--gray-400);
}

/* Route modal */
.route-modal { max-width: 660px; position: relative; }

.modal-close-btn {
  position: absolute;
  top: var(--spacing-md);
  right: var(--spacing-md);
  background: transparent;
  border: 1px solid var(--gray-600);
  color: var(--paper);
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition);
}

.modal-close-btn:hover { border-color: var(--paper); }

.route-meta-row {
  display: flex;
  gap: var(--spacing-sm);
  color: var(--gray-400);
  font-size: 0.75rem;
  flex-wrap: wrap;
}

.route-desc {
  color: var(--gray-200);
  line-height: 1.8;
  font-size: 0.875rem;
  margin-bottom: var(--spacing-lg);
}

/* Stops timeline */
.stops-list { margin-bottom: var(--spacing-xl); }

.stops-timeline { display: flex; flex-direction: column; gap: 0; }

.stop-item {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--gray-800);
  position: relative;
}

.stop-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--gray-800);
  border: 1px solid var(--accent);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  flex-shrink: 0;
}

.stop-name { font-weight: 500; font-size: 0.875rem; margin-bottom: 2px; }
.stop-desc { font-size: 0.75rem; color: var(--gray-400); }

.route-modal-actions {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
  margin-top: var(--spacing-lg);
}

@media (max-width: 768px) {
  .routes-grid { grid-template-columns: 1fr; }
  .routes-filter-bar { gap: var(--spacing-md); }
}
</style>
