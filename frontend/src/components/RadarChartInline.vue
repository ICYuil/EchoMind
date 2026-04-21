<template>
  <div class="radar-chart" :style="{ height: height + 'px' }">
    <svg :viewBox="`0 0 ${size} ${size}`" class="radar-svg">
      <g class="grid">
        <polygon
          v-for="level in levels"
          :key="level"
          :points="getGridPoints(level)"
          class="grid-line"
        />
        <line
          v-for="(item, i) in normalizedData"
          :key="'axis-' + i"
          :x1="center"
          :y1="center"
          :x2="getPoint(item.angle, maxRadius).x"
          :y2="getPoint(item.angle, maxRadius).y"
          class="axis-line"
        />
      </g>
      <polygon
        :points="dataPoints"
        class="data-area"
        :style="{ fillOpacity: 0.6 }"
      />
      <polygon
        :points="dataPoints"
        class="data-border"
        fill="none"
      />
      <circle
        v-for="(item, i) in normalizedData"
        :key="'point-' + i"
        :cx="getPoint(item.angle, item.radius).x"
        :cy="getPoint(item.angle, item.radius).y"
        r="4"
        class="data-point"
      />
      <text
        v-for="(item, i) in normalizedData"
        :key="'label-' + i"
        :x="getLabelPosition(item).x"
        :y="getLabelPosition(item).y"
        class="axis-label"
        text-anchor="middle"
        dominant-baseline="middle"
      >
        {{ item.subject }}
      </text>
      <text
        v-for="(item, i) in normalizedData"
        :key="'value-' + i"
        :x="getPoint(item.angle, item.radius).x"
        :y="getPoint(item.angle, item.radius).y - 10"
        class="value-label"
        text-anchor="middle"
      >
        {{ item.originalScore }}
      </text>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    required: true,
    default: () => []
  },
  height: {
    type: Number,
    default: 200
  }
})

const size = 200
const center = size / 2
const maxRadius = 70
const levels = 5

const getPoint = (angle, radius) => ({
  x: center + Math.cos(angle) * radius,
  y: center + Math.sin(angle) * radius
})

const normalizedData = computed(() => {
  if (!props.data || props.data.length === 0) return []
  const fullMarks = props.data.map(item => item.fullMark)
  const maxFullMark = fullMarks.length > 0 ? Math.max(...fullMarks) : 100
  const angleStep = (Math.PI * 2) / props.data.length

  return props.data.map((item, index) => ({
    ...item,
    angle: index * angleStep - Math.PI / 2,
    radius: maxFullMark > 0 ? (item.score / maxFullMark) * maxRadius : 0,
    originalScore: item.score,
    originalFullMark: item.fullMark
  }))
})

const getGridPoints = (level) => {
  const radius = (maxRadius / levels) * level
  return normalizedData.value
    .map(item => {
      const point = getPoint(item.angle, radius)
      return `${point.x},${point.y}`
    })
    .join(' ')
}

const dataPoints = computed(() => {
  return normalizedData.value
    .map(item => {
      const point = getPoint(item.angle, item.radius)
      return `${point.x},${point.y}`
    })
    .join(' ')
})

const getLabelPosition = (item) => {
  const labelRadius = maxRadius + 20
  return getPoint(item.angle, labelRadius)
}
</script>

<style scoped>
.radar-chart {
  display: flex;
  justify-content: center;
}

.radar-svg {
  width: 100%;
  max-width: 220px;
}

.grid-line {
  fill: none;
  stroke: var(--stroke, #e2e8f0);
  stroke-width: 1;
}

.axis-line {
  stroke: var(--stroke, #e2e8f0);
  stroke-width: 1;
}

.data-area {
  fill: var(--brand, #6366f1);
  stroke: none;
}

.data-border {
  stroke: var(--brand, #4f46e5);
  stroke-width: 2;
}

.data-point {
  fill: var(--brand, #6366f1);
  stroke: var(--bg0, white);
  stroke-width: 2;
}

.axis-label {
  font-size: 11px;
  fill: var(--muted, #64748b);
}

.value-label {
  font-size: 10px;
  fill: var(--text, #334155);
  font-weight: 600;
}
</style>
