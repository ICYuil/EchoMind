<template>
  <div class="radar-chart">
    <svg :viewBox="`0 0 ${size} ${size}`" :width="size" :height="size">
      <!-- 背景网格 -->
      <g class="grid">
        <polygon
          v-for="i in 5"
          :key="`grid-${i}`"
          :points="getPolygonPoints(i / 5)"
          fill="none"
          stroke="#e2e8f0"
          stroke-width="1"
        />
        <!-- 轴线 -->
        <line
          v-for="(axis, index) in axes"
          :key="`axis-${index}`"
          :x1="center"
          :y1="center"
          :x2="axis.x"
          :y2="axis.y"
          stroke="#e2e8f0"
          stroke-width="1"
        />
      </g>
      
      <!-- 数据区域 -->
      <polygon
        :points="dataPoints"
        fill="rgba(59, 130, 246, 0.2)"
        stroke="#3b82f6"
        stroke-width="2"
      />
      
      <!-- 数据点 -->
      <circle
        v-for="(point, index) in dataPointsArray"
        :key="`point-${index}`"
        :cx="point.x"
        :cy="point.y"
        r="4"
        fill="#3b82f6"
        stroke="#fff"
        stroke-width="2"
      />
      
      <!-- 标签 -->
      <text
        v-for="(label, index) in labelPositions"
        :key="`label-${index}`"
        :x="label.x"
        :y="label.y"
        text-anchor="middle"
        dominant-baseline="middle"
        font-size="11"
        fill="#64748b"
      >{{ label.text }}</text>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  labels: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  size: { type: Number, default: 300 }
})

const center = computed(() => props.size / 2)
const radius = computed(() => props.size / 2 - 40)
const maxValue = computed(() => Math.max(...props.data, 100))

const angleStep = computed(() => (Math.PI * 2) / (props.labels.length || 1))

const axes = computed(() => {
  return props.labels.map((_, index) => {
    const angle = index * angleStep.value - Math.PI / 2
    return {
      x: center.value + Math.cos(angle) * radius.value,
      y: center.value + Math.sin(angle) * radius.value
    }
  })
})

const dataPointsArray = computed(() => {
  return props.data.map((value, index) => {
    const angle = index * angleStep.value - Math.PI / 2
    const r = (value / maxValue.value) * radius.value
    return {
      x: center.value + Math.cos(angle) * r,
      y: center.value + Math.sin(angle) * r
    }
  })
})

const dataPoints = computed(() => {
  return dataPointsArray.value.map(p => `${p.x},${p.y}`).join(' ')
})

const labelPositions = computed(() => {
  return props.labels.map((label, index) => {
    const angle = index * angleStep.value - Math.PI / 2
    const r = radius.value + 25
    return {
      x: center.value + Math.cos(angle) * r,
      y: center.value + Math.sin(angle) * r,
      text: label.length > 6 ? label.slice(0, 6) + '...' : label
    }
  })
})

function getPolygonPoints(ratio) {
  const points = []
  for (let i = 0; i < (props.labels.length || 1); i++) {
    const angle = i * angleStep.value - Math.PI / 2
    const r = radius.value * ratio
    const x = center.value + Math.cos(angle) * r
    const y = center.value + Math.sin(angle) * r
    points.push(`${x},${y}`)
  }
  return points.join(' ')
}
</script>

<style scoped>
.radar-chart {
  width: 100%;
  display: flex;
  justify-content: center;
}

.radar-chart svg {
  max-width: 100%;
  height: auto;
}
</style>
