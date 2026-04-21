<template>
  <Teleport to="body">
    <div 
      class="resume-modal-wrapper"
      :style="{ zIndex: zIndex }"
      @mousedown="bringToFront"
    >
      <div 
        class="resume-modal"
        :class="{ 'is-dragging': isDragging }"
        :style="{ 
          left: position.x + 'px', 
          top: position.y + 'px',
          width: width + 'px',
          height: height + 'px'
        }"
      >
        <!-- 标题栏（可拖拽） -->
        <div 
          class="modal-header"
          @mousedown="startDrag"
        >
          <div class="header-title">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
            <span class="filename">{{ resume?.filename || '简历详情' }}</span>
          </div>
          <div class="header-actions">
            <button class="action-btn minimize" @click="minimize" title="最小化">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </button>
            <button class="action-btn close" @click="close" title="关闭">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 标签切换 -->
        <div class="modal-tabs">
          <button 
            class="tab-btn"
            :class="{ active: activeTab === 'content' }"
            @click="activeTab = 'content'"
          >
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
            简历内容
          </button>
          <button 
            class="tab-btn"
            :class="{ active: activeTab === 'analysis' }"
            @click="activeTab = 'analysis'"
          >
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 11 12 14 22 4"/>
              <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
            </svg>
            简历分析
            <span v-if="isAnalyzing" class="tab-badge analyzing">分析中</span>
          </button>
        </div>

        <!-- 内容区域 -->
        <div class="modal-body">
          <!-- 加载状态 -->
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>

          <!-- 错误状态 -->
          <div v-else-if="error" class="error-state">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <p>{{ error }}</p>
            <button class="btn-retry" @click="loadResumeDetail">重试</button>
          </div>

          <!-- 简历内容标签 -->
          <div v-else-if="activeTab === 'content'" class="tab-panel content-panel">
            <div v-if="resume?.resumeText || resume?.content" class="resume-content">
              <pre>{{ resume?.resumeText || resume?.content }}</pre>
            </div>
            <div v-else class="empty-state">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
              <p>暂无简历内容</p>
            </div>
          </div>

          <!-- 简历分析标签 -->
          <div v-else-if="activeTab === 'analysis'" class="tab-panel analysis-panel">
            <!-- 分析中 -->
            <div v-if="isAnalyzing" class="analyzing-state">
              <div class="analyzing-animation">
                <div class="pulse-ring"></div>
                <div class="pulse-ring"></div>
                <div class="pulse-ring"></div>
              </div>
              <h4>AI 正在分析简历...</h4>
              <p>请稍候，正在提取关键信息并生成分析报告</p>
            </div>

            <!-- 分析结果 -->
            <div v-else-if="analysis" class="analysis-content" ref="analysisContent">
              <!-- 核心评价 -->
              <div class="analysis-section">
                <div class="section-header">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                    <polyline points="17 6 23 6 23 12"/>
                  </svg>
                  <span>核心评价</span>
                </div>
                <div class="evaluation-card">
                  <p class="summary">{{ analysis.summary || '暂无评价' }}</p>
                  <div class="score-display">
                    <div class="score-item">
                      <span class="score-label">综合评分</span>
                      <div class="score-value">
                        <span class="score-number">{{ analysis.overallScore || 0 }}</span>
                        <span class="score-total">/100</span>
                      </div>
                    </div>
                    <div class="score-item" v-if="analysis.analyzedAt">
                      <span class="score-label">分析时间</span>
                      <span class="score-time">{{ formatDateTime(analysis.analyzedAt) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 多维度评分雷达图 -->
              <div class="analysis-section radar-section">
                <div class="section-header">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <circle cx="12" cy="12" r="6"/>
                    <circle cx="12" cy="12" r="2"/>
                  </svg>
                  <span>多维度评分</span>
                </div>
                <div class="radar-container">
                  <RadarChart :data="radarData" :height="220" />
                </div>
                <div class="dimension-scores">
                  <ScoreProgressBar
                    label="项目经验"
                    :score="analysis.projectScore || 0"
                    :maxScore="40"
                    color="purple"
                  />
                  <ScoreProgressBar
                    label="技能匹配"
                    :score="analysis.skillMatchScore || 0"
                    :maxScore="20"
                    color="blue"
                  />
                  <ScoreProgressBar
                    label="内容完整性"
                    :score="analysis.contentScore || 0"
                    :maxScore="15"
                    color="emerald"
                  />
                  <ScoreProgressBar
                    label="结构清晰度"
                    :score="analysis.structureScore || 0"
                    :maxScore="15"
                    color="cyan"
                  />
                  <ScoreProgressBar
                    label="表达专业性"
                    :score="analysis.expressionScore || 0"
                    :maxScore="10"
                    color="orange"
                  />
                </div>
              </div>

              <!-- 优势 -->
              <div v-if="analysis.strengths?.length > 0" class="analysis-section">
                <div class="section-header success">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                    <polyline points="22 4 12 14.01 9 11.01"/>
                  </svg>
                  <span>优势亮点</span>
                </div>
                <div class="strengths-list">
                  <span v-for="(strength, i) in analysis.strengths" :key="i" class="strength-tag">
                    {{ strength }}
                  </span>
                </div>
              </div>

              <!-- 改进建议 -->
              <div v-if="analysis.suggestions?.length > 0" class="analysis-section">
                <div class="section-header warning">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                  </svg>
                  <span>改进建议</span>
                  <span class="suggestion-count">({{ analysis.suggestions.length }} 条)</span>
                </div>
                <div class="suggestions-list">
                  <!-- 高优先级 -->
                  <div v-if="suggestionsByPriority.high.length > 0" class="suggestion-group">
                    <div class="group-header high">
                      <span class="priority-badge high">高</span>
                      <span class="group-title">高优先级</span>
                    </div>
                    <div 
                      v-for="(suggestion, i) in suggestionsByPriority.high" 
                      :key="'high-'+i"
                      class="suggestion-item"
                    >
                      <span class="category-tag" :class="suggestion.category">{{ suggestion.category }}</span>
                      <div class="suggestion-content">
                        <div class="suggestion-issue">{{ suggestion.issue }}</div>
                        <div class="suggestion-recommendation" v-if="suggestion.recommendation">
                          <span class="recommendation-label">建议：</span>{{ suggestion.recommendation }}
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 中优先级 -->
                  <div v-if="suggestionsByPriority.medium.length > 0" class="suggestion-group">
                    <div class="group-header medium">
                      <span class="priority-badge medium">中</span>
                      <span class="group-title">中优先级</span>
                    </div>
                    <div 
                      v-for="(suggestion, i) in suggestionsByPriority.medium" 
                      :key="'medium-'+i"
                      class="suggestion-item"
                    >
                      <span class="category-tag" :class="suggestion.category">{{ suggestion.category }}</span>
                      <div class="suggestion-content">
                        <div class="suggestion-issue">{{ suggestion.issue }}</div>
                        <div class="suggestion-recommendation" v-if="suggestion.recommendation">
                          <span class="recommendation-label">建议：</span>{{ suggestion.recommendation }}
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 低优先级 -->
                  <div v-if="suggestionsByPriority.low.length > 0" class="suggestion-group">
                    <div class="group-header low">
                      <span class="priority-badge low">低</span>
                      <span class="group-title">低优先级</span>
                    </div>
                    <div 
                      v-for="(suggestion, i) in suggestionsByPriority.low" 
                      :key="'low-'+i"
                      class="suggestion-item"
                    >
                      <span class="category-tag" :class="suggestion.category">{{ suggestion.category }}</span>
                      <div class="suggestion-content">
                        <div class="suggestion-issue">{{ suggestion.issue }}</div>
                        <div class="suggestion-recommendation" v-if="suggestion.recommendation">
                          <span class="recommendation-label">建议：</span>{{ suggestion.recommendation }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 技能评估 -->
              <div v-if="analysis.skillScores?.length > 0" class="analysis-section">
                <div class="section-header">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                  </svg>
                  <span>技能评估</span>
                </div>
                <div class="skills-list">
                  <div v-for="(skill, i) in analysis.skillScores" :key="i" class="skill-item">
                    <span class="skill-name">{{ skill.name }}</span>
                    <div class="skill-bar">
                      <div class="skill-fill" :style="{ width: skill.score + '%' }"></div>
                    </div>
                    <span class="skill-score">{{ skill.score }}</span>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="analysis-actions">
                <button class="btn-action" @click="reanalyze" :disabled="reanalyzing">
                  <svg v-if="!reanalyzing" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="23 4 23 10 17 10"/>
                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
                  </svg>
                  <span v-else class="btn-spinner"></span>
                  {{ reanalyzing ? '分析中...' : '重新分析' }}
                </button>
                <button class="btn-action secondary" @click="exportPdf" :disabled="exporting">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7 10 12 15 17 10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                  {{ exporting ? '导出中...' : '导出报告' }}
                </button>
              </div>
            </div>

            <!-- 无分析结果 -->
            <div v-else class="empty-state">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <p>暂无分析结果</p>
              <button class="btn-action" @click="reanalyze" :disabled="reanalyzing">
                <span v-if="reanalyzing" class="btn-spinner"></span>
                {{ reanalyzing ? '分析中...' : '立即分析' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 调整大小手柄 -->
        <div class="resize-handle" @mousedown="startResize"></div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { resumeApi } from '../../api/resume.js'
import RadarChart from '../RadarChart.vue'
import ScoreProgressBar from '../ScoreProgressBar.vue'

const props = defineProps({
  resumeId: {
    type: Number,
    required: true
  },
  initialPosition: {
    type: Object,
    default: () => ({ x: 100, y: 100 })
  },
  zIndex: {
    type: Number,
    default: 1000
  }
})

const emit = defineEmits(['close', 'minimize', 'focus'])

// 位置和尺寸
const position = ref({ ...props.initialPosition })
const width = ref(800)
const height = ref(700)
const isDragging = ref(false)
const isResizing = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const resizeStart = ref({ x: 0, y: 0, width: 0, height: 0 })

// 状态
const loading = ref(true)
const error = ref(null)
const activeTab = ref('content')
const resume = ref(null)
const reanalyzing = ref(false)
const exporting = ref(false)
const analysisContent = ref(null)

// 计算属性
const analysis = computed(() => {
  if (!resume.value?.analyses || resume.value.analyses.length === 0) return null
  return resume.value.analyses[0]
})

const isAnalyzing = computed(() => {
  return resume.value?.analyzeStatus === 'PROCESSING' || reanalyzing.value
})

// 雷达图数据
const radarData = computed(() => {
  if (!analysis.value) return []
  return [
    { subject: '表达专业性', score: analysis.value.expressionScore || 0, fullMark: 10 },
    { subject: '技能匹配', score: analysis.value.skillMatchScore || 0, fullMark: 20 },
    { subject: '内容完整性', score: analysis.value.contentScore || 0, fullMark: 15 },
    { subject: '结构清晰度', score: analysis.value.structureScore || 0, fullMark: 15 },
    { subject: '项目经验', score: analysis.value.projectScore || 0, fullMark: 40 }
  ]
})

// 建议按优先级分组
const suggestionsByPriority = computed(() => {
  if (!analysis.value?.suggestions) {
    return { high: [], medium: [], low: [] }
  }
  const suggestions = analysis.value.suggestions
  return {
    high: suggestions.filter(s => s.priority === 'HIGH' || s.priority === '高'),
    medium: suggestions.filter(s => s.priority === 'MEDIUM' || s.priority === '中'),
    low: suggestions.filter(s => s.priority === 'LOW' || s.priority === '低')
  }
})

// 加载简历详情
const loadResumeDetail = async () => {
  loading.value = true
  error.value = null
  try {
    const data = await resumeApi.getResumeDetail(props.resumeId)
    resume.value = data
  } catch (err) {
    console.error('加载简历详情失败:', err)
    error.value = '加载失败: ' + (err.message || '请稍后重试')
  } finally {
    loading.value = false
  }
}

// 重新分析
const reanalyze = async () => {
  reanalyzing.value = true
  try {
    await resumeApi.reanalyze(props.resumeId)
    // 轮询检查分析状态
    const checkStatus = async () => {
      try {
        const data = await resumeApi.getResumeDetail(props.resumeId)
        resume.value = data
        if (data.analyzeStatus === 'PROCESSING') {
          setTimeout(checkStatus, 2000)
        } else {
          reanalyzing.value = false
        }
      } catch (err) {
        console.error('检查分析状态失败:', err)
        reanalyzing.value = false
      }
    }
    checkStatus()
  } catch (err) {
    console.error('重新分析失败:', err)
    alert('重新分析失败: ' + (err.message || '请稍后重试'))
    reanalyzing.value = false
  }
}

// 导出PDF
const exportPdf = async () => {
  exporting.value = true
  try {
    const blob = await resumeApi.exportAnalysisPdf(props.resumeId)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `简历分析报告_${resume.value?.filename || props.resumeId}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (err) {
    console.error('导出失败:', err)
    alert('导出失败: ' + (err.message || '请稍后重试'))
  } finally {
    exporting.value = false
  }
}

// 拖拽功能
const startDrag = (e) => {
  if (e.target.closest('.action-btn')) return
  isDragging.value = true
  dragOffset.value = {
    x: e.clientX - position.value.x,
    y: e.clientY - position.value.y
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  if (!isDragging.value) return
  position.value = {
    x: Math.max(0, e.clientX - dragOffset.value.x),
    y: Math.max(0, e.clientY - dragOffset.value.y)
  }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 调整大小功能
const startResize = (e) => {
  isResizing.value = true
  resizeStart.value = {
    x: e.clientX,
    y: e.clientY,
    width: width.value,
    height: height.value
  }
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
}

const onResize = (e) => {
  if (!isResizing.value) return
  width.value = Math.max(400, resizeStart.value.width + (e.clientX - resizeStart.value.x))
  height.value = Math.max(300, resizeStart.value.height + (e.clientY - resizeStart.value.y))
}

const stopResize = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

// 置顶
const bringToFront = () => {
  emit('focus')
}

// 关闭和最小化
const close = () => {
  emit('close')
}

const minimize = () => {
  emit('minimize')
}

// 格式化日期时间
const formatDateTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取优先级标签
const getPriorityLabel = (priority) => {
  const labels = {
    'HIGH': '高',
    'MEDIUM': '中',
    'LOW': '低',
    'high': '高',
    'medium': '中',
    'low': '低'
  }
  return labels[priority] || '中'
}

// 生命周期
onMounted(() => {
  loadResumeDetail()
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
})
</script>

<style scoped>
.resume-modal-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.resume-modal-wrapper > * {
  pointer-events: auto;
}

.resume-modal {
  position: absolute;
  background: var(--bg0);
  border-radius: 12px;
  border: 1px solid var(--stroke);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 400px;
  min-height: 300px;
}

.resume-modal.is-dragging {
  opacity: 0.95;
  box-shadow: 0 32px 64px rgba(0, 0, 0, 0.5);
}

/* 标题栏 */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--panel);
  border-bottom: 1px solid var(--stroke);
  cursor: grab;
  user-select: none;
}

.modal-header:active {
  cursor: grabbing;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text);
  font-size: 14px;
  font-weight: 500;
}

.header-title svg {
  color: var(--brand);
}

.filename {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-actions {
  display: flex;
  gap: 6px;
}

.action-btn {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: var(--panel2);
}

.action-btn.close:hover {
  background: #ef4444;
  color: white;
}

.action-btn.minimize:hover {
  color: var(--text);
}

/* 标签页 */
.modal-tabs {
  display: flex;
  gap: 4px;
  padding: 8px 12px;
  background: var(--bg0);
  border-bottom: 1px solid var(--stroke);
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  background: var(--panel);
  color: var(--text);
}

.tab-btn.active {
  background: var(--brand);
  color: white;
}

.tab-badge {
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  background: var(--warn);
  color: white;
}

.tab-badge.analyzing {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 内容区域 */
.modal-body {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.tab-panel {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

/* 加载和错误状态 */
.loading-state,
.error-state,
.empty-state,
.analyzing-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: var(--muted);
  text-align: center;
  padding: 32px;
}

.loading-spinner,
.btn-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--stroke);
  border-top-color: var(--brand);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border-width: 2px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-retry {
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  background: var(--brand);
  color: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-retry:hover {
  background: var(--panel-hover);
}

/* 分析中动画 */
.analyzing-animation {
  position: relative;
  width: 60px;
  height: 60px;
}

.pulse-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid var(--brand);
  opacity: 0;
  animation: pulse-ring 2s ease-out infinite;
}

.pulse-ring:nth-child(2) {
  animation-delay: 0.5s;
}

.pulse-ring:nth-child(3) {
  animation-delay: 1s;
}

@keyframes pulse-ring {
  0% {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.5);
    opacity: 0;
  }
}

/* 简历内容 */
.resume-content {
  height: 100%;
  overflow-y: auto;
}

.resume-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text);
  margin: 0;
}

/* 分析内容 */
.analysis-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.analysis-section {
  background: var(--panel);
  border-radius: 10px;
  padding: 16px;
  border: 1px solid var(--stroke);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 12px;
}

.section-header svg {
  color: var(--brand);
}

.section-header.success svg {
  color: var(--ok);
}

.section-header.warning svg {
  color: var(--warn);
}

/* 核心评价 */
.evaluation-card {
  background: var(--bg0);
  border-radius: 8px;
  padding: 16px;
}

.summary {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text);
  margin-bottom: 16px;
}

.score-display {
  display: flex;
  gap: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--stroke);
}

.score-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.score-label {
  font-size: 12px;
  color: var(--muted);
}

.score-value {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.score-number {
  font-size: 28px;
  font-weight: 700;
  color: var(--brand);
}

.score-total {
  font-size: 14px;
  color: var(--muted);
}

.score-time {
  font-size: 13px;
  color: var(--text);
}

/* 优势标签 */
.strengths-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.strength-tag {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(34, 197, 94, 0.15);
  color: var(--ok);
  border: 1px solid rgba(34, 197, 94, 0.3);
}

/* 建议列表 */
.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestion-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: var(--bg0);
  border-radius: 8px;
  border-left: 3px solid var(--warn);
}

.suggestion-item.high {
  border-left-color: var(--bad);
}

.suggestion-item.medium {
  border-left-color: var(--warn);
}

.suggestion-item.low {
  border-left-color: var(--ok);
}

.priority-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  height: fit-content;
}

.priority-badge.high {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.priority-badge.medium {
  background: rgba(245, 158, 11, 0.2);
  color: var(--warn);
}

.priority-badge.low {
  background: rgba(54, 211, 153, 0.2);
  color: var(--ok);
}

.suggestion-content {
  flex: 1;
}

.suggestion-issue {
  font-size: 13px;
  color: var(--text);
  margin-bottom: 4px;
}

.suggestion-recommendation {
  font-size: 12px;
  color: var(--muted);
}

.recommendation-label {
  color: var(--brand);
  font-weight: 500;
}

/* 技能评估 */
.skills-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skill-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.skill-name {
  width: 100px;
  font-size: 13px;
  color: var(--text);
  flex-shrink: 0;
}

.skill-bar {
  flex: 1;
  height: 8px;
  background: var(--stroke);
  border-radius: 4px;
  overflow: hidden;
}

.skill-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--brand), var(--panel-hover));
  border-radius: 4px;
  transition: width 0.5s ease;
}

.skill-score {
  width: 32px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand);
}

/* 操作按钮 */
.analysis-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background: var(--brand);
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action:hover:not(:disabled) {
  background: var(--panel-hover);
  transform: translateY(-1px);
}

.btn-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-action.secondary {
  background: var(--panel);
  color: var(--text);
  border: 1px solid var(--stroke);
}

.btn-action.secondary:hover:not(:disabled) {
  background: var(--panel2);
  border-color: var(--brand);
}

/* 调整大小手柄 */
.resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 16px;
  height: 16px;
  cursor: se-resize;
  background: linear-gradient(135deg, transparent 50%, var(--stroke) 50%);
  border-bottom-right-radius: 12px;
}

.resize-handle:hover {
  background: linear-gradient(135deg, transparent 50%, var(--brand) 50%);
}

/* 滚动条 */
.tab-panel::-webkit-scrollbar {
  width: 6px;
}

.tab-panel::-webkit-scrollbar-track {
  background: transparent;
}

.tab-panel::-webkit-scrollbar-thumb {
  background: var(--stroke);
  border-radius: 3px;
}

.tab-panel::-webkit-scrollbar-thumb:hover {
  background: var(--muted);
}

/* 雷达图 */
.radar-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.radar-container {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

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
  stroke: var(--stroke);
  stroke-width: 1;
}

.axis-line {
  stroke: var(--stroke);
  stroke-width: 1;
}

.data-area {
  fill: var(--brand);
  stroke: none;
}

.data-border {
  stroke: var(--brand);
  stroke-width: 2;
}

.data-point {
  fill: var(--brand);
  stroke: var(--bg0);
  stroke-width: 2;
}

.axis-label {
  font-size: 11px;
  fill: var(--muted);
}

.value-label {
  font-size: 10px;
  fill: var(--text);
  font-weight: 600;
}

/* 多维度评分条 */
.dimension-scores {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

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
  color: var(--text);
}

.progress-value {
  font-size: 12px;
  color: var(--muted);
  font-weight: 500;
}

.progress-track {
  height: 8px;
  background: var(--stroke);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

/* 建议分组 */
.suggestion-count {
  font-size: 12px;
  color: var(--muted);
  font-weight: 400;
  margin-left: 4px;
}

.suggestion-group {
  margin-bottom: 16px;
}

.suggestion-group:last-child {
  margin-bottom: 0;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--stroke);
}

.group-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.category-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  background: var(--panel2);
  color: var(--muted);
  margin-bottom: 8px;
  display: inline-block;
}
</style>
