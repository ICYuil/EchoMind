<template>
  <div class="interview-history-page">
    <!-- 头部区域 -->
    <MotionDiv
      tag="div"
      class="page-header"
      :initial="{ opacity: 0, y: -20 }"
      :animate="{ opacity: 1, y: 0 }"
      :transition="{ duration: 0.5 }"
    >
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <div>
          <h1 class="page-title">面试历史</h1>
          <p class="page-subtitle">查看所有模拟面试记录和统计</p>
        </div>
      </div>
      
      <!-- 搜索框 -->
      <div class="search-box">
        <svg class="search-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <path d="m21 21-4.35-4.35"/>
        </svg>
        <input 
          v-model="searchKeyword"
          type="text"
          placeholder="搜索简历名称..."
          class="search-input"
        />
      </div>
    </MotionDiv>

    <!-- 统计卡片区域 -->
    <div class="stats-grid">
      <MotionDiv
        v-for="(stat, index) in statsCards"
        :key="stat.key"
        class="stat-card"
        :initial="{ opacity: 0, y: 20 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.5, delay: index * 0.1 }"
        :while-hover="{ y: -4, scale: 1.02 }"
      >
        <div class="stat-icon" :class="stat.color">
          <component :is="stat.icon" width="24" height="24" />
        </div>
        <div class="stat-content">
          <p class="stat-label">{{ stat.label }}</p>
          <p class="stat-value">
            {{ stat.value }}
            <span v-if="stat.suffix" class="stat-suffix">{{ stat.suffix }}</span>
          </p>
        </div>
      </MotionDiv>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 面试列表 -->
    <MotionDiv
      v-else
      class="interview-list-card"
      :initial="{ opacity: 0, y: 20 }"
      :animate="{ opacity: 1, y: 0 }"
      :transition="{ duration: 0.5, delay: 0.3 }"
    >
      <div class="list-header">
        <h3 class="list-title">面试记录</h3>
        <div class="list-filters">
          <button 
            v-for="filter in filters" 
            :key="filter.key"
            class="filter-btn"
            :class="{ active: currentFilter === filter.key }"
            @click="currentFilter = filter.key"
          >
            {{ filter.label }}
          </button>
        </div>
      </div>

      <!-- 使用虚拟列表 -->
      <VirtualList
        v-if="filteredInterviews.length > 0"
        ref="virtualListRef"
        :items="filteredInterviews"
        :item-height="80"
        :buffer-size="3"
        class="interview-virtual-list"
      >
        <template #default="{ item: interview, index }">
          <MotionDiv
            class="interview-item"
            :initial="{ opacity: 0, x: -20 }"
            :animate="{ opacity: 1, x: 0 }"
            :transition="{ duration: 0.3, delay: index * 0.05 }"
            :while-hover="{ backgroundColor: 'rgba(99, 102, 241, 0.03)' }"
            @click="viewInterview(interview)"
          >
            <div class="item-main">
              <div class="resume-info">
                <div class="file-icon">
                  <FileText width="20" height="20" />
                </div>
                <div class="resume-details">
                  <p class="resume-name">{{ interview.resumeFilename || '未知简历' }}</p>
                  <p class="resume-meta">
                    <span class="meta-item">{{ interview.totalQuestions || 8 }} 题</span>
                    <span class="meta-dot">·</span>
                    <span class="meta-item">{{ formatDate(interview.createdAt) }}</span>
                  </p>
                </div>
              </div>
              
              <div class="item-status">
                <StatusBadge :interview="interview" />
              </div>
              
              <div class="item-score">
                <ScoreDisplay 
                  v-if="isEvaluateCompleted(interview) && interview.overallScore !== null"
                  :score="interview.overallScore"
                />
                <span v-else-if="isEvaluating(interview)" class="evaluating-badge">
                  <Loader2 width="14" height="14" class="animate-spin" />
                  评估中
                </span>
                <span v-else class="no-score">-</span>
              </div>
            </div>
            
            <div class="item-actions" @click.stop>
              <button 
                v-if="isEvaluateCompleted(interview)"
                class="action-btn"
                title="导出PDF"
                @click="exportInterview(interview)"
                :disabled="exporting === interview.sessionId"
              >
                <Download v-if="exporting !== interview.sessionId" width="16" height="16" />
                <Loader2 v-else width="16" height="16" class="animate-spin" />
              </button>
              <button 
                class="action-btn delete"
                title="删除"
                @click="confirmDelete(interview)"
              >
                <Trash2 width="16" height="16" />
              </button>
              <ChevronRight width="20" height="20" class="arrow-icon" />
            </div>
          </MotionDiv>
        </template>
      </VirtualList>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon">
          <Users width="48" height="48" />
        </div>
        <h3 class="empty-title">暂无面试记录</h3>
        <p class="empty-desc">开始一次模拟面试后，记录将显示在这里</p>
      </div>
    </MotionDiv>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showDeleteModal" class="modal-overlay" @click="showDeleteModal = false">
          <MotionDiv
            class="modal-content"
            :initial="{ opacity: 0, scale: 0.9 }"
            :animate="{ opacity: 1, scale: 1 }"
            :transition="{ type: 'spring', stiffness: 300, damping: 25 }"
            @click.stop
          >
            <div class="modal-header">
              <AlertCircle width="24" height="24" class="warning-icon" />
              <h3>确认删除</h3>
            </div>
            <div class="modal-body">
              <p>确定要删除这次面试记录吗？此操作无法撤销。</p>
              <div v-if="deleteTarget" class="delete-info">
                <p><strong>简历：</strong>{{ deleteTarget.resumeFilename }}</p>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn secondary" @click="showDeleteModal = false">取消</button>
              <button class="btn danger" @click="handleDelete" :disabled="deleting">
                <Loader2 v-if="deleting" width="16" height="16" class="animate-spin" />
                {{ deleting ? '删除中...' : '确认删除' }}
              </button>
            </div>
          </MotionDiv>
        </div>
      </Transition>
    </Teleport>

    <!-- 面试详情弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showDetailModal" class="detail-modal-overlay" @click="closeDetailModal">
          <MotionDiv
            class="detail-modal-content"
            :initial="{ opacity: 0, scale: 0.95, y: 20 }"
            :animate="{ opacity: 1, scale: 1, y: 0 }"
            :transition="{ type: 'spring', stiffness: 300, damping: 25 }"
            @click.stop
          >
            <!-- 弹窗头部 -->
            <div class="detail-modal-header">
              <div class="detail-header-left">
                <h3 class="detail-modal-title">面试详情</h3>
                <p v-if="currentInterview" class="detail-modal-subtitle">
                  {{ currentInterview.resumeFilename || '未知简历' }} · {{ formatDate(currentInterview.createdAt) }}
                </p>
              </div>
              <div class="detail-header-actions">
                <button 
                  v-if="currentInterview && isEvaluateCompleted(currentInterview)"
                  class="detail-export-btn" 
                  @click="exportDetailPDF"
                  :disabled="detailExporting"
                >
                  <Download v-if="!detailExporting" width="16" height="16" />
                  <Loader2 v-else width="16" height="16" class="animate-spin" />
                  导出PDF
                </button>
                <button class="detail-close-btn" @click="closeDetailModal">
                  <X width="20" height="20" />
                </button>
              </div>
            </div>

            <!-- 弹窗内容 -->
            <div class="detail-modal-body">
              <!-- 加载状态 -->
              <div v-if="detailLoading" class="detail-loading-state">
                <div class="loading-spinner"></div>
                <p>加载中...</p>
              </div>

              <!-- 错误状态 -->
              <div v-else-if="detailError" class="detail-error-state">
                <AlertCircle width="48" height="48" />
                <p>{{ detailError }}</p>
                <button class="btn primary" @click="closeDetailModal">关闭</button>
              </div>

              <!-- 详情内容 -->
              <div v-else-if="currentInterview" class="detail-content">
                <!-- 顶部评分区域 -->
                <div class="detail-top-section">
                  <!-- 总分卡片 -->
                  <div class="detail-score-card">
                    <div class="detail-score-header">
                      <h4>面试评分</h4>
                      <span class="detail-score-badge" :class="getScoreLevel(currentInterview.overallScore)">
                        {{ getScoreLevelText(currentInterview.overallScore) }}
                      </span>
                    </div>
                    <div class="detail-score-main">
                      <div class="detail-score-circle">
                        <svg class="detail-score-ring" viewBox="0 0 140 140">
                          <circle cx="70" cy="70" r="62" fill="none" stroke="#f1f5f9" stroke-width="10"/>
                          <circle 
                            cx="70" cy="70" r="62" 
                            fill="none" 
                            :stroke="getScoreColor(currentInterview.overallScore)"
                            stroke-width="10"
                            stroke-linecap="round"
                            :stroke-dasharray="2 * Math.PI * 62"
                            :stroke-dashoffset="2 * Math.PI * 62 - (currentInterview.overallScore / 100) * 2 * Math.PI * 62"
                            class="score-progress"
                          />
                        </svg>
                        <div class="detail-score-value">
                          <span class="detail-score-number">{{ currentInterview.overallScore ?? '-' }}</span>
                          <span class="detail-score-total">/100</span>
                        </div>
                      </div>
                      <p class="detail-score-feedback">{{ currentInterview.overallFeedback || '表现良好，展示了扎实的技术基础。' }}</p>
                    </div>
                  </div>

                  <!-- 面试信息卡片 -->
                  <div class="detail-info-card">
                    <div class="detail-info-header">
                      <h4>面试信息</h4>
                    </div>
                    <div class="detail-info-content">
                      <div class="detail-info-item">
                        <span class="detail-info-label">面试岗位：</span>
                        <span class="detail-info-value">{{ getJobTypeText(currentInterview.jobId) }}</span>
                      </div>
                      <div class="detail-info-item">
                        <span class="detail-info-label">总题数：</span>
                        <span class="detail-info-value">{{ currentInterview.totalQuestions || 0 }} 题</span>
                      </div>
                      <div class="detail-info-item">
                        <span class="detail-info-label">创建时间：</span>
                        <span class="detail-info-value">{{ formatDate(currentInterview.createdAt) }}</span>
                      </div>
                      <div class="detail-info-item" v-if="currentInterview.completedAt">
                        <span class="detail-info-label">完成时间：</span>
                        <span class="detail-info-value">{{ formatDate(currentInterview.completedAt) }}</span>
                      </div>
                      <div class="detail-info-item">
                        <span class="detail-info-label">评估状态：</span>
                        <span class="detail-info-value" :class="getEvaluateStatusClass(currentInterview.evaluateStatus)">
                          {{ getEvaluateStatusText(currentInterview.evaluateStatus) }}
                        </span>
                      </div>
                      <div class="detail-info-item" v-if="currentInterview.evaluateError">
                        <span class="detail-info-label">评估错误：</span>
                        <span class="detail-info-value error">{{ currentInterview.evaluateError }}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 多维度评分 -->
                <div v-if="currentInterview.categoryScores && currentInterview.categoryScores.length > 0" class="detail-dimensions-card">
                  <div class="detail-dimensions-header">
                    <h4>多维度评分</h4>
                  </div>
                  <div class="detail-dimensions-list">
                    <div v-for="(category, index) in currentInterview.categoryScores" :key="index" class="detail-dimension-item">
                      <div class="detail-dimension-header">
                        <span class="detail-dimension-name">{{ category.category }}</span>
                        <span class="detail-dimension-score">{{ category.score }}分</span>
                      </div>
                      <div class="detail-dimension-bar">
                        <div class="detail-dimension-fill" :style="{ width: category.score + '%', backgroundColor: getScoreColor(category.score) }"></div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 优势和改进建议 -->
                <div class="detail-middle-section">
                  <div v-if="currentInterview.strengths && currentInterview.strengths.length > 0" class="detail-section-card">
                    <div class="detail-section-header success">
                      <CheckCircle2 width="18" height="18" />
                      <h4>表现优势</h4>
                    </div>
                    <ul class="detail-section-list">
                      <li v-for="(strength, i) in currentInterview.strengths" :key="i">
                        <span class="detail-list-dot success"></span>
                        <span>{{ strength }}</span>
                      </li>
                    </ul>
                  </div>

                  <div v-if="currentInterview.improvements && currentInterview.improvements.length > 0" class="detail-section-card">
                    <div class="detail-section-header warning">
                      <AlertTriangle width="18" height="18" />
                      <h4>改进建议</h4>
                    </div>
                    <ul class="detail-section-list">
                      <li v-for="(improvement, i) in currentInterview.improvements" :key="i">
                        <span class="detail-list-dot warning"></span>
                        <span>{{ improvement }}</span>
                      </li>
                    </ul>
                  </div>

                  <!-- 学习计划 -->
                  <div v-if="currentInterview.learningPlan && currentInterview.learningPlan.length > 0" class="detail-section-card learning-plan-card">
                    <div class="detail-section-header info">
                      <BookOpen width="18" height="18" />
                      <h4>学习计划</h4>
                    </div>
                    <p class="learning-plan-desc">
                      <Lightbulb width="14" height="14" />
                      根据您的面试回答，我们从知识库中找出了能帮助您提升的知识点
                    </p>
                    <ul class="detail-section-list">
                      <li v-for="(plan, i) in currentInterview.learningPlan" :key="i">
                        <span class="detail-list-dot info"></span>
                        <span>{{ plan }}</span>
                      </li>
                    </ul>
                    <button class="btn-go-study" @click="goToStudyPlan(currentInterview)">
                      <GraduationCap width="16" height="16" />
                      <span>前往学习这些知识点</span>
                      <ChevronRight width="16" height="16" />
                    </button>
                  </div>
                </div>

                <!-- 问答记录详情 -->
                <div v-if="currentInterview.answers && currentInterview.answers.length > 0" class="detail-questions-section">
                  <div class="detail-section-header">
                    <MessageSquare width="18" height="18" />
                    <h4>问答记录详情</h4>
                    <span class="detail-question-count">共 {{ currentInterview.answers.length }} 题</span>
                  </div>
                  
                  <div class="detail-questions-list">
                    <div 
                      v-for="(answer, idx) in currentInterview.answers" 
                      :key="idx"
                      class="detail-question-card"
                      :class="{ expanded: expandedQuestions.has(idx) }"
                    >
                      <div class="detail-question-header" @click="toggleQuestion(idx)">
                        <div class="detail-question-meta">
                          <span class="detail-question-index">Q{{ answer.questionIndex + 1 }}</span>
                          <span class="detail-question-category">{{ answer.category || '综合' }}</span>
                          <span class="detail-question-score" :class="getScoreClass(answer.score)">
                            {{ answer.score }}分
                          </span>
                        </div>
                        <ChevronDown 
                          width="18" 
                          height="18" 
                          class="detail-expand-icon"
                          :class="{ expanded: expandedQuestions.has(idx) }"
                        />
                      </div>

                      <div class="detail-question-content">
                        <p class="detail-question-text">{{ answer.question }}</p>
                      </div>

                      <div v-show="expandedQuestions.has(idx)" class="detail-question-detail">
                        <div class="detail-box user-answer">
                          <div class="detail-box-label">
                            <User width="14" height="14" />
                            你的回答
                          </div>
                          <p class="detail-box-text" :class="{ 'no-answer': !answer.userAnswer || answer.userAnswer === '不知道' }">
                            {{ answer.userAnswer || '(未回答)' }}
                          </p>
                        </div>

                        <div v-if="answer.feedback" class="detail-box ai-feedback">
                          <div class="detail-box-label">
                            <Bot width="14" height="14" />
                            AI 评价
                          </div>
                          <p class="detail-box-text">{{ answer.feedback }}</p>
                        </div>

                        <div v-if="answer.referenceAnswer" class="detail-box reference">
                          <div class="detail-box-label">
                            <BookOpen width="14" height="14" />
                            参考答案
                          </div>
                          <pre class="detail-reference-text">{{ answer.referenceAnswer }}</pre>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </MotionDiv>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  Users, 
  CheckCircle, 
  TrendingUp, 
  FileText, 
  Download, 
  Trash2, 
  ChevronRight,
  Loader2,
  AlertCircle,
  X,
  CheckCircle2,
  AlertTriangle,
  MessageSquare,
  ChevronDown,
  User,
  Bot,
  BookOpen,
  Lightbulb,
  GraduationCap
} from 'lucide-vue-next'
import { MotionDiv } from '../components/motion'
import VirtualList from '../components/VirtualList.vue'
import { interviewApi } from '../api/interview'
import { resumeApi } from '../api/resume'
import StatusBadge from '../components/StatusBadge.vue'
import ScoreDisplay from '../components/ScoreDisplay.vue'

