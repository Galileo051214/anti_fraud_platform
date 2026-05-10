<!-- 面具 - 情景模拟 -->
<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12 22C6.5 22 2 17.5 2 12S6.5 2 12 2s10 4.5 10 10-4.5 10-10 10z" :stroke="color" stroke-width="2"/>
    <circle cx="8" cy="10" r="1.5" :fill="color"/>
    <circle cx="16" cy="10" r="1.5" :fill="color"/>
    <path d="M9 16c1.5 1 4.5 1 6 0" :stroke="color" stroke-width="2" stroke-linecap="round"/>
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
