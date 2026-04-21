<template>
  <div class="score-display">
    <div class="score-ring">
      <svg class="score-svg" viewBox="0 0 44 44">
        <!-- 背景圆环 -->
        <circle
          class="score-track"
          cx="22"
          cy="22"
          r="18"
          fill="none"
          stroke-width="4"
        />
        <!-- 进度圆环 -->
        <circle
          class="score-fill"
          cx="22"
          cy="22"
          r="18"
          fill="none"
          stroke-width="4"
          :stroke-dasharray="circumference"
          :stroke-dashoffset="strokeDashoffset"
          :class="scoreColorClass"
        />
      </svg>
      <span class="score-value" :class="scoreColorClass">{{ score }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  score: { type: Number, required: true },
  size: { type: Number, default: 44 }
})

const radius = 18
const circumference = 2 * Math.PI * radius

const strokeDashoffset = computed(() => {
  const progress = props.score / 100
  return circumference - progress * circumference
})

const scoreColorClass = computed(() => {
  const score = props.score
  if (score >= 80) return 'excellent'
  if (score >= 60) return 'good'
  return 'needs-improvement'
})
</script>

<style scoped>
.score-display {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.score-ring {
  position: relative;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.score-svg {
  position: absolute;
  inset: 0;
  transform: rotate(-90deg);
}

.score-track {
  stroke: #f3f4f6;
}

.score-fill {
  transition: stroke-dashoffset 0.5s ease;
  stroke-linecap: round;
}

.score-fill.excellent {
  stroke: #22c55e;
}

.score-fill.good {
  stroke: #f59e0b;
}

.score-fill.needs-improvement {
  stroke: #ef4444;
}

.score-value {
  font-size: 14px;
  font-weight: 700;
  z-index: 1;
}

.score-value.excellent {
  color: #16a34a;
}

.score-value.good {
  color: #d97706;
}

.score-value.needs-improvement {
  color: #dc2626;
}
</style>
