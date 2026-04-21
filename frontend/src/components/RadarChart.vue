<template>
  <div class="radar-chart" :style="{ height: config.height + 'px' }">
    <svg :viewBox="`0 0 ${config.size} ${config.size}`" class="radar-svg" :class="{ 'inline': size === 'inline' }">
      <!-- 背景网格 -->
      <g class="grid">
        <!-- 同心多边形 -->
        <polygon
          v-for="level in levels"
          :key="level"
          :points="getGridPoints(level)"
          class="grid-line"
        />
        <!-- 轴线 -->
        <line
          v-for="(item, i) in normalizedData"
          :key="'axis-' + i"
          :x1="center"
          :y1="center"
          :x2="getPoint(item.angle, config.maxRadius).x"
          :y2="getPoint(item.angle, config.maxRadius).y"
          class="axis-line"
        />
      </g>

      <!-- 数据区域 -->
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

      <!-- 数据点 -->
      <circle
        v-for="(item, i) in normalizedData"
        :key="'point-' + i"
        :cx="getPoint(item.angle, item.radius).x"
        :cy="getPoint(item.angle, item.radius).y"
        :r="config.pointRadius"
        class="data-point"
      />

      <!-- 标签 -->
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

      <!-- 数值标签 -->
      <text
        v-for="(item, i) in normalizedData"
        :key="'value-' + i"
        :x="getPoint(item.angle, item.radius).x"
        :y="getPoint(item.angle, item.radius).y - config.valueOffset"
        class="value-label"
        text-anchor="middle"
      >
        {{ size === 'inline' ? item.originalScore : item.originalScore + '/' + item.originalFullMark }}
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
  size: {
    type: String,
    default: 'default' // 'default' | 'inline'
  }
})

// 根据 size 返回配置
const config = computed(() => {
  if (props.size === 'inline') {
    return {
      size: 200,
      maxRadius: 70,
      pointRadius: 4,
      valueOffset: 10,
      height: 200,
      labelOffset: 20
    }
  }
  return {
    size: 280,
    maxRadius: 90,
    pointRadius: 5,
    valueOffset: 12,
    height: 280,
    labelOffset: 25
  }
})

const center = computed(() => config.value.size / 2)
const levels = 5

// 归一化数据
const normalizedData = computed(() => {
  if (!props.data || props.data.length === 0) return []

  const maxFullMark = Math.max(...props.data.map(item => item.fullMark))
  const angleStep = (Math.PI * 2) / props.data.length

  return props.data.map((item, index) => {
    const normalizedScore = (item.score / item.fullMark) * maxFullMark
    const radius = (normalizedScore / maxFullMark) * config.value.maxRadius
    const angle = index * angleStep - Math.PI / 2

    return {
      ...item,
      angle,
      radius,
      originalScore: item.score,
      originalFullMark: item.fullMark
    }
  })
})

// 计算数据点
const dataPoints = computed(() => {
  return normalizedData.value
    .map(item => {
      const point = getPoint(item.angle, item.radius)
      return `${point.x},${point.y}`
    })
    .join(' ')
})

// 获取网格点
const getGridPoints = (level) => {
  const radius = (level / levels) * config.value.maxRadius
  const angleStep = (Math.PI * 2) / (props.data.length || 5)
  const points = []

  for (let i = 0; i < (props.data.length || 5); i++) {
    const angle = i * angleStep - Math.PI / 2
    const point = getPoint(angle, radius)
    points.push(`${point.x},${point.y}`)
  }

  return points.join(' ')
}

// 根据角度和半径计算坐标
const getPoint = (angle, radius) => {
  return {
    x: center.value + radius * Math.cos(angle),
    y: center.value + radius * Math.sin(angle)
  }
}

// 获取标签位置
const getLabelPosition = (item) => {
  const labelRadius = config.value.maxRadius + config.value.labelOffset
  const point = getPoint(item.angle, labelRadius)
  
  // 调整标签位置避免重叠
  let y = point.y
  if (Math.abs(item.angle + Math.PI / 2) < 0.1) {
    y -= 5 // 顶部标签上移
  } else if (Math.abs(item.angle - Math.PI / 2) < 0.1) {
    y += 5 // 底部标签下移
  }
  
  return { x: point.x, y }
}
</script>

<style scoped>
.radar-chart {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.radar-svg {
  width: 100%;
  height: 100%;
  max-width: 280px;
  max-height: 280px;
}

.radar-svg.inline {
  max-width: 200px;
  max-height: 200px;
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
  fill-opacity: 0.6;
}

.data-border {
  stroke: var(--brand-2, #4f46e5);
  stroke-width: 2;
}

.data-point {
  fill: var(--panel, white);
  stroke: var(--brand-2, #4f46e5);
  stroke-width: 2;
}

.axis-label {
  font-size: 12px;
  font-weight: 500;
  fill: var(--text, #64748b);
}

.value-label {
  font-size: 10px;
  font-weight: 600;
  fill: var(--brand, #4f46e5);
}
</style>
