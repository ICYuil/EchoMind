<template>
  <component
    :is="tag"
    ref="elementRef"
    :class="className"
  >
    <slot />
  </component>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useMotion } from '@vueuse/motion'

const props = defineProps({
  tag: { type: String, default: 'div' },
  className: { type: String, default: '' },
  initial: { type: Object, default: () => ({}) },
  animate: { type: Object, default: () => ({}) },
  exit: { type: Object, default: () => ({}) },
  transition: { type: Object, default: () => ({}) },
  whileHover: { type: Object, default: null },
  whileTap: { type: Object, default: null },
  whileInView: { type: Object, default: null },
  viewport: { type: Object, default: () => ({ once: true }) },
  delay: { type: Number, default: 0 }
})

const elementRef = ref(null)

// 构建 motion 配置
const motionConfig = computed(() => {
  const config = {
    initial: props.initial,
    enter: props.animate,
    ...props.transition
  }
  
  if (props.delay) {
    config.delay = props.delay
  }
  
  if (props.whileHover) {
    config.hovered = props.whileHover
  }
  
  if (props.whileTap) {
    config.tapped = props.whileTap
  }
  
  return config
})

// 使用 useMotion
const { variant } = useMotion(elementRef, motionConfig)

// 监听 animate 变化
watch(() => props.animate, (newAnimate) => {
  if (elementRef.value) {
    variant.value = 'enter'
  }
}, { deep: true })

onMounted(() => {
  // 延迟触发动画
  if (props.delay) {
    setTimeout(() => {
      variant.value = 'enter'
    }, props.delay * 1000)
  }
})
</script>
