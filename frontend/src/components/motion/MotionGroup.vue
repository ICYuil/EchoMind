<template>
  <component :is="tag" :class="className">
    <slot />
  </component>
</template>

<script setup>
import { provide } from 'vue'

defineProps({
  tag: { type: String, default: 'div' },
  className: { type: String, default: '' }
})

// 提供 stagger 上下文
provide('motionGroup', {
  staggerDelay: 0.1
})
</script>