const router = useRouter()
const route = useRoute()

// 状态
const interviews = ref([])
const loading = ref(true)
const searchKeyword = ref('')
const currentFilter = ref('all')
const exporting = ref(null)
const deleting = ref(false)
const showDeleteModal = ref(false)
const deleteTarget = ref(null)
const virtualListRef = ref(null)
const pollingTimer = ref(null)

// 详情弹窗状态
const showDetailModal = ref(false)
const detailLoading = ref(false)
const detailError = ref(null)
const currentInterview = ref(null)
const detailExporting = ref(false)
const expandedQuestions = ref(new Set())

// 筛选选项
const filters = [
  { key: 'all', label: '全部' },
  { key: 'completed', label: '已完成' },
  { key: 'in_progress', label: '进行中' }
]

// 统计卡片数据
const statsCards = computed(() => [
  {
    key: 'total',
    label: '面试总数',
    value: stats.value?.totalCount || 0,
    icon: Users,
    color: 'blue'
  },
  {
    key: 'completed',
    label: '已完成',
    value: stats.value?.completedCount || 0,
    icon: CheckCircle,
    color: 'green'
  },
  {
    key: 'average',
    label: '平均分数',
    value: stats.value?.averageScore || 0,
    suffix: '分',
    icon: TrendingUp,
    color: 'purple'
  }
])

