import { defineStore } from 'pinia'

/* ─────────────────────────────────────────
   Cart Store
───────────────────────────────────────── */
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    isOpen: false
  }),
  getters: {
    totalCount: (state) => state.items.reduce((sum, i) => sum + (i.qty || 1), 0),
    totalPrice: (state) => state.items.reduce((sum, i) => sum + (i.price || 0) * (i.qty || 1), 0)
  },
  actions: {
    addItem(product) {
      const found = this.items.find(i => i.id === product.id)
      if (found) {
        found.qty = (found.qty || 1) + 1
      } else {
        this.items.push({ ...product, qty: 1 })
      }
    },
    removeItem(id) {
      this.items = this.items.filter(i => i.id !== id)
    },
    updateQty(id, qty) {
      const item = this.items.find(i => i.id === id)
      if (item) {
        if (qty <= 0) this.removeItem(id)
        else item.qty = qty
      }
    },
    clearCart() {
      this.items = []
    },
    toggleCart() {
      this.isOpen = !this.isOpen
    }
  }
})

/* ─────────────────────────────────────────
   Map Store
───────────────────────────────────────── */
export const useMapStore = defineStore('map', {
  state: () => ({
    pois: [],
    selectedPoi: null,
    activeFilters: [],
    userLocation: null,
    isLoadingPois: false,
    nearbyPoi: null
  }),


  getters: {
    filteredPois: (state) => {
      if (!Array.isArray(state.pois)) return []
      if (state.activeFilters.length === 0) return state.pois
      return state.pois.filter(poi => state.activeFilters.includes(poi.category))
    },

    categories: (state) => {
      if (!Array.isArray(state.pois)) return []
      return [...new Set(state.pois.map(p => p.category).filter(Boolean))]
    }
  },

  actions: {
    async fetchPois() {
      this.isLoadingPois = true
      try {
        const res = await fetch('/java-api/api/v1/pois?status=PUBLISHED')
    
        const contentType = res.headers.get('content-type')
        if (!contentType || !contentType.includes('application/json')) {
          throw new Error('API returned non-JSON response')
        }
    
        const json = await res.json()
    
        const raw = json?.status === 'success' && Array.isArray(json.data)
          ? json.data
          : Array.isArray(json)
            ? json
            : []
    
        // 🔹 Нормализуем под фронт (lat/lng и дополнительные поля)
        this.pois = raw.map(p => ({
          id: p.id,
          name: p.name,
          description: p.description,
          category: p.category,
          // 2ГИС ожидает lon/lat — мы даём lat/lng в объекте POI
          lat: p.latitude ?? p.coordinates?.latitude ?? 0,
          lng: p.longitude ?? p.coordinates?.longitude ?? 0,
          address: p.address,
          rating: p.rating,
          reviewsCount: p.reviewsCount,
          year: p.extendedInfo?.foundedYear ?? null,
          architect: p.extendedInfo?.architect ?? null,
          image: p.image || null,
          tags: p.tags || [],
        }))
      } catch (err) {
        console.error('Failed to fetch POIs:', err)
        this.pois = getMockPois()
      } finally {
        this.isLoadingPois = false
      }
    },

    setSelected(poi) {
      this.selectedPoi = poi
    },

    clearSelected() {
      this.selectedPoi = null
    },

    setUserLocation(coords) {
      this.userLocation = coords
      this.checkGeoTriggers()
    },

    toggleFilter(category) {
      const idx = this.activeFilters.indexOf(category)
      if (idx === -1) this.activeFilters.push(category)
      else this.activeFilters.splice(idx, 1)
    },

    checkGeoTriggers() {
      if (!this.userLocation || !Array.isArray(this.pois)) return
      const { lat, lng } = this.userLocation
      const TRIGGER_RADIUS = 150 // metres

      for (const poi of this.pois) {
        const dist = haversine(lat, lng, poi.lat, poi.lng)
        if (dist <= TRIGGER_RADIUS) {
          if (!this.nearbyPoi || this.nearbyPoi.id !== poi.id) {
            this.nearbyPoi = poi
          }
          return
        }
      }
      this.nearbyPoi = null
    }
  }
})

/* ─────────────────────────────────────────
   Toast Store
───────────────────────────────────────── */
export const useToastStore = defineStore('toast', {
  state: () => ({ toasts: [] }),
  actions: {
    push(msg, type = 'info', duration = 3500) {
      const id = Date.now()
      this.toasts.push({ id, msg, type })
      setTimeout(() => this.remove(id), duration)
    },
    remove(id) {
      this.toasts = this.toasts.filter(t => t.id !== id)
    }
  }
})

/* ─────────────────────────────────────────
   Helpers
───────────────────────────────────────── */
function haversine(lat1, lon1, lat2, lon2) {
  const R = 6371000
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLon = (lon2 - lon1) * Math.PI / 180
  const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function getMockPois() {
  return [
    {
      id: 1, lat: 46.3497, lng: 48.0408,
      name: 'Астраханский Кремль',
      category: 'архитектура',
      description: 'Астраханский кремль — выдающийся памятник русского оборонного зодчества XVI–XVII веков.',
      year: 1582,
      architect: 'Михаил Вельяминов',
      photos: []
    },
    {
      id: 2, lat: 46.3453, lng: 48.0331,
      name: 'Успенский собор',
      category: 'архитектура',
      description: 'Главный православный храм Астраханской епархии, построенный в стиле русского барокко.',
      year: 1710,
      architect: 'Дорофей Мякишев',
      photos: []
    },
    {
      id: 3, lat: 46.3512, lng: 48.0452,
      name: 'Астраханский государственный музей-заповедник',
      category: 'музеи',
      description: 'Один из крупнейших музеев Поволжья с богатой коллекцией по истории и культуре края.',
      year: 1837,
      architect: null,
      photos: []
    },
    {
      id: 4, lat: 46.3479, lng: 48.0387,
      name: 'Дом-музей Ульяновых',
      category: 'музеи',
      description: 'Исторический дом, связанный с семьёй Ульяновых.',
      year: 1924,
      architect: null,
      photos: []
    },
    {
      id: 5, lat: 46.3530, lng: 48.0310,
      name: 'Братский сад',
      category: 'парки',
      description: 'Старейший парк Астрахани, разбитый в честь воинов, павших в Первую мировую войну.',
      year: 1915,
      architect: null,
      photos: []
    }
  ]
}