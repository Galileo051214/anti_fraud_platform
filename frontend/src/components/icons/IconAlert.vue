<!-- 公告/喇叭 - 紧急预警 -->
<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12 22c6 0 10-4.5 10-10S18 2 12 2 2 6.5 2 12s4 10 10 10z" :stroke="color" stroke-width="2"/>
    <path d="M12 8v4M12 16h.01" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    <path d="M7 8l-1 1M17 8l1 1M7 15l-1 1M17 15l1 1" :stroke="color" stroke-width="2" stroke-linecap="round"/>
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
