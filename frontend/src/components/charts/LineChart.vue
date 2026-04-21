<template>
  <div class="line-chart">
    <svg :viewBox="`0 0 ${width} ${height}`" :width="width" :height="height">
      <!-- 网格线 -->
      <g class="grid">
        <line
          v-for="i in 5"
          :key="`h-${i}`"
          x1="0"
          :y1="(height - padding * 2) * i / 5 + padding"
          :x2="width"
          :y2="(height - padding * 2) * i / 5 + padding"
          stroke="#e2e8f0"
          stroke-dasharray="4"
        />
      </g>
      
      <!-- 折线 -->
      <path
        :d="linePath"
        fill="none"
        stroke="#3b82f6"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      
      <!-- 数据点 -->
      <circle
        v-for="(point, index) in points"
        :key="`point-${index}`"
        :cx="point.x"
        :cy="point.y"
        r="4"
        fill="#3b82f6"
        stroke="#fff"
        stroke-width="2"
      />
      
      <!-- X轴标签 -->
      <text
        v-for="(label, index) in xLabels"
        :key="`label-${index}`"
        :x="label.x"
        :y="height - 5"
        text-anchor="middle"
        font-size="10"
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
  width: { type: Number, default: 400 },
  height: { type: Number, default: 150 },
  padding: { type: Number, default: 20 }
})

const maxValue = computed(() => Math.max(...props.data, 100))
const minValue = computed(() => Math.min(...props.data, 0))

const points = computed(() => {
  const chartWidth = props.width - props.padding * 2
  const chartHeight = props.height - props.padding * 2
  const range = maxValue.value - minValue.value || 1
  
  return props.data.map((value, index) => ({
    x: props.padding + (chartWidth / (props.data.length - 1 || 1)) * index,
    y: props.height - props.padding - ((value - minValue.value) / range) * chartHeight
  }))
})

const linePath = computed(() => {
  if (points.value.length === 0) return ''
  return points.value.reduce((path, point, index) => {
    return index === 0 
      ? `M ${point.x} ${point.y}` 
      : `${path} L ${point.x} ${point.y}`
  }, '')
})

const xLabels = computed(() => {
  const chartWidth = props.width - props.padding * 2
  const step = Math.ceil(props.labels.length / 6)
  
  return props.labels.map((label, index) => ({
    x: props.padding + (chartWidth / (props.labels.length - 1 || 1)) * index,
    text: index % step === 0 ? label : ''
  })).filter(l => l.text)
})
</script>

<style scoped>
.line-chart {
  width: 100%;
}

.line-chart svg {
  width: 100%;
  height: auto;
}
</style>
