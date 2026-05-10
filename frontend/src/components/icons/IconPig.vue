<!-- 猪 - 杀猪盘 -->
<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <ellipse cx="12" cy="12" rx="9" ry="7" :stroke="color" stroke-width="2"/>
    <ellipse cx="12" cy="13" rx="3" ry="2" :stroke="color" stroke-width="2"/>
    <circle cx="9" cy="10" r="1" :fill="color"/>
    <circle cx="15" cy="10" r="1" :fill="color"/>
    <path d="M6 7c-1-1-2-1-2 0" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    <path d="M18 7c1-1 2-1 2 0" :stroke="color" stroke-width="2" stroke-linecap="round"/>
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