// 统计数据
const stats = computed(() => {
  const total = interviews.value.length
  const completed = interviews.value.filter(i => isEvaluateCompleted(i)).length
  const scored = interviews.value.filter(i => isEvaluateCompleted(i) && i.overallScore !== null)
  const average = scored.length > 0 
    ? Math.round(scored.reduce((sum, i) => sum + i.overallScore, 0) / scored.length)
    : 0
  
  return { totalCount: total, completedCount: completed, averageScore: average }
})

// 筛选后的面试列表
const filteredInterviews = computed(() => {
  let result = interviews.value
  
  // 搜索筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(i => 
      (i.resumeFilename || '').toLowerCase().includes(keyword)
    )
  }
  
  // 状态筛选
  if (currentFilter.value === 'completed') {
    result = result.filter(i => isEvaluateCompleted(i))
  } else if (currentFilter.value === 'in_progress') {
    // 显示所有未完成的面试（包括进行中、已中断、已提交但未评估的）
    result = result.filter(i => !isEvaluateCompleted(i))
  }
  
  return result
})

// 状态判断函数
function isEvaluateCompleted(interview) {
  if (interview.evaluateStatus === 'COMPLETED') return true
  if (interview.status === 'EVALUATED') return true
  return false
}

function isEvaluating(interview) {
  return interview.evaluateStatus === 'PENDING' || interview.evaluateStatus === 'PROCESSING'
}

