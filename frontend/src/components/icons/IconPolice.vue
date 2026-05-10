<!-- 警察 - 冒充公检法 -->
<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="12" cy="6" r="4" :stroke="color" stroke-width="2"/>
    <path d="M5 21v-2a7 7 0 0 1 7-7h0a7 7 0 0 1 7 7v2" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M8 6l1 1M16 6l-1 1" :stroke="color" stroke-width="2" stroke-linecap="round"/>
  </svg>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  size?: number | string
  color?: string
}>(), {
  size: 24,
  color: 'currentColor'
})
</script>
