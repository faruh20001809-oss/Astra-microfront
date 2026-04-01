<template>
  <svg
    class="nav-icon-svg"
    xmlns="http://www.w3.org/2000/svg"
    fill="none"
    viewBox="0 0 24 24"
    :width="size"
    :height="size"
    stroke-width="1.5"
    stroke="currentColor"
    aria-hidden="true"
    focusable="false"
  >
    <path
      v-if="name === 'map'"
      stroke-linecap="round"
      stroke-linejoin="round"
      d="M9 6.75V15m6-6v8.25m.503-8.25 4.125-1.5A.75.75 0 0 1 21 7.5v11.25a.75.75 0 0 1-.502.724l-4.125 1.5a.75.75 0 0 1-.996-.724V6.75a.75.75 0 0 1 .502-.724l4.125-1.5a.75.75 0 0 1 .996.724ZM9 6.75 4.875 5.25A.75.75 0 0 0 9 6.75v8.25m0-8.25-4.125-1.5A.75.75 0 0 0 3 7.5v11.25c0 .415.336.75.75.75l4.125-1.5a.75.75 0 0 0 .502-.724V6.75a.75.75 0 0 0-.502-.724L4.875 4.5"
    />
    <path
      v-else-if="name === 'routes'"
      stroke-linecap="round"
      stroke-linejoin="round"
      d="M13.5 4.5 21 12m0 0-7.5 7.5M21 12H3"
    />
    <path
      v-else-if="name === 'shop'"
      stroke-linecap="round"
      stroke-linejoin="round"
      d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z"
    />
    <path
      v-else-if="name === 'profile'"
      stroke-linecap="round"
      stroke-linejoin="round"
      d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z"
    />
    <path
      v-else-if="name === 'contact'"
      stroke-linecap="round"
      stroke-linejoin="round"
      d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75"
    />
    <path
      v-else-if="name === 'logo'"
      stroke-linecap="round"
      stroke-linejoin="round"
      d="M12 2.25 20.25 7.5v9L12 21.75 3.75 16.5v-9L12 2.25Z"
    />
    <path
      v-else
      stroke-linecap="round"
      stroke-linejoin="round"
      d="M12 6v12m6-6H6"
    />
  </svg>
</template>

<script setup>
defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 20 },
})
</script>

<style scoped>
.nav-icon-svg {
  flex-shrink: 0;
  display: block;
}
</style>
