<template>
  <div class="museum-virtual-page">
    <header class="museum-virtual-head" aria-labelledby="museum-virtual-title">
      <h1 id="museum-virtual-title">Виртуальный музей</h1>
      <p class="museum-virtual-intro">
        Проект сохраняет уникальное деревянное зодчество Астраханской области в виртуальном
        и общедоступном формате. Выберите объект в каталоге ниже — на странице точки доступна
        динамическая карта 2ГИС (в каталоге — только просмотр) и на странице точки — полное управление картой.
      </p>
      <button type="button" class="museum-virtual-suggest" @click="suggestOpen = true">
        💡 Предложить точку на карту
      </button>
    </header>

    <section id="museum-map" class="museum-feature-section" aria-label="Превью выбранного объекта">
      <div class="museum-feature-card">
        <div class="museum-feature-map">
          <PoiInteractiveMap2gis
            v-if="panelPoi && poiHasMapCoords(panelPoi)"
            :key="`panel-${panelPoi.id}`"
            readonly
            :lat="Number(panelPoi.lat)"
            :lng="Number(panelPoi.lng)"
            :zoom="17"
            :aria-label="`Карта: ${panelPoi.name}`"
          />
          <div v-else class="museum-feature-map__fallback" aria-hidden="true" />
        </div>

        <aside
          v-if="panelPoi"
          class="museum-feature-panel"
          aria-labelledby="museum-panel-title"
        >
          <img
            v-if="panelPoi.image"
            class="museum-feature-panel__photo"
            :src="panelPoi.image"
            :alt="panelPoi.name"
            loading="lazy"
          />
          <h2 id="museum-panel-title" class="museum-feature-panel__title">{{ panelPoi.name }}</h2>
          <p class="museum-feature-panel__line">{{ museumPoiAddressLine(panelPoi) }}</p>
          <p v-if="panelPoi.style" class="museum-feature-panel__line">
            <strong>Стиль:</strong> {{ panelPoi.style }}
          </p>
          <p class="museum-feature-panel__desc">{{ panelBriefText(panelPoi) }}</p>
          <router-link
            :to="{ name: 'poi-details', params: { id: panelPoi.id } }"
            class="museum-feature-panel__cta"
          >
            Подробнее →
          </router-link>
        </aside>
        <aside v-else class="museum-feature-panel museum-feature-panel--empty" aria-live="polite">
          <p>{{ mapStore.isLoadingPois ? 'Загрузка объектов…' : 'Выберите объект в каталоге ниже' }}</p>
        </aside>
      </div>
    </section>

    <section class="museum-map-recs" aria-labelledby="museum-recs-title">
      <h2 id="museum-recs-title">Обязательно посмотрите</h2>
      <p v-if="mapStore.isLoadingPois" class="museum-map-recs__status text-mono">Загрузка объектов…</p>
      <p v-else-if="!featuredRecs.length" class="museum-map-recs__status text-mono">Объекты скоро появятся.</p>
      <div v-else class="museum-map-recs__grid">
        <article
          v-for="poi in featuredRecs"
          :key="poi.id"
          class="museum-poi-card"
          :class="{ 'museum-poi-card--active': selectedId === poi.id }"
          @click="selectPoi(poi)"
        >
          <div class="museum-poi-card__media">
            <img
              v-if="poi.image"
              :src="poi.image"
              :alt="poi.name"
              class="museum-poi-card__photo"
              loading="lazy"
            />
            <PoiInteractiveMap2gis
              v-else-if="poiHasMapCoords(poi)"
              :key="`card-${poi.id}`"
              readonly
              compact
              :lat="Number(poi.lat)"
              :lng="Number(poi.lng)"
              :zoom="16"
              :aria-label="`Карта: ${poi.name}`"
            />
            <div v-else class="museum-poi-card__media-fallback" aria-hidden="true" />
          </div>
          <div class="museum-poi-card__body">
            <h3>{{ poi.name }}</h3>
            <p>{{ poi.address || 'Адрес не указан' }}</p>
            <router-link
              :to="{ name: 'poi-details', params: { id: poi.id } }"
              class="museum-poi-card__cta"
              @click.stop
            >
              Подробнее →
            </router-link>
          </div>
        </article>
      </div>
    </section>

    <SuggestPoiDialog v-model:visible="suggestOpen" @submitted="onSuggestSubmitted" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMapStore } from '@/store/index.js'
