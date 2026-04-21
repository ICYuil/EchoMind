<template>
  <div class="page-with-bg">
    <Topbar
      title="面试统计"
      subtitle="用可视化观察分数走势与能力画像（来自后端真实数据）。"
    />

    <div class="container page-content">
      <!-- 简历选择器 -->
      <div class="card" style="padding:12px 14px; margin-bottom:14px;">
        <div class="row space center">
          <div>
            <div style="font-weight:950;">选择简历</div>
            <div class="muted2" style="font-size:12px; margin-top:4px;">查看该简历关联的面试统计数据</div>
          </div>
          <select v-model="selectedResumeId" class="select" @change="loadData">
            <option value="">请选择简历</option>
            <option v-for="resume in resumes" :key="resume.id" :value="resume.id">
              {{ resume.filename }}
            </option>
          </select>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="card soft" style="padding:40px; text-align:center;">
        <div class="loading-spinner"></div>
        <div class="muted" style="margin-top:16px;">正在加载统计数据...</div>
      </div>

      <!-- 错误提示 -->
      <div v-else-if="error" class="card soft" style="padding:20px; text-align:center;">
        <div style="color:#ef4444; margin-bottom:12px;">⚠️ {{ error }}</div>
        <button class="btn" @click="loadData">重试</button>
      </div>

      <!-- 无数据提示 -->
      <div v-else-if="!hasData" class="card soft" style="padding:40px; text-align:center;">
        <div class="muted">暂无面试记录。</div>
        <div class="muted2" style="font-size:12px; margin-top:8px;">
          {{ selectedResumeId ? '该简历暂无面试数据，请先完成一次面试。' : '请先选择简历查看统计数据。' }}
        </div>
      </div>

      <!-- 统计数据展示 -->
      <template v-else>
        <!-- 概览卡片 -->
        <div class="grid4" style="margin-bottom:14px;">
          <div class="card" style="padding:16px; text-align:center;">
            <div class="muted2" style="font-size:12px;">面试次数</div>
            <div style="font-size:32px; font-weight:700; color:#6366f1; margin-top:8px;">{{ totalCount }}</div>
          </div>
          <div class="card" style="padding:16px; text-align:center;">
            <div class="muted2" style="font-size:12px;">平均分</div>
            <div style="font-size:32px; font-weight:700; color:#8b5cf6; margin-top:8px;">{{ averageScore }}</div>
          </div>
          <div class="card" style="padding:16px; text-align:center;">
            <div class="muted2" style="font-size:12px;">最高分</div>
            <div style="font-size:32px; font-weight:700; color:#10b981; margin-top:8px;">{{ maxScore }}</div>
          </div>
          <div class="card" style="padding:16px; text-align:center;">
            <div class="muted2" style="font-size:12px;">最低分</div>
            <div style="font-size:32px; font-weight:700; color:#f59e0b; margin-top:8px;">{{ minScore }}</div>
          </div>
        </div>

        <div class="grid2">
          <!-- 总分走势 -->
          <div class="card" style="padding:12px 14px;">
            <div class="row space center">
              <div>
                <div style="font-weight:950;">总分走势</div>
                <div class="muted2" style="font-size:12px; margin-top:4px;">历次面试总分变化趋势</div>
              </div>
            </div>
            <div style="margin-top:12px;">
              <LineChart :labels="labels" :data="scores" title="Score" />
            </div>
            <div style="margin-top:14px;">
              <LineChart :labels="labels" :data="trend" title="Trend (3次平均)" />
            </div>
          </div>

          <!-- 能力维度平均分 -->
          <div class="card" style="padding:12px 14px;">
            <div style="font-weight:950;">能力维度平均分</div>
            <div class="muted2" style="font-size:12px; margin-top:4px;">各能力维度平均分（越高越好）</div>
            <div style="margin-top:12px;">
              <BarChart :labels="dimLabels" :data="dimValues" title="Average" />
            </div>

            <div class="card soft" style="margin-top:14px; padding:12px 14px;">
              <div class="muted2" style="font-size:12px;">解读建议</div>
              <div class="muted" style="margin-top:8px; font-size:12px;">
                如果"技术深度"偏低：准备 2 个项目的关键设计权衡与指标；<br/>
                如果"沟通表达"偏低：回答时用 <span class="mono">STAR</span> 或 <span class="mono">PREP</span> 方法。
              </div>
            </div>
          </div>
        </div>

        <!-- 最近一次能力画像 -->
        <div class="card" style="margin-top:14px; padding:12px 14px;" v-if="last">
          <div class="row space center">
            <div>
              <div style="font-weight:950;">最近一次能力画像</div>
              <div class="muted2" style="font-size:12px; margin-top:4px;">雷达图：最近一次的各维度分数</div>
            </div>
            <div class="badge">总分 {{ last.overall }}</div>
          </div>
          <div style="margin-top:12px; max-width:500px; margin-left:auto; margin-right:auto;">
            <RadarChart :labels="lastDimLabels" :data="lastDimValues" title="Last Interview" />
          </div>
        </div>

        <!-- 详细数据表格 -->
        <div class="card" style="margin-top:14px; padding:12px 14px;">
          <div style="font-weight:950; margin-bottom:12px;">面试记录明细</div>
          <table class="data-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>总分</th>
                <th v-for="dim in allDimensions" :key="dim">{{ dim }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(record, index) in tableData" :key="index">
                <td>第 {{ index + 1 }} 次</td>
                <td style="font-weight:600; color:#6366f1;">{{ record.overall }}</td>
                <td v-for="dim in allDimensions" :key="dim">
                  {{ record.dimensions[dim] || '-' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import Topbar from '../components/ui/Topbar.vue'
import LineChart from '../components/charts/LineChart.vue'
import BarChart from '../components/charts/BarChart.vue'
import RadarChart from '../components/charts/RadarChart.vue'
import { interviewApi } from '../api/interview.js'
import { resumeApi } from '../api/resume.js'

// 状态
const loading = ref(false)
const error = ref('')
const selectedResumeId = ref('')
const resumes = ref([])
const interviewScores = ref([])

// 从后端获取简历列表
async function loadResumes() {
  try {
    console.log('正在获取简历列表...')
    const data = await resumeApi.getResumes()
    console.log('获取到的简历列表:', data)
    resumes.value = data || []
    // 如果有简历，默认选择第一个
    if (resumes.value.length > 0 && !selectedResumeId.value) {
      selectedResumeId.value = resumes.value[0].id
      console.log('默认选择第一个简历:', selectedResumeId.value)
      loadData()
    }
  } catch (err) {
    console.error('获取简历列表失败:', err)
  }
}

// 从后端获取面试统计数据
async function loadData() {
  if (!selectedResumeId.value) return
  
  loading.value = true
  error.value = ''
  
  try {
    console.log('正在获取面试统计数据，resumeId:', selectedResumeId.value)
    const data = await interviewApi.getInterviewScores(selectedResumeId.value)
    console.log('获取到的数据:', data)
    interviewScores.value = data || []
    console.log('处理后的数据:', interviewScores.value)
  } catch (err) {
    console.error('获取面试统计数据失败:', err)
    error.value = '获取统计数据失败，请稍后重试'
    interviewScores.value = []
  } finally {
    loading.value = false
  }
}

// 是否有数据
const hasData = computed(() => interviewScores.value.length > 0)

// 统计概览
const totalCount = computed(() => interviewScores.value.length)
const averageScore = computed(() => {
  if (!interviewScores.value.length) return 0
  const sum = interviewScores.value.reduce((s, r) => s + (r.overallScore || 0), 0)
  return Math.round(sum / interviewScores.value.length)
})
const maxScore = computed(() => {
  if (!interviewScores.value.length) return 0
  return Math.max(...interviewScores.value.map(r => r.overallScore || 0))
})
const minScore = computed(() => {
  if (!interviewScores.value.length) return 0
  return Math.min(...interviewScores.value.map(r => r.overallScore || 0))
})

// 转换后端数据为前端格式
const processedData = computed(() => {
  console.log('processedData 计算，原始数据:', interviewScores.value)
  const result = interviewScores.value.map((score, index) => {
    // 转换维度数据
    const dimensions = {}
    if (score.scoreQuestionEntity) {
      console.log('处理 scoreQuestionEntity:', score.scoreQuestionEntity)
      score.scoreQuestionEntity.forEach(q => {
        dimensions[q.category] = q.socre || 0  // 注意后端拼写是 socre
      })
    }
    
    return {
      id: index,
      overall: score.overallScore || 0,
      dimensions,
      startedAt: Date.now() - (interviewScores.value.length - index) * 86400000 // 模拟时间
    }
  })
  console.log('processedData 结果:', result)
  return result
})

// 折线图数据
const labels = computed(() => {
  return processedData.value.map((_, i) => `第${i + 1}次`)
})
const scores = computed(() => {
  return processedData.value.map(r => r.overall)
})
const trend = computed(() => {
  const points = processedData.value.map(r => r.overall)
  return points.map((p, i) => {
    const window = points.slice(Math.max(0, i - 2), i + 1)
    const avg = window.reduce((s, t) => s + t, 0) / window.length
    return Math.round(avg)
  })
})

// 所有维度名称
const allDimensions = computed(() => {
  const dims = new Set()
  processedData.value.forEach(r => {
    Object.keys(r.dimensions).forEach(d => dims.add(d))
  })
  const result = Array.from(dims)
  console.log('allDimensions:', result)
  return result
})

// 维度平均分（柱状图）
const dimLabels = computed(() => allDimensions.value)
const dimValues = computed(() => {
  return allDimensions.value.map(dim => {
    let sum = 0
    let count = 0
    processedData.value.forEach(r => {
      if (r.dimensions[dim]) {
        sum += r.dimensions[dim]
        count++
      }
    })
    return count ? Math.round(sum / count) : 0
  })
})

// 最近一次数据
const last = computed(() => {
  if (!processedData.value.length) return null
  return processedData.value[processedData.value.length - 1]
})
const lastDimLabels = computed(() => {
  if (!last.value) return []
  return Object.keys(last.value.dimensions)
})
const lastDimValues = computed(() => {
  if (!last.value) return []
  return Object.values(last.value.dimensions)
})

// 表格数据
const tableData = computed(() => processedData.value)

onMounted(() => {
  loadResumes()
  
  document.querySelectorAll('.btn-glow').forEach(btn => {
    btn.addEventListener('mousemove', (e) => {
      const rect = btn.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top
      btn.style.setProperty('--mouse-x', `${x}px`)
      btn.style.setProperty('--mouse-y', `${y}px`)
    })
  })
})
</script>

<style scoped>
.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(99, 102, 241, 0.2);
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.select {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid rgba(99, 102, 241, 0.3);
  background: rgba(15, 23, 42, 0.8);
  color: #fff;
  font-size: 14px;
  min-width: 200px;
  cursor: pointer;
}

.select:focus {
  outline: none;
  border-color: #6366f1;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: center;
  border-bottom: 1px solid rgba(99, 102, 241, 0.1);
}

.data-table th {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(99, 102, 241, 0.1);
}

.data-table tr:hover {
  background: rgba(99, 102, 241, 0.05);
}

.grid4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

@media (max-width: 768px) {
  .grid4 {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
