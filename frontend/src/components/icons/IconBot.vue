<!-- 机器人 - 智能客服 -->
<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="3" y="11" width="18" height="10" rx="2" :stroke="color" stroke-width="2"/>
    <circle cx="12" cy="5" r="3" :stroke="color" stroke-width="2"/>
    <path d="M9 16L10 18" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    <path d="M15 16L14 18" :stroke="color" stroke-width="2" stroke-linecap="round"/>
    <circle cx="8" cy="16" r="1" :fill="color"/>
    <circle cx="16" cy="16" r="1" :fill="color"/>
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
