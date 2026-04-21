<template>
  <button 
    class="fullscreen-btn" 
    :class="{ 'is-fullscreen': isFullscreen }"
    @click="toggleFullscreen"
    :title="isFullscreen ? '退出全屏' : '全屏模式'"
  >
    <svg v-if="!isFullscreen" viewBox="0 0 24 24" width="20" height="20">
      <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z" fill="currentColor"/>
    </svg>
    <svg v-else viewBox="0 0 24 24" width="20" height="20">
      <path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z" fill="currentColor"/>
    </svg>
  </button>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'toggle'])

const isFullscreen = ref(props.modelValue || false)

watch(() => props.modelValue, (newVal) => {
  isFullscreen.value = newVal
})

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  emit('update:modelValue', isFullscreen.value)
  emit('toggle', isFullscreen.value)
}
</script>

<style scoped>
.fullscreen-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--stroke);
  background: var(--panel);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  /* 移除 position: absolute */
}

.fullscreen-btn:hover {
  background: var(--panel-hover);
  color: var(--brand);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: var(--brand);
}

.fullscreen-btn.is-fullscreen {
  background: var(--brand);
  color: white;
  border-color: var(--brand);
}

.fullscreen-btn.is-fullscreen:hover {
  background: var(--brand-hover);
}
</style>