<template>
  <div class="score-progress-bar">
    <div class="progress-header">
      <span class="progress-label">{{ label }}</span>
      <span class="progress-value">{{ score }}/{{ maxScore }}</span>
    </div>
    <div class="progress-track">
      <div 
        class="progress-fill"
        :class="color"
        :style="{ width: percentage + '%' }"
      ></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  score: {
    type: Number,
    default: 0
  },
  maxScore: {
    type: Number,
    default: 100
  },
  color: {
    type: String,
    default: 'blue'
  }
})

const percentage = computed(() => {
  if (props.maxScore === 0) return 0
  return Math.min(100, Math.max(0, (props.score / props.maxScore) * 100))
})
</script>

<style scoped>
.score-progress-bar {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label {
  font-size: 13px;
  color: var(--muted, #64748b);
}

.progress-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--text, #334155);
}

.progress-track {
  height: 8px;
  background: var(--stroke, #e2e8f0);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.progress-fill.purple {
  background: linear-gradient(90deg, #a855f7 0%, #c084fc 100%);
}

.progress-fill.blue {
  background: linear-gradient(90deg, #3b82f6 0%, #60a5fa 100%);
}

.progress-fill.emerald {
  background: linear-gradient(90deg, #10b981 0%, #34d399 100%);
}

.progress-fill.cyan {
  background: linear-gradient(90deg, #06b6d4 0%, #22d3ee 100%);
}

.progress-fill.orange {
  background: linear-gradient(90deg, #f97316 0%, #fb923c 100%);
}
</style>
