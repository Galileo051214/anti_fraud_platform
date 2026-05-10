<!-- 游戏手柄 - 游戏化学习 -->
<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="2" y="6" width="20" height="12" rx="3" :stroke="color" stroke-width="2"/>
    <path d="M12 12H12.01" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    <path d="M8 10V14" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    <path d="M16 10V14" :stroke="color" stroke-width="2" stroke-linecap="round"/>
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