function isEvaluateFailed(interview) {
  return interview.evaluateStatus === 'FAILED'
}

// 加载数据 - 通过获取所有简历来汇总面试记录（与 EchoMind 前端保持一致）
const loadData = async () => {
  try {
    loading.value = true
    
    // 获取所有简历
    const resumes = await resumeApi.getResumes()
    const allInterviews = []
    
    // 遍历每个简历，获取其面试记录
    for (const resume of resumes) {
      const detail = await resumeApi.getResumeDetail(resume.id)
      if (detail.interviews && detail.interviews.length > 0) {
        detail.interviews.forEach(interview => {
          allInterviews.push({
            ...interview,
            resumeId: resume.id,
            resumeFilename: resume.filename
          })
        })
      }
    }
    
    // 按创建时间倒序排序
    const sortedInterviews = allInterviews.sort((a, b) => {
      const timeA = new Date(a.createdAt || a.startTime || 0).getTime()
      const timeB = new Date(b.createdAt || b.startTime || 0).getTime()
      return timeB - timeA
    })
    
    interviews.value = sortedInterviews
  } catch (err) {
    console.error('加载面试历史失败', err)
  } finally {
    loading.value = false
  }
}

// 轮询更新（有评估中的面试时）
const startPolling = () => {
  if (pollingTimer.value) return
  pollingTimer.value = setInterval(() => {
    const hasEvaluating = interviews.value.some(i => isEvaluating(i))
    if (hasEvaluating) {
      loadData()
    }
  }, 5000)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

// 查看面试详情 - 改为弹窗形式
const viewInterview = async (interview) => {
  showDetailModal.value = true
  detailLoading.value = true
  detailError.value = null
  expandedQuestions.value = new Set()
  
  try {
    // 获取面试详情（已包含所有报告数据）
    const detail = await interviewApi.getInterviewDetail(interview.sessionId)
    console.log('面试详情数据:', detail)
    
    currentInterview.value = detail
    
    // 默认展开所有问题
    if (currentInterview.value.answers) {
      expandedQuestions.value = new Set(currentInterview.value.answers.map((_, idx) => idx))
    }
  } catch (err) {
    console.error('加载面试详情失败:', err)
    detailError.value = err.message || '加载失败，请重试'
  } finally {
    detailLoading.value = false
  }
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  currentInterview.value = null
  detailError.value = null
  expandedQuestions.value = new Set()
}

// 切换问题展开/收起
const toggleQuestion = (index) => {
  const newSet = new Set(expandedQuestions.value)
  if (newSet.has(index)) {
    newSet.delete(index)
  } else {
    newSet.add(index)
  }
  expandedQuestions.value = newSet
}

// 从弹窗导出PDF
const exportDetailPDF = async () => {
  if (!currentInterview.value) return
  
  detailExporting.value = true
  try {
    const blob = await interviewApi.exportReport(currentInterview.value.sessionId)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `面试报告_${currentInterview.value.sessionId.slice(-8)}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch (err) {
    alert('导出失败，请重试')
  } finally {
    detailExporting.value = false
  }
}

// 导出面试
const exportInterview = async (interview) => {
  try {
    exporting.value = interview.sessionId
    // 导出逻辑
    const blob = await interviewApi.exportReport(interview.sessionId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `面试报告_${interview.resumeFilename || '未知'}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    console.error('导出失败', err)
    alert('导出失败：' + err.message)
  } finally {
    exporting.value = null
  }
}

// 删除确认
const confirmDelete = (interview) => {
  deleteTarget.value = interview
  showDeleteModal.value = true
}

// 执行删除
const handleDelete = async () => {
  if (!deleteTarget.value) return
  
  try {
    deleting.value = true
    await interviewApi.deleteSession(deleteTarget.value.sessionId)
    interviews.value = interviews.value.filter(
      i => i.sessionId !== deleteTarget.value.sessionId
    )
    showDeleteModal.value = false
  } catch (err) {
    console.error('删除失败', err)
    alert('删除失败：' + err.message)
  } finally {
    deleting.value = false
    deleteTarget.value = null
  }
}

// 返回
const goBack = () => {
  router.push('/app/home')
}

// 跳转到学习计划页面
const goToStudyPlan = (interview) => {
  closeDetailModal()
  router.push({
    path: '/app/study-plan',
    query: { sessionId: interview.sessionId }
  })
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取分数颜色
const getScoreColor = (score) => {
  if (score >= 80) return '#22c55e'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

// 获取分数等级
const getScoreLevel = (score) => {
  if (score >= 80) return 'excellent'
  if (score >= 60) return 'good'
  return 'needs-improvement'
}

// 获取分数等级文本
const getScoreLevelText = (score) => {
  if (score >= 80) return '优秀'
  if (score >= 60) return '良好'
  return '需改进'
}

// 获取分数样式类
const getScoreClass = (score) => {
  if (score >= 80) return 'high'
  if (score >= 60) return 'medium'
  return 'low'
}

// 获取岗位类型文本
const getJobTypeText = (jobId) => {
  const jobTypes = {
    1: '后端开发',
    2: '前端开发',
    3: '测试工程师'
  }
  return jobTypes[jobId] || '未知岗位'
}

// 获取评估状态文本
const getEvaluateStatusText = (status) => {
  const statusMap = {
    'PENDING': '待评估',
    'PROCESSING': '评估中',
    'COMPLETED': '评估完成',
    'FAILED': '评估失败'
  }
  return statusMap[status] || '未知状态'
}

// 获取评估状态样式类
const getEvaluateStatusClass = (status) => {
  const classMap = {
    'PENDING': 'pending',
    'PROCESSING': 'processing',
    'COMPLETED': 'completed',
    'FAILED': 'failed'
  }
  return classMap[status] || ''
}

onMounted(() => {
  loadData()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})

// 监听路由变化，当从其他页面跳转回来时刷新数据
watch(() => route.path, (newPath, oldPath) => {
  if (newPath === '/app/records' && oldPath !== '/app/records') {
    loadData()
  }
})
</script>

<style scoped>
.interview-history-page {
  padding: 24px;
  width: 100%;
  max-width: none;
  margin: 0;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-back {
  width: 40px;
  height: 40px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 4px 0 0;
}

.search-box {
  position: relative;
  width: 280px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
}

.search-input {
  width: 100%;
  padding: 10px 12px 10px 40px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  background: white;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
  cursor: pointer;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.blue {
  background: #eff6ff;
  color: #3b82f6;
}

.stat-icon.green {
  background: #f0fdf4;
  color: #22c55e;
}

.stat-icon.purple {
  background: #faf5ff;
  color: #a855f7;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.stat-suffix {
  font-size: 16px;
  font-weight: 500;
  color: #9ca3af;
  margin-left: 4px;
}

.interview-list-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
  overflow: hidden;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
}

.list-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.list-filters {
  display: flex;
  gap: 8px;
}

.filter-btn {
  padding: 6px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: white;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  background: #f9fafb;
}

.filter-btn.active {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
}

.interview-virtual-list {
  height: 500px;
}

.interview-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background-color 0.2s;
}

.interview-item:last-child {
  border-bottom: none;
}

.item-main {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
}

.resume-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.file-icon {
  width: 40px;
  height: 40px;
  background: #f3f4f6;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}

.resume-details {
  flex: 1;
}

.resume-name {
  font-size: 15px;
  font-weight: 500;
  color: #111827;
  margin: 0 0 4px;
}

.resume-meta {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-dot {
  opacity: 0.5;
}

.item-status {
  width: 100px;
}

.item-score {
  width: 120px;
  display: flex;
  justify-content: center;
}

.evaluating-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6366f1;
  padding: 6px 12px;
  background: #eef2ff;
  border-radius: 20px;
}

.no-score {
  font-size: 14px;
  color: #9ca3af;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.action-btn.delete:hover {
  background: #fef2f2;
  border-color: #fecaca;
  color: #ef4444;
}

.arrow-icon {
  color: #d1d5db;
  margin-left: 8px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #6b7280;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f4f6;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: #f9fafb;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d1d5db;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 400px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.warning-icon {
  color: #f59e0b;
}

.modal-body {
  padding: 20px 24px;
}

.modal-body p {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 16px;
}

.delete-info {
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
  font-size: 13px;
}

.delete-info p {
  margin: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f3f4f6;
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn.secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn.secondary:hover {
  background: #e5e7eb;
}

.btn.danger {
  background: #ef4444;
  color: white;
}

.btn.danger:hover:not(:disabled) {
  background: #dc2626;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* ===== 详情弹窗样式 ===== */
.detail-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
  padding: 20px;
}

.detail-modal-content {
  background: white;
  border-radius: 20px;
  width: 100%;
  max-width: 900px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.detail-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
  flex-shrink: 0;
}

.detail-header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.detail-modal-subtitle {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

.detail-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-export-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-export-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.detail-export-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.detail-close-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-close-btn:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #374151;
}

.detail-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.detail-loading-state,
.detail-error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  gap: 16px;
  color: #6b7280;
}

.detail-error-state {
  color: #ef4444;
}

.detail-error-state p {
  color: #6b7280;
}

/* 详情内容样式 */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-top-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@media (max-width: 768px) {
  .detail-top-section {
    grid-template-columns: 1fr;
  }
}

.detail-score-card {
  background: #f9fafb;
  border-radius: 16px;
  padding: 20px;
}

.detail-score-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.detail-score-header h4 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.detail-score-badge {
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.detail-score-badge.excellent {
  background: #f0fdf4;
  color: #16a34a;
}

.detail-score-badge.good {
  background: #fef3c7;
  color: #d97706;
}

.detail-score-badge.needs-improvement {
  background: #fef2f2;
  color: #dc2626;
}

.detail-score-main {
  display: flex;
  align-items: center;
  gap: 20px;
}

.detail-score-circle {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.detail-score-ring {
  transform: rotate(-90deg);
}

.score-progress {
  transition: stroke-dashoffset 0.8s ease;
}

.detail-score-value {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.detail-score-number {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
}

.detail-score-total {
  font-size: 12px;
  color: #9ca3af;
}

.detail-score-feedback {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
  margin: 0;
}

.detail-dimensions-card {
  background: #f9fafb;
  border-radius: 16px;
  padding: 20px;
}

.detail-dimensions-card h4 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 16px;
}

.detail-dimensions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-dimension-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-dimension-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-dimension-name {
  font-size: 13px;
  color: #4b5563;
}

.detail-dimension-score {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.detail-dimension-bar {
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.detail-dimension-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.detail-middle-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@media (max-width: 640px) {
  .detail-middle-section {
    grid-template-columns: 1fr;
  }
}

.detail-section-card {
  background: #f9fafb;
  border-radius: 16px;
  padding: 20px;
}

.detail-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.detail-section-header h4 {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.detail-section-header.success {
  color: #16a34a;
}

.detail-section-header.success h4 {
  color: #16a34a;
}

.detail-section-header.warning {
  color: #d97706;
}

.detail-section-header.warning h4 {
  color: #d97706;
}

.detail-section-header.info {
  color: #2563eb;
}

.detail-section-header.info h4 {
  color: #2563eb;
}

.detail-section-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-section-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
}

.detail-list-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  margin-top: 7px;
  flex-shrink: 0;
}

.detail-list-dot.success {
  background: #22c55e;
}

.detail-list-dot.warning {
  background: #f59e0b;
}

.detail-list-dot.info {
  background: #3b82f6;
}

/* 学习计划卡片样式 */
.learning-plan-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
}

.learning-plan-desc {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #0369a1;
  margin: 0 0 12px 0;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
}

.btn-go-study {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 16px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-go-study:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.3);
}

.btn-go-study svg:last-child {
  transition: transform 0.2s ease;
}

.btn-go-study:hover svg:last-child {
  transform: translateX(4px);
}

.detail-questions-section {
  background: #f9fafb;
  border-radius: 16px;
  padding: 20px;
}

.detail-questions-section .detail-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.detail-question-count {
  margin-left: auto;
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 10px;
}

.detail-questions-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-question-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.2s;
}

.detail-question-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.detail-question-card.expanded {
  border-color: #6366f1;
}

.detail-question-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
  background: #fafafa;
  transition: background 0.2s;
}

.detail-question-header:hover {
  background: #f3f4f6;
}

.detail-question-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-question-index {
  width: 28px;
  height: 28px;
  background: #6366f1;
  color: white;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.detail-question-category {
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

.detail-question-score {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.detail-question-score.high {
  background: #f0fdf4;
  color: #16a34a;
}

.detail-question-score.medium {
  background: #fef3c7;
  color: #d97706;
}

.detail-question-score.low {
  background: #fef2f2;
  color: #dc2626;
}

/* 面试信息卡片样式 */
.detail-info-card {
  background: #f9fafb;
  border-radius: 16px;
  padding: 20px;
}

.detail-info-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.detail-info-header h4 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.detail-info-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-info-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.detail-info-label {
  width: 80px;
  font-weight: 500;
  color: #6b7280;
  flex-shrink: 0;
}

.detail-info-value {
  flex: 1;
  color: #4b5563;
}

.detail-info-value.error {
  color: #ef4444;
  background: #fef2f2;
  padding: 4px 8px;
  border-radius: 4px;
  word-break: break-word;
}

/* 评估状态样式 */
.detail-info-value.pending {
  color: #f59e0b;
  background: #fef3c7;
  padding: 2px 8px;
  border-radius: 4px;
}

.detail-info-value.processing {
  color: #3b82f6;
  background: #eff6ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.detail-info-value.completed {
  color: #16a34a;
  background: #f0fdf4;
  padding: 2px 8px;
  border-radius: 4px;
}

.detail-info-value.failed {
  color: #dc2626;
  background: #fef2f2;
  padding: 2px 8px;
  border-radius: 4px;
}

.detail-expand-icon {
  color: #9ca3af;
  transition: transform 0.2s;
}

.detail-expand-icon.expanded {
  transform: rotate(180deg);
}

.detail-question-content {
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
}

.detail-question-text {
  font-size: 14px;
  color: #111827;
  margin: 0;
  line-height: 1.5;
}

.detail-question-detail {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-box {
  background: #f9fafb;
  border-radius: 8px;
  padding: 12px;
}

.detail-box-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 8px;
}

.detail-box.user-answer .detail-box-label {
  color: #3b82f6;
}

.detail-box.ai-feedback .detail-box-label {
  color: #8b5cf6;
}

.detail-box.reference .detail-box-label {
  color: #10b981;
}

.detail-box-text {
  font-size: 13px;
  color: #374151;
  margin: 0;
  line-height: 1.6;
}

.detail-box-text.no-answer {
  color: #9ca3af;
  font-style: italic;
}

.detail-reference-text {
  font-size: 13px;
  color: #374151;
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
  font-family: inherit;
}

.btn.primary {
  padding: 10px 20px;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn.primary:hover {
  background: #4f46e5;
}
</style>
