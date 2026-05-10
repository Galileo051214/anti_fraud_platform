<!-- 目标 - 知识关卡 -->
<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="12" cy="12" r="10" :stroke="color" stroke-width="2"/>
    <circle cx="12" cy="12" r="6" :stroke="color" stroke-width="2"/>
    <circle cx="12" cy="12" r="2" :fill="color"/>
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