import { poiHasMapCoords } from '@/utils/poiStaticMap.js'
import PoiInteractiveMap2gis from '@/components/poi/PoiInteractiveMap2gis.vue'
import SuggestPoiDialog from '@/components/poi/SuggestPoiDialog.vue'

const route = useRoute()
const mapStore = useMapStore()
const suggestOpen = ref(false)
const selectedId = ref(null)

const catalogPois = computed(() => (Array.isArray(mapStore.pois) ? mapStore.pois : []))
const featuredRecs = computed(() => catalogPois.value.slice(0, 4))

const panelPoi = computed(() => {
  if (selectedId.value != null) {
    return catalogPois.value.find((p) => p.id === selectedId.value) || null
  }
  return featuredRecs.value[0] || catalogPois.value[0] || null
})

function museumPoiAddressLine(poi) {
  if (!poi) return ''
  const addr = poi.address?.trim() || '—'
  const lat = Number(poi.lat)
  const lng = Number(poi.lng)
  const hasCoords = Number.isFinite(lat) && Number.isFinite(lng) && (lat !== 0 || lng !== 0)
  return hasCoords
    ? `Адрес: ${addr}; ${lat.toFixed(6)}, ${lng.toFixed(6)}`
    : `Адрес: ${addr}`
}

function panelBriefText(poi) {
  return poi?.shortDescription || poi?.description || poi?.mapDescription || ''
}

function selectPoi(poi) {
  selectedId.value = poi.id
  mapStore.setSelected(poi)
}

function applyPoiFromQuery() {
  const raw = route.query.poi
  if (raw == null || raw === '') return
  const poi = catalogPois.value.find((p) => String(p.id) === String(raw))
  if (poi) selectPoi(poi)
}

async function onSuggestSubmitted() {
  await mapStore.fetchPois({ force: true })
  if (!selectedId.value && catalogPois.value[0]) selectPoi(catalogPois.value[0])
}

onMounted(async () => {
  await mapStore.fetchPois()
  if (!selectedId.value) {
    const first = featuredRecs.value[0] || catalogPois.value[0]
    if (first) selectPoi(first)
  }
  applyPoiFromQuery()
})

watch(
  () => [route.query.poi, catalogPois.value.length],
  () => applyPoiFromQuery(),
)
</script>

<style scoped>
.museum-virtual-page {
  flex: 1 0 auto;
  background: #fff;
  color: #1d1d1b;
  font-family: Arial, Helvetica, sans-serif;
}

#app-root.app-dark .museum-virtual-page,
body.app-dark-theme .museum-virtual-page {
  background: #fff;
  color: #1d1d1b;
}

.museum-virtual-head {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: calc(var(--nav-h, 64px) + 1rem) 1.5rem 1.25rem;
  box-sizing: border-box;
}

.museum-virtual-head h1 {
  margin: 0 0 0.75rem;
  font-size: clamp(2rem, 2vw + 1rem, 3.4rem);
  font-weight: 900;
  line-height: 0.98;
}

.museum-virtual-intro {
  margin: 0 0 1rem;
  max-width: 52em;
  font-size: clamp(0.95rem, 0.22vw + 0.9rem, 1.05rem);
  line-height: 1.22;
}

.museum-virtual-suggest {
  display: inline-flex;
  align-items: center;
  min-height: 2.2rem;
  padding: 0 1rem;
  border: 2px solid #1d1d1b;
  border-radius: 999px;
  background: #fff;
  color: #1d1d1b;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.museum-virtual-suggest:hover {
  background: #1d1d1b;
  color: #fff;
}

.museum-feature-section {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto 2rem;
  padding: 0 1.5rem;
  box-sizing: border-box;
}

