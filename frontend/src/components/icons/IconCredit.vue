<!-- 信用卡 - 贷款诈骗 -->
<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="2" y="5" width="20" height="14" rx="2" :stroke="color" stroke-width="2"/>
    <line x1="2" y1="10" x2="22" y2="10" :stroke="color" stroke-width="2"/>
    <line x1="6" y1="15" x2="10" y2="15" :stroke="color" stroke-width="2" stroke-linecap="round"/>
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
