<template>
  <div ref="containerRef" class="virtual-list-container" :style="containerStyle">
    <div class="virtual-list-phantom" :style="phantomStyle"></div>
    <div class="virtual-list-content" :style="contentStyle">
      <div
        v-for="item in visibleItems"
        :key="item.key"
        class="virtual-list-item"
        :style="item.style"
      >
        <slot :item="item.data" :index="item.index"></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

const props = defineProps({
  items: { type: Array, required: true },
  itemHeight: { type: Number, default: 60 },
  bufferSize: { type: Number, default: 5 },
  scrollToBottom: { type: Boolean, default: false }
})

const emit = defineEmits(['scroll', 'scrollToTop', 'scrollToBottom'])

const containerRef = ref(null)
const scrollTop = ref(0)
const containerHeight = ref(0)

// 计算可见区域
const visibleRange = computed(() => {
  const start = Math.floor(scrollTop.value / props.itemHeight)
  const visibleCount = Math.ceil(containerHeight.value / props.itemHeight)
  
  const startIndex = Math.max(0, start - props.bufferSize)
  const endIndex = Math.min(
    props.items.length,
    start + visibleCount + props.bufferSize
  )
  
  return { startIndex, endIndex }
})

// 可见项目
const visibleItems = computed(() => {
  const { startIndex, endIndex } = visibleRange.value
  return props.items.slice(startIndex, endIndex).map((item, index) => ({
    data: item,
    index: startIndex + index,
    key: item.id || `item-${startIndex + index}`,
    style: {
      position: 'absolute',
      top: `${(startIndex + index) * props.itemHeight}px`,
      height: `${props.itemHeight}px`,
      left: 0,
      right: 0
    }
  }))
})

// 容器样式
const containerStyle = computed(() => ({
  position: 'relative',
  overflow: 'auto',
  height: '100%'
}))

// 幻影高度（总高度）
const phantomStyle = computed(() => ({
  height: `${props.items.length * props.itemHeight}px`
}))

// 内容样式
const contentStyle = computed(() => {
  const { startIndex } = visibleRange.value
  return {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    transform: `translateY(${startIndex * props.itemHeight}px)`
  }
})

// 处理滚动
const handleScroll = () => {
  if (!containerRef.value) return
  scrollTop.value = containerRef.value.scrollTop
  emit('scroll', scrollTop.value)
}

// 滚动到底部
const scrollToBottomFn = () => {
  nextTick(() => {
    if (containerRef.value) {
      containerRef.value.scrollTop = containerRef.value.scrollHeight
    }
  })
}

// 滚动到顶部
const scrollToTop = () => {
  if (containerRef.value) {
    containerRef.value.scrollTop = 0
  }
}

// 监听 items 变化，自动滚动到底部
watch(() => props.items.length, (newLength, oldLength) => {
  if (props.scrollToBottom && newLength > oldLength) {
    scrollToBottomFn()
  }
})

onMounted(() => {
  if (containerRef.value) {
    containerHeight.value = containerRef.value.clientHeight
    containerRef.value.addEventListener('scroll', handleScroll)
    
    // 初始滚动到底部
    if (props.scrollToBottom) {
      scrollToBottomFn()
    }
  }
  
  // 监听容器大小变化
  const resizeObserver = new ResizeObserver((entries) => {
    for (const entry of entries) {
      containerHeight.value = entry.contentRect.height
    }
  })
  
  if (containerRef.value) {
    resizeObserver.observe(containerRef.value)
  }
  
  onUnmounted(() => {
    resizeObserver.disconnect()
  })
})

onUnmounted(() => {
  if (containerRef.value) {
    containerRef.value.removeEventListener('scroll', handleScroll)
  }
})

// 暴露方法
defineExpose({
  scrollToBottom: scrollToBottomFn,
  scrollToTop,
  getScrollTop: () => scrollTop.value
})
</script>

<style scoped>
.virtual-list-container {
  position: relative;
  overflow: auto;
  will-change: transform;
}

.virtual-list-phantom {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  pointer-events: none;
}

.virtual-list-content {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

.virtual-list-item {
  position: absolute;
  left: 0;
  right: 0;
}

/* 隐藏滚动条但保留功能 */
.virtual-list-container::-webkit-scrollbar {
  width: 4px;
}

.virtual-list-container::-webkit-scrollbar-track {
  background: transparent;
}

.virtual-list-container::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 2px;
}
</style>