.museum-feature-card {
  display: grid;
  grid-template-columns: minmax(0, 300px) minmax(0, 1fr);
  width: 100%;
  min-width: 0;
  background: #fff;
  border-radius: 2px;
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.museum-feature-map {
  height: 180px;
  max-height: 180px;
  min-width: 0;
  background: #e8e8e8;
}

.museum-feature-map :deep(.poi-interactive-2gis) {
  width: 100%;
  height: 100%;
  min-height: 0;
  border-radius: 0;
  box-shadow: none;
}

.museum-feature-map :deep(.poi-interactive-2gis__canvas) {
  height: 180px;
  max-height: 180px;
  min-height: 0;
  aspect-ratio: auto;
}

.museum-feature-map__fallback {
  width: 100%;
  height: 180px;
  background: linear-gradient(160deg, #ececec, #d4d4d4);
}

.museum-feature-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 1.25rem 1.35rem 1.5rem;
  border-left: 1px solid rgba(0, 0, 0, 0.08);
  overflow-y: auto;
}

.museum-feature-panel__photo {
  width: 100%;
  aspect-ratio: 1.35 / 1;
  object-fit: cover;
  margin-bottom: 1rem;
  display: block;
}

.museum-feature-panel__title {
  margin: 0 0 0.5rem;
  font-size: 1.35rem;
  font-weight: 900;
  line-height: 1.05;
}

.museum-feature-panel__line {
  margin: 0 0 0.35rem;
  font-size: 0.88rem;
  line-height: 1.2;
}

.museum-feature-panel__desc {
  margin: 0.75rem 0 1rem;
  flex: 1 1 auto;
  font-size: 0.9rem;
  line-height: 1.22;
}

.museum-feature-panel__cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
  min-height: 2.65rem;
  padding: 0 1.35rem;
  border-radius: 999px;
  background: #1d1d1b;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 700;
  text-decoration: none;
}

.museum-feature-panel__cta:hover {
  background: #000;
}

.museum-feature-panel--empty {
  justify-content: center;
  align-items: center;
  color: #5f5f5f;
  font-size: 0.9rem;
}

.museum-map-recs {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 1.5rem 2.5rem;
  box-sizing: border-box;
}

.museum-map-recs h2 {
  margin: 0 0 1rem;
  font-size: clamp(2rem, 2vw + 1rem, 3.4rem);
  font-weight: 900;
  line-height: 0.98;
}

.museum-map-recs__status {
  margin: 0;
  color: #5f5f5f;
  font-size: 0.85rem;
}

.museum-map-recs__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
}

.museum-poi-card {
  display: grid;
  min-width: 0;
  background: #191716;
  color: #fff;
  padding: 1rem;
  cursor: pointer;
}

.museum-poi-card--active {
  outline: 2px solid #1d1d1b;
  outline-offset: 2px;
}

.museum-poi-card__media {
  aspect-ratio: 2.3 / 1;
  max-height: 140px;
  background: #c9c9c9;
  overflow: hidden;
}

.museum-poi-card__photo {
  width: 100%;
  height: 100%;
  min-height: 120px;
  max-height: 140px;
  object-fit: cover;
  display: block;
}

.museum-poi-card__media :deep(.poi-interactive-2gis) {
  width: 100%;
  height: 100%;
  min-height: 120px;
  border-radius: 0;
  box-shadow: none;
}

.museum-poi-card__media :deep(.poi-interactive-2gis__canvas) {
  min-height: 120px;
  height: 100%;
}

.museum-poi-card__media-fallback {
  width: 100%;
  height: 100%;
  min-height: 120px;
  background: linear-gradient(160deg, #ececec, #d4d4d4);
}

.museum-poi-card__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 0.3rem 1rem;
  margin-top: 0.75rem;
}

.museum-poi-card__body h3,
.museum-poi-card__body p {
  grid-column: 1 / 2;
  margin: 0;
}

.museum-poi-card__body h3 {
  font-size: 1rem;
  line-height: 1.05;
}

.museum-poi-card__body p {
  font-size: 0.9rem;
  opacity: 0.9;
}

.museum-poi-card__cta {
  grid-column: 2 / 3;
  grid-row: 1 / 3;
  align-self: end;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2rem;
  padding: 0 1rem;
  border-radius: 999px;
  background: #fff;
  color: #1d1d1b;
  font-size: 0.78rem;
  font-weight: 700;
  text-decoration: none;
}

.museum-poi-card__cta:hover {
  background: #d4d4d4;
}

@media (min-width: 1200px) {
  .museum-feature-card {
    grid-template-rows: minmax(400px, 520px);
  }
}

@media (max-width: 900px) {
  .museum-virtual-head,
  .museum-feature-section,
  .museum-map-recs {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .museum-feature-card {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(280px, 45vw) auto;
  }

  .museum-feature-panel {
    border-left: 0;
    border-top: 1px solid rgba(0, 0, 0, 0.08);
  }

  .museum-map-recs__grid {
    grid-template-columns: 1fr;
  }
}
</style>
