<template>
  <div class="route-card card" @click="$emit('open', route)">
    <!-- Cover placeholder / image -->
    <div class="route-cover">
      <RouteCoverImage v-if="hasCover" :route="route" :alt="route.title" />
      <div v-else class="route-cover-placeholder">
        <span>{{ categoryGlyph }}</span>
      </div>
      <div class="route-card-top-actions">
        <button
          type="button"
          class="route-icon-btn"
          :class="{ active: favorite }"
          :aria-pressed="favorite"
          :aria-label="favorite ? 'Убрать из избранного' : 'В избранное'"
          @click.stop="$emit('toggleFavorite')"
        >
          {{ favorite ? '♥' : '♡' }}
        </button>
        <button
          type="button"
          class="route-icon-btn"
          aria-label="Поделиться"
          @click.stop="$emit('share')"
        >
          ↗
        </button>
      </div>
      <div class="route-cover-overlay">
        <span :class="['route-price-tag', route.isPaid ? '' : 'free']">
          {{ route.isPaid ? `${route.price} ₽` : 'Бесплатно' }}
        </span>
      </div>
    </div>

    <!-- Content -->
    <div class="route-content">
      <div class="route-category">{{ route.category }}</div>
      <h3 class="route-title">{{ route.title }}</h3>
      <p class="route-desc">{{ route.description }}</p>

      <div class="route-stats">
        <span class="route-stat">
          <span class="stat-icon">⏱</span> {{ route.duration }}
        </span>
        <span class="route-stat">
          <span class="stat-icon">⇢</span> {{ route.distance }}
        </span>
        <span class="route-stat">
          <span class="stat-icon">◎</span> {{ route.stops?.length || 0 }} ост.
        </span>
      </div>

      <div class="route-footer">
        <span class="route-price-inline">{{ route.isPaid ? `${route.price} ₽` : 'Бесплатно' }}</span>
        <div class="rating" v-if="route.rating">
          <span class="star">★</span> {{ route.rating.toFixed(1) }}
        </div>
        <span class="route-cta">Подробнее →</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import RouteCoverImage from '@/components/routes/RouteCoverImage.vue'
import { pickRouteCoverSource } from '@/utils/routeMedia.js'

const props = defineProps({
  route: Object,
  favorite: { type: Boolean, default: false },
})
defineEmits(['open', 'toggleFavorite', 'share'])

const hasCover = computed(() => !!pickRouteCoverSource(props.route))

const categoryGlyph = computed(() => {
  const map = {
    'Архитектура': '⬡', 'История': '◈',
    'Природа': '⊹', 'Культура': '◇'
  }
  return map[props.route.category] || '◎'
})
</script>

<style scoped>
.route-card { cursor: pointer; }

.route-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
  background: var(--gray-800);
}

.route-cover img { width: 100%; height: 100%; object-fit: cover; }

.route-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3.5rem;
  color: var(--gray-600);
  background: linear-gradient(135deg, var(--gray-800), var(--ink));
}

.route-card-top-actions {
  position: absolute;
  top: var(--spacing-sm);
  left: var(--spacing-sm);
  display: flex;
  gap: 0.35rem;
  z-index: 2;
}

.route-icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  border: 1px solid var(--gray-600);
  background: rgba(10, 10, 10, 0.75);
  color: var(--gray-300);
  font-size: 0.85rem;
  cursor: pointer;
  transition: border-color var(--transition), color var(--transition), background var(--transition);
}

.route-icon-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.route-icon-btn.active {
  color: #e8a0a0;
  border-color: rgba(232, 160, 160, 0.5);
}

.route-cover-overlay {
  position: absolute;
  top: var(--spacing-sm);
  right: var(--spacing-sm);
}

.route-price-tag {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  background: rgba(10,10,10,0.85);
  backdrop-filter: blur(4px);
  border: 1px solid var(--gray-600);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.06em;
  color: var(--gray-400);
}

.route-price-tag.free {
  border-color: var(--accent);
  color: var(--accent);
}

.route-content {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.route-category {
  font-size: 0.65rem;
  font-family: var(--font-mono);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--gray-400);
}

.route-title {
  font-family: var(--font-display);
  font-size: 1.2rem;
  line-height: 1.25;
}

.route-desc {
  font-size: 0.8rem;
  color: var(--gray-400);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.route-stats {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
  padding-top: var(--spacing-xs);
}

.route-stat {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.7rem;
  color: var(--gray-400);
}

.stat-icon { color: var(--accent); }

.route-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-xs);
}

.rating {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  color: var(--gray-400);
}

.route-price-inline {
  display: none;
}

.star { color: var(--accent); }

.route-cta {
  font-size: 0.7rem;
  font-family: var(--font-mono);
  letter-spacing: 0.06em;
  color: var(--accent);
  transition: letter-spacing var(--transition);
}

.route-card:hover .route-cta { letter-spacing: 0.12em; }
</style>
