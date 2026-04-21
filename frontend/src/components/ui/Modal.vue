<template>
  <div v-if="open" class="modal-backdrop" @mousedown.self="$emit('close')">
    <div class="modal">
      <div class="mhead">
        <div>
          <div style="font-weight:950;">{{ title }}</div>
          <div v-if="subtitle" class="muted" style="font-size:12px; margin-top:4px;">{{ subtitle }}</div>
        </div>
        <button class="btn ghost" @click="$emit('close')">关闭</button>
      </div>
      <div class="mbody">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  open: Boolean,
  title: String,
  subtitle: String,
})
defineEmits(['close'])
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal {
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: var(--shadow-hover);
  animation: modal-in 0.3s ease;
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.mhead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--stroke);
}

.mbody {
  padding: 20px;
  overflow-y: auto;
  max-height: calc(90vh - 80px);
}

.btn.ghost {
  padding: 6px 12px;
  border: 1px solid var(--stroke);
  background: transparent;
  color: var(--muted);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn.ghost:hover {
  background: var(--panel-hover);
  color: var(--text);
}
</style>
