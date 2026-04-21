<template>
  <div class="bar-chart">
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
      
      <!-- 柱状图 -->
      <rect
        v-for="(bar, index) in bars"
        :key="`bar-${index}`"
        :x="bar.x"
        :y="bar.y"
        :width="bar.width"
        :height="bar.height"
        fill="#3b82f6"
        rx="4"
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
      
      <!-- 数值标签 -->
      <text
        v-for="(bar, index) in bars"
        :key="`value-${index}`"
        :x="bar.x + bar.width / 2"
        :y="bar.y - 5"
        text-anchor="middle"
        font-size="10"
        fill="#3b82f6"
        font-weight="600"
      >{{ props.data[index] }}</text>
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
  height: { type: Number, default: 200 },
  padding: { type: Number, default: 30 }
})

const maxValue = computed(() => Math.max(...props.data, 100))

const bars = computed(() => {
  const chartWidth = props.width - props.padding * 2
  const chartHeight = props.height - props.padding * 2
  const barWidth = chartWidth / props.data.length * 0.6
  const gap = chartWidth / props.data.length * 0.4
  
  return props.data.map((value, index) => {
    const barHeight = (value / maxValue.value) * chartHeight
    return {
      x: props.padding + (barWidth + gap) * index + gap / 2,
      y: props.height - props.padding - barHeight,
      width: barWidth,
      height: barHeight
    }
  })
})

const xLabels = computed(() => {
  const chartWidth = props.width - props.padding * 2
  const barWidth = chartWidth / props.labels.length * 0.6
  const gap = chartWidth / props.labels.length * 0.4
  
  return props.labels.map((label, index) => ({
    x: props.padding + (barWidth + gap) * index + gap / 2 + barWidth / 2,
    text: label.length > 4 ? label.slice(0, 4) + '...' : label
  }))
})
</script>

<style scoped>
.bar-chart {
  width: 100%;
}

.bar-chart svg {
  width: 100%;
  height: auto;
}
</style>
