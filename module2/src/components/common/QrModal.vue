<template>
  <teleport to="body">
    <transition name="fade">
      <div v-if="modelValue" class="qr-modal-backdrop" @click.self="close">
        <div class="qr-modal-box card">
          <button type="button" class="qr-modal-close" aria-label="Закрыть" @click="close">✕</button>
          <p class="text-mono" style="color:var(--accent);font-size:0.7rem;margin-bottom:0.5rem">QR-код</p>
          <p class="qr-hint">Отсканируйте, чтобы открыть маршрут</p>
          <div class="qr-canvas-wrap">
            <img v-if="dataUrl" :src="dataUrl" alt="QR-код со ссылкой" class="qr-img" />
            <div v-else class="spinner" />
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  text: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const dataUrl = ref('')

function close() {
  emit('update:modelValue', false)
}

watch(
  () => [props.modelValue, props.text],
  async ([open, text]) => {
    if (!open || !text) {
      dataUrl.value = ''
      return
    }
    try {
      dataUrl.value = await QRCode.toDataURL(text, { margin: 2, width: 220, color: { dark: '#1a1a1a', light: '#ffffff' } })
    } catch {
      dataUrl.value = ''
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.qr-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 10050;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.qr-modal-box {
  position: relative;
  padding: 1.5rem;
  max-width: 320px;
  background: var(--paper, #f5f0e6);
  color: var(--ink, #1a1a1a);
}

.qr-modal-close {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  background: transparent;
  border: 1px solid var(--gray-600, #666);
  color: inherit;
  width: 28px;
  height: 28px;
  cursor: pointer;
  border-radius: var(--radius-sm, 4px);
}

.qr-hint { margin: 0 0 1rem; font-size: 0.85rem; }

.qr-canvas-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
}

.qr-img { display: block; max-width: 100%; height: auto; }
</style>
