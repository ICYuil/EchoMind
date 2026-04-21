<template>
  <span class="status-badge" :class="statusClass">
    <component :is="statusIcon" v-if="statusIcon" width="14" height="14" />
    {{ statusText }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { CheckCircle, Clock, AlertCircle, Loader2 } from 'lucide-vue-next'

const props = defineProps({
  interview: { type: Object, required: true }
})

const isEvaluateCompleted = (interview) => {
  if (interview.evaluateStatus === 'COMPLETED') return true
  if (interview.status === 'EVALUATED') return true
  return false
}

const isEvaluating = (interview) => {
  return interview.evaluateStatus === 'PENDING' || interview.evaluateStatus === 'PROCESSING'
}

const isEvaluateFailed = (interview) => {
  return interview.evaluateStatus === 'FAILED'
}

const statusText = computed(() => {
  const { interview } = props
  if (isEvaluateFailed(interview)) return '评估失败'
  if (isEvaluating(interview)) {
    return interview.evaluateStatus === 'PROCESSING' ? '评估中' : '等待评估'
  }
  if (isEvaluateCompleted(interview)) return '已完成'
  if (interview.status === 'IN_PROGRESS') return '进行中'
  if (interview.status === 'INTERRUPTED') return '已中断'
  if (interview.status === 'COMPLETED' || interview.status === 'EVALUATED') return '已提交'
  return '已创建'
})

const statusClass = computed(() => {
  const { interview } = props
  if (isEvaluateCompleted(interview)) return 'completed'
  if (isEvaluating(interview)) return 'evaluating'
  if (isEvaluateFailed(interview)) return 'failed'
  if (interview.status === 'IN_PROGRESS') return 'in-progress'
  if (interview.status === 'INTERRUPTED') return 'interrupted'
  return 'created'
})

const statusIcon = computed(() => {
  const { interview } = props
  if (isEvaluateCompleted(interview)) return CheckCircle
  if (isEvaluating(interview)) return Loader2
  if (isEvaluateFailed(interview)) return AlertCircle
  if (interview.status === 'IN_PROGRESS') return Clock
  return null
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.status-badge.completed {
  background: #f0fdf4;
  color: #16a34a;
}

.status-badge.evaluating {
  background: #eef2ff;
  color: #6366f1;
}

.status-badge.failed {
  background: #fef2f2;
  color: #dc2626;
}

.status-badge.in-progress {
  background: #fef3c7;
  color: #d97706;
}

.status-badge.interrupted {
  background: #fef2f2;
  color: #dc2626;
}

.status-badge.created {
  background: #f3f4f6;
  color: #6b7280;
}
</style>
