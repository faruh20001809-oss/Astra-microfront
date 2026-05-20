<template>
  <Dialog
    :visible="visible"
    modal
    header="Предложить точку на карту"
    :style="{ width: 'min(420px, 92vw)' }"
    class="suggest-poi-dialog"
    :dismissableMask="true"
    @update:visible="$emit('update:visible', $event)"
    @hide="onHide"
  >
    <form class="suggest-poi-form" @submit.prevent="submit">
      <div class="form-group">
        <label class="form-label">Название *</label>
        <input v-model="form.name" type="text" class="form-input" placeholder="Например: Дом-музей Кустодиева" required />
      </div>
      <div class="form-group">
        <label class="form-label">Место *</label>
        <input v-model="form.place" type="text" class="form-input" placeholder="Адрес или описание места" required />
      </div>
      <div class="form-group">
        <label class="form-label">Описание</label>
        <textarea v-model="form.description" class="form-textarea" placeholder="Краткое описание объекта…" rows="3" />
      </div>
      <div class="form-group">
        <label class="form-label">Почему добавить *</label>
        <textarea v-model="form.whyAdd" class="form-textarea" placeholder="Почему это место важно?" required rows="3" />
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>
      <p v-if="success" class="form-success">Предложение отправлено. Мы рассмотрим его и при одобрении добавим точку.</p>
      <div class="form-actions">
        <Button type="submit" label="Отправить" :loading="submitting" :disabled="submitting" />
        <Button type="button" label="Отмена" class="p-button-text p-button-secondary" @click="close" />
      </div>
    </form>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import { javaApi } from '@/api/backend.js'
import { useToastStore } from '@/store/index.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['update:visible', 'submitted'])

const toastStore = useToastStore()
const form = ref({ name: '', place: '', description: '', whyAdd: '' })
const error = ref('')
const success = ref(false)
const submitting = ref(false)

function close() {
  emit('update:visible', false)
}

function onHide() {
  error.value = ''
}

watch(
  () => props.visible,
  (open) => {
    if (!open) {
      success.value = false
      error.value = ''
    }
  },
)

async function submit() {
  error.value = ''
  success.value = false
  submitting.value = true
  try {
    await javaApi.poiSuggestions.submit({
      name: form.value.name.trim(),
      place: form.value.place.trim(),
      description: form.value.description?.trim() || undefined,
      whyAdd: form.value.whyAdd.trim(),
    })
    success.value = true
    form.value = { name: '', place: '', description: '', whyAdd: '' }
    toastStore.push('Предложение отправлено', 'info')
    emit('submitted')
    setTimeout(() => {
      close()
      success.value = false
    }, 2000)
  } catch (e) {
    error.value = e.message || 'Не удалось отправить. Попробуйте позже.'
  } finally {
    submitting.value = false
  }
}
</script>
