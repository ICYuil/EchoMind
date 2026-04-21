<template>
  <div class="interview-room page-with-bg" :class="{ 'fullscreen-mode': isFullscreen }">
    <!-- 中间：Live2D面试官 -->
    <div class="avatar-section">
        <!-- 在这里添加按钮组 -->
      <div class="avatar-header">
        <div class="button-group">
          <button class="icon-btn" @click="unlock" title="解锁音频">🔊</button>
          <FullscreenButton
            v-model="isFullscreen"
            @toggle="onFullscreenToggle"
          />
          <button class="icon-btn" @click="togglePause" :title="paused ? '继续' : '暂停'">
            {{ paused ? '▶' : '⏸' }}
          </button>
          <button class="icon-btn" @click="openSettings=true" title="设置">⚙</button>
        </div>
      </div>

      <div class="live2d-stage" :class="speaking ? 'speaking' : ''">
        <Live2DAvatar ref="avatar" @subtitle="subtitle=$event" @speaking="speaking=$event" @loaded="onAvatarLoaded" />
      </div>
      <!-- 当前状态提示 -->
      <div class="phase-indicator" :style="phaseColor">
        {{ phaseLabel }}
      </div>
    </div>

    <!-- 问题进度提醒 -->
    <div v-if="interviewSession.questions.length > 0 && currentQuestionObj.question" class="question-progress-panel">
      <div class="progress-header">
        <span class="progress-title">
          题目 {{ currentQuestionObj.questionIndex + 1 }} / {{ interviewSession.questions.length }}
        </span>
        <span class="progress-percent">{{ Math.round(progressPercent) }}%</span>
      </div>
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
    </div>

    <!-- 聊天区域：QQ/微信风格 - 单列表，新消息从底部添加 -->
    <div class="chat-area">
      <div class="chat-messages" ref="chatMessagesRef">
        <!-- 消息列表 -->
        <div class="messages-container">
          <div 
            v-for="(m, index) in allMessages" 
            :key="m.id" 
            class="message-row"
            :class="m.role"
          >
            <div class="message-bubble" :class="m.role">
              <div class="message-header">
                <span class="message-sender">{{ m.role === 'interviewer' ? '面试官' : '我' }}</span>
                <span v-if="m.role === 'interviewer' && m.category" class="question-type-tag">
                  {{ m.category }}
                </span>
              </div>
              <div class="message-text" :class="{ 'streaming': m.isStreaming }">
                {{ m.text }}
                <span v-if="m.isStreaming" class="streaming-cursor"></span>
              </div>
              <!-- 语音输入的ASR分数显示 -->
              <div v-if="m.role === 'candidate' && m.voiceAnalysis" class="voice-scores">
                <span class="score-tag confidence">
                  <span class="score-icon">🎯</span>
                  自信度: {{ m.voiceAnalysis.confidenceScore }}/10
                </span>
                <span class="score-tag clarity">
                  <span class="score-icon">🎤</span>
                  清晰度: {{ m.voiceAnalysis.clarityScore }}/10
                </span>
              </div>
            </div>
          </div>
          <div v-if="allMessages.length === 0" class="chat-placeholder">
            <div class="placeholder-icon">💬</div>
            <div>面试即将开始，请做好准备...</div>
          </div>
        </div>
        <!-- 滚动锚点 -->
        <div ref="scrollAnchor" class="scroll-anchor"></div>
      </div>
    </div>

    <!-- 底部输入区域 -->
    <div class="input-section">
      <!-- 手动输入 -->
      <div v-if="inputMode==='text'" class="text-input-bar">
        <button class="input-icon" @click="inputMode='voice'" title="切换语音">🎙</button>
        <textarea
          v-model="draft"
          class="input-field"
          rows="1"
          :disabled="!canAnswer"
          placeholder="输入你的回答…"
          @keydown.enter.exact.prevent="commitText"
        ></textarea>
        <button class="send-btn" :disabled="!canAnswer || !draft.trim()" @click="commitText">
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" fill="currentColor"/>
          </svg>
        </button>
      </div>

      <!-- 语音输入 -->
      <div v-else class="voice-input-bar">
        <button class="input-icon" @click="inputMode='text'" title="切换文字">⌨</button>
        <button
          class="voice-btn"
          :class="listening ? 'recording' : ''"
          :disabled="!canAnswer"
          @click="toggleVoice"
        >
          <span v-if="listening">
            <span class="recording-wave"></span>
            点击结束
          </span>
          <span v-else>按住说话</span>
        </button>
      </div>
    </div>

    <!-- 设置弹窗 -->
    <Modal
      :open="openSettings"
      title="设置"
      subtitle="修改设置会自动暂停面试。"
      @close="openSettings=false"
    >
      <div class="card soft" style="padding:12px 14px;">
        <div class="row space center">
          <div>
            <div class="muted2" style="font-size:12px;">输入方式</div>
            <div style="font-weight:950; margin-top:6px;">{{ inputMode==='text' ? '手动输入' : '语音输入' }}</div>
          </div>
          <div class="seg">
            <button class="seg-btn" :class="inputMode==='voice'?'on':''" @click="inputMode='voice'">语音</button>
            <button class="seg-btn" :class="inputMode==='text'?'on':''" @click="inputMode='text'">手动</button>
          </div>
        </div>

        <div class="row space center" style="margin-top:12px;">
          <div>
            <div class="muted2" style="font-size:12px;">思考时间</div>
            <div style="font-weight:950; margin-top:6px;">{{ cfg.thinkSeconds }} 秒</div>
          </div>
          <input type="range" min="5" max="60" step="5" v-model.number="cfg.thinkSeconds" style="width:260px;" />
        </div>

        <div style="margin-top:12px;">
          <div class="muted2" style="font-size:12px;">人声（TTS）</div>
          <div class="row gap10 wrap" style="margin-top:10px;">
            <select class="select" v-model="cfg.voice" style="min-width:260px;">
              <option value="alex">alex</option>
              <option value="anna">anna</option>
              <option value="bella">bella</option>
              <option value="benjamin">benjamin</option>
              <option value="charles">charles</option>
              <option value="claire">claire</option>
              <option value="david">david</option>
              <option value="diana">diana</option>
            </select>
            <div style="min-width:240px;">
              <div class="muted2" style="font-size:12px;">倍速：{{ cfg.rate }}</div>
              <input type="range" min="0.5" max="2.0" step="0.1" v-model.number="ttsRateNum" style="width:100%;" />
            </div>
          </div>

          <div class="row gap10 wrap" style="margin-top:12px;">
            <button class="btn ghost btn-glow" @click="unlock">解锁音频</button>
            <button class="btn btn-glow" @click="testSpeak">试播</button>
            <div style="flex:1;"></div>
            <button class="btn danger btn-glow" @click="showCompleteConfirm=true">结束面试</button>
            <button class="btn primary btn-glow" @click="applySettings">保存并继续</button>
          </div>
        </div>
      </div>
    </Modal>

    <!-- 提前交卷确认对话框 -->
    <Modal
      :open="showCompleteConfirm"
      title="提前交卷"
      subtitle="确定要提前交卷吗？未回答的问题将按0分计算。"
      @close="showCompleteConfirm=false"
    >
      <div class="card soft" style="padding:20px;">
        <div class="row gap10" style="justify-content: flex-end;">
          <button 
            class="btn ghost" 
            @click="showCompleteConfirm=false"
            :disabled="isSubmitting"
          >
            取消
          </button>
          <button 
            class="btn danger" 
            @click="finishInterview"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? '提交中...' : '确定交卷' }}
          </button>
        </div>
      </div>
    </Modal>

    <!-- 退出面试确认对话框 -->
    <Modal
      :open="showExitConfirm"
      title="确认退出面试"
      subtitle="您可以选择提前交卷或暂时退出，暂时退出后可以在下次继续"
      @close="showExitConfirm = false"
    >
      <div class="card soft" style="padding:20px;">
        <p style="margin-bottom: 20px; color: #64748b; line-height: 1.6;">
          <strong>提前交卷：</strong>结束本次面试并生成评价报告<br>
          <strong>暂时退出：</strong>保存当前进度，下次可以继续答题
        </p>
        <div class="row gap10" style="justify-content: flex-end;">
          <button 
            class="btn ghost" 
            @click="showExitConfirm = false"
          >
            取消
          </button>
          <button 
            class="btn warning" 
            @click="handleTempExit"
            :disabled="isExiting"
          >
            {{ isExiting ? '保存中...' : '暂时退出' }}
          </button>
          <button 
            class="btn primary" 
            @click="finishInterview"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? '提交中...' : '提前交卷' }}
          </button>
        </div>
      </div>
    </Modal>

    <!-- 未完成面试提示对话框 -->
    <Modal
      :open="showUnfinishedDialog"
      title="发现未完成的面试"
      :subtitle="unfinishedSession ? `您有一个进行中的面试（题目 ${unfinishedSession.currentQuestionIndex + 1}/${unfinishedSession.questions.length}），是否继续？` : ''"
      @close="handleStartNew"
    >
      <div class="card soft" style="padding:20px;">
        <p style="margin-bottom: 16px; color: #64748b;">
          您可以选择继续之前的面试进度，或者开始一场新的面试。
        </p>
        <div class="row gap10" style="justify-content: flex-end;">
          <button 
            class="btn ghost" 
            @click="handleStartNew"
          >
            开始新面试
          </button>
          <button 
            class="btn primary" 
            @click="handleContinueUnfinished"
          >
            继续面试
          </button>
        </div>
      </div>
    </Modal>

    <!-- 面试完成弹窗 -->
    <Modal
      :open="showInterviewCompleteModal"
      title="面试完成"
      @close="closeInterviewCompleteModal"
    >
      <div class="card soft" style="padding:20px;">
        <div v-if="interviewDetailLoading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>正在加载面试详情...</p>
        </div>
        <div v-else-if="interviewDetailError" class="error-state">
          <p>{{ interviewDetailError }}</p>
        </div>
        <div v-else-if="interviewDetail" class="detail-content">
          <div class="score-section">
            <h4>面试评分</h4>
            <div class="score-circle">
              <svg class="score-ring" viewBox="0 0 140 140">
                <circle cx="70" cy="70" r="62" fill="none" stroke="#f1f5f9" stroke-width="10"/>
                <circle 
                  cx="70" cy="70" r="62" 
                  fill="none" 
                  :stroke="getScoreColor(interviewDetail.overallScore)"
                  stroke-width="10"
                  stroke-linecap="round"
                  :stroke-dasharray="2 * Math.PI * 62"
                  :stroke-dashoffset="2 * Math.PI * 62 - (interviewDetail.overallScore / 100) * 2 * Math.PI * 62"
                  class="score-progress"
                />
              </svg>
              <div class="score-value">
                <span class="score-number">{{ interviewDetail.overallScore }}</span>
                <span class="score-total">/100</span>
              </div>
            </div>
            <p class="score-feedback">{{ interviewDetail.overallFeedback || '表现良好，展示了扎实的技术基础。' }}</p>
          </div>
          <div class="summary-section">
            <div v-if="interviewDetail.strengths && interviewDetail.strengths.length > 0" class="summary-card">
              <h5>表现优势</h5>
              <ul>
                <li v-for="(strength, i) in interviewDetail.strengths" :key="i">{{ strength }}</li>
              </ul>
            </div>
            <div v-if="interviewDetail.improvements && interviewDetail.improvements.length > 0" class="summary-card">
              <h5>改进建议</h5>
              <ul>
                <li v-for="(improvement, i) in interviewDetail.improvements" :key="i">{{ improvement }}</li>
              </ul>
            </div>
          </div>
          <div class="questions-summary">
            <h5>问答记录</h5>
            <p>共 {{ interviewDetail.answers?.length || 0 }} 个问题</p>
            <div v-if="interviewDetail.answers && interviewDetail.answers.length > 0" class="answers-list">
              <div v-for="(answer, index) in interviewDetail.answers" :key="index" class="answer-item">
                <div class="answer-question">
                  <span class="question-index">Q{{ answer.questionIndex + 1 }}:</span>
                  {{ answer.question }}
                </div>
                <div class="answer-content">
                  <span class="answer-label">你的回答:</span>
                  <span class="answer-text">{{ answer.userAnswer || '(未回答)' }}</span>
                </div>
                <div v-if="answer.feedback" class="answer-feedback">
                  <span class="feedback-label">AI 评价:</span>
                  <span class="feedback-text">{{ answer.feedback }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="btn primary" @click="closeInterviewCompleteModal">
          确认
        </button>
      </template>
    </Modal>

    <!-- 面试评价生成成功提示弹窗 -->
    <Modal
      :open="showEvaluationSuccessModal"
      title="面试完成"
      @close="closeEvaluationSuccessModal"
    >
      <div class="evaluation-success-content">
        <div class="success-icon">🎉</div>
        <h3 class="success-title">面试评价生成成功！</h3>
        <p class="success-message">
          您可以在面试记录里面查看详细评价
        </p>
        <p class="success-hint">
          💡 记得查看为您准备的个性化学习计划哦
        </p>
      </div>
      <template #footer>
        <button class="btn primary" @click="closeEvaluationSuccessModal">
          去查看
        </button>
      </template>
    </Modal>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Modal from '../components/ui/Modal.vue'
import Live2DAvatar from '../components/Live2DAvatar.vue'
import FullscreenButton from '../components/home/FullscreenButton.vue'  // 添加这行
import { addRecord, labelType } from '../lib/records'
import { interviewApi } from '../api/interview.js'
import { getToken } from '../lib/auth.js'

// EchoMind-37a 后端地址
// 后端已配置 CORS，直接使用完整 URL
const API_BASE = 'http://localhost:8080'

const router = useRouter()
const route = useRoute()
// 添加全屏状态
const isFullscreen = ref(false)
// 定义事件
const emit = defineEmits(['fullscreen-change'])

// 添加全屏事件处理
const onFullscreenToggle = (value) => {
  isFullscreen.value = value
  emit('fullscreen-change', value)
}



// Live2D 实例（组件 ref），用于调用 speak/unlockAudio
const avatar = ref(null)

// Live2D 加载状态
const avatarLoaded = ref(false)

// 后端连通性：仅用于"是否尝试播报"，不影响 UI 演示。
const serverOk = ref(false)

// 面试会话信息 - 完全照抄 EchoMind-feature- 的结构
const interviewSession = ref({
  sessionId: '',
  jobId: 0, // 0: 前端, 1: 后端, 2: 测试
  questions: [],
  currentQuestionIndex: 0,
  currentFollowupIndex: 0 // 0-1，每个主问题有两个追问
})

// 当前问题（完全照抄 EchoMind-feature- 的 currentQuestion 结构）
const currentQuestionObj = ref({
  questionIndex: 0,
  question: '',
  type: '',
  category: '',
  addQuestionIndex: 0
})

// 辅助函数
function rand(a,b){ return Math.round(a + Math.random()*(b-a)) }
function clamp(x){ return Math.max(45, Math.min(96, x)) }

// 获取分数颜色
function getScoreColor(score) {
  if (score >= 80) return '#22c55e'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

// 从 URL 查询参数读取配置（从简历分析页面传入）
const routeQuery = route.query

// 从 localStorage 读取面试配置（优先读取 interviewSettings，然后是 interviewConfig）
const savedSettings = JSON.parse(localStorage.getItem('interviewSettings') || '{}')
const savedConfig = JSON.parse(localStorage.getItem('interviewConfig') || '{}')

// 面试配置 - 优先使用 URL 参数，其次是 localStorage，最后是默认值
const cfg = reactive({
  type: routeQuery.type || savedSettings.jobType || savedConfig.type || 'frontend',               // frontend | backend | algo | pm
  voice: routeQuery.voice || savedConfig.voice || 'anna',
  rate: routeQuery.rate ? parseFloat(routeQuery.rate) : (savedConfig.rate || 1.0),
  showSubtitles: routeQuery.showSubtitles !== undefined 
    ? routeQuery.showSubtitles === 'true' 
    : (savedConfig.showSubtitles !== undefined ? savedConfig.showSubtitles : true),
  thinkSeconds: routeQuery.thinkSeconds ? parseInt(routeQuery.thinkSeconds) : (savedConfig.thinkSeconds || 20),
  avatarPos: routeQuery.avatarPos || savedConfig.avatarPos || 'left',         // left | right | float
  jobId: routeQuery.jobId !== undefined ? parseInt(routeQuery.jobId) : (savedSettings.jobId || savedConfig.jobId !== undefined ? savedConfig.jobId : 0),  // 0: 前端, 1: 后端, 2: 测试
  resumeText: routeQuery.resumeText || savedConfig.resumeText || '简历文本',    // 简历文本
  resumeId: routeQuery.resumeId ? parseInt(routeQuery.resumeId) : (savedSettings.resumeId || savedConfig.resumeId || 1),                // 简历ID
  questionCount: routeQuery.questionCount ? parseInt(routeQuery.questionCount) : (savedSettings.questionCount || savedConfig.questionCount || 8),       // 题目数量(4-20)
  knowledgeBaseIds: savedSettings.knowledgeBaseIds || []
})

// 倍速 range slider 的值（0.5~2.0）
const ttsRateNum = ref(1.0)
watch(ttsRateNum, (v) => {
  cfg.rate = Number(v || 1.0)
})

// 运行时状态
const subtitle = ref('')
const speaking = ref(false)

// 面试完成弹窗状态
const showInterviewCompleteModal = ref(false)
const interviewDetail = ref(null)
const interviewDetailLoading = ref(false)
const interviewDetailError = ref(null)

// 面试评价生成成功提示弹窗
const showEvaluationSuccessModal = ref(false)

// 面试题与对话
const transcript = ref([]) // { id, role:'interviewer'|'candidate', text }

// 流式消息状态
const streamingMessage = ref({
  id: null,
  text: '',
  fullText: '',
  isStreaming: false,
  category: null,
  isFollowUp: false
})

// QQ风格：左右分列的消息
const qMsgs = computed(() => transcript.value.filter(m => m.role === 'interviewer'))
const aMsgs = computed(() => transcript.value.filter(m => m.role === 'candidate'))

// 合并所有消息用于显示（包含流式消息）
const allMessages = computed(() => {
  const messages = [...transcript.value]
  // 如果有正在流式显示的消息，添加到列表末尾
  if (streamingMessage.value.isStreaming && streamingMessage.value.id) {
    messages.push({
      id: streamingMessage.value.id,
      role: 'interviewer',
      text: streamingMessage.value.text,
      category: streamingMessage.value.category,
      isFollowUp: streamingMessage.value.isFollowUp,
      isStreaming: true
    })
  }
  return messages
})

const draft = ref('')

// 当前问题显示文本
const currentQuestion = computed(() => {
  return currentQuestionObj.value.question || '请做一个自我介绍。'
})

// 计算当前主问题序号（用于显示）
const currentMainQuestionNumber = computed(() => {
  const currentQ = currentQuestionObj.value
  if (!currentQ.question) return 1
  
  const isFollowUp = currentQ.isFollowUp || (currentQ.addQuestionIndex > 0)
  const mainQuestions = interviewSession.value.questions.filter(q => !q.isFollowUp)
  
  if (isFollowUp) {
    // 追问问题，找到对应的主问题序号
    const parentIndex = currentQ.mainQuestionIndex !== undefined ? currentQ.mainQuestionIndex : currentQ.questionIndex
    const idx = mainQuestions.findIndex(q => 
      (q.questionIndex !== undefined ? q.questionIndex : interviewSession.value.questions.indexOf(q)) === parentIndex
    )
    return idx !== -1 ? idx + 1 : (parentIndex < mainQuestions.length ? parentIndex + 1 : mainQuestions.length)
  } else {
    // 主问题，计算这是第几个主问题
    const idx = mainQuestions.findIndex(q => 
      (q.questionIndex !== undefined ? q.questionIndex : interviewSession.value.questions.indexOf(q)) === currentQ.questionIndex
    )
    return idx !== -1 ? idx + 1 : 1
  }
})

// 进度百分比 - 基于 questionIndex 计算
const progressPercent = computed(() => {
  const totalQuestions = interviewSession.value.questions.length
  if (!totalQuestions || !currentQuestionObj.value.question) return 0
  
  const currentIndex = currentQuestionObj.value.questionIndex ?? 0
  return ((currentIndex + 1) / totalQuestions) * 100
})

// room 阶段：分成 3 个相位
// asking：面试官在说（播报）
// thinking：思考倒计时
// answering：回答计时
const phase = ref('asking')
const paused = ref(false)

const thinkLeft = ref(0)
const answerSec = ref(0)
let thinkTimer = null
let answerTimer = null

// 输入模式：语音/文本
const inputMode = ref('text')
const listening = ref(false)
const voiceStartedAt = ref(0)

// 语音分析结果
const lastVoiceAnalysis = ref(null)

// 设置弹窗
const openSettings = ref(false)
watch(openSettings, (v) => {
  if (v) {
    paused.value = true
    // 打开设置时停止语音采集（避免误触）
    listening.value = false
  }
})

// 提前交卷确认对话框
const showCompleteConfirm = ref(false)
const isSubmitting = ref(false)

// 退出面试确认对话框
const showExitConfirm = ref(false)
const isExiting = ref(false)

// 未完成面试相关
const showUnfinishedDialog = ref(false)
const unfinishedSession = ref(null)
const forceCreateNew = ref(false)

// 聊天滚动区域
const chatMessagesRef = ref(null)
const scrollAnchor = ref(null)
const chatScroll = ref(null)

// 自动滚动到底部
function scrollToBottom() {
  nextTick(() => {
    // 直接滚动容器到底部
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动
watch(() => transcript.value.length, () => {
  scrollToBottom()
}, { flush: 'post' })

const canAnswer = computed(() => !paused.value && phase.value==='answering')

const phaseLabel = computed(() => {
  if (paused.value) return '已暂停'
  if (phase.value === 'interviewer_thinking') return '面试官思考中'
  if (phase.value === 'asking') return '提问中'
  if (phase.value === 'thinking') return `思考中 ${thinkLeft.value}s`
  return '回答中'
})

const phaseColor = computed(() => {
  if (paused.value) return 'color:var(--muted)'
  if (phase.value === 'interviewer_thinking') return 'color:var(--warn)'
  if (phase.value === 'asking') return 'color:var(--brand)'
  if (phase.value === 'thinking') return 'color:var(--warn)'
  return 'color:var(--ok)'
})

function fmt(sec){
  const s = Math.max(0, Math.floor(sec || 0))
  const mm = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${mm}:${ss}`
}

// 使用新的API模块获取面试报告
async function getInterviewEvaluation() {
  try {
    const session = interviewSession.value
    // 使用新的API模块
    const data = await interviewApi.getReport(session.sessionId)
    
    // 适配后端InterviewReportDTO格式
    return {
      overall: data.overallScore,
      summary: data.overallFeedback,
      weakness: data.improvements || [],
      strengths: data.strengths || [],
      dimLabels: data.categoryScores?.map(d => d.category) || [],
      dimData: data.categoryScores?.map(d => d.score) || [],
      qLabels: data.questionDetails?.map((_, i) => `Q${i+1}`) || [],
      qScores: data.questionDetails?.map(q => q.score) || [],
      questionDetails: data.questionDetails || [],
      referenceAnswers: data.referenceAnswers || []
    }
  } catch (error) {
    throw error
  }
}

// 后端检测 / 音频解锁
async function checkServer(){
  // 暂时注释掉 health 检查，直接认为后端可用
  serverOk.value = true
  // try {
  //   const r = await fetch(`${API_BASE}/api/resumes/health`, { cache: 'no-store' })
  //   serverOk.value = r.ok
  // } catch {
  //   serverOk.value = false
  // }
}

function unlock(){
  // Live2DAvatar 里会创建 AudioContext，浏览器需要用户手势才能播放。
  // 所以我们提供一个"解锁音频"按钮。
  try { avatar.value?.unlockAudio?.() } catch {}
}

// 使用新的API模块创建面试会话（带重试机制）
async function createInterviewSession(maxRetries = 3) {
  let lastError = null

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      subtitle.value = attempt === 1 ? '正在创建面试会话...' : `连接失败，正在重试 (${attempt}/${maxRetries})...`

      // 使用新的API模块 - 完全照抄 EchoMind-feature-
      const data = await interviewApi.createSession({
        resumeText: cfg.resumeText,
        questionCount: cfg.questionCount,
        resumeId: cfg.resumeId,
        jobId: cfg.jobId,
        knowledgeBaseIds: cfg.knowledgeBaseIds,
        forceCreate: forceCreateNew.value
      })

      // 保存会话信息
      interviewSession.value = {
        sessionId: data.sessionId,
        jobId: data.jobId,
        questions: data.questions.map(q => ({
          text: q.question,
          category: q.category,
          type: q.type,
          questionIndex: q.questionIndex,
          followupQuestions: []
        })),
        currentQuestionIndex: data.currentQuestionIndex || 0,
        currentFollowupIndex: 0
      }

      return data
    } catch (error) {
      lastError = error
      console.error(`创建面试会话失败 (尝试 ${attempt}/${maxRetries}):`, error)

      if (attempt < maxRetries) {
        // 延迟后重试
        await new Promise(resolve => setTimeout(resolve, 2000 * attempt))
      }
    }
  }

  // 所有重试都失败了
  subtitle.value = '连接服务器失败，请检查网络后刷新页面重试'
  throw lastError
}

// 使用新的API模块提交答案并获取下一题（带重试机制）
async function submitAnswerAndGetNext(answerText, maxRetries = 3) {
  const session = interviewSession.value
  const currentQ = currentQuestionObj.value
  let lastError = null

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      if (attempt > 1) {
        subtitle.value = `提交失败，正在重试 (${attempt}/${maxRetries})...`
        await new Promise(resolve => setTimeout(resolve, 1500 * attempt))
      }

      // 使用新的API模块 - 完全照抄 EchoMind-feature-
      const response = await interviewApi.submitAnswer({
        sessionId: session.sessionId,
        questionIndex: currentQ.questionIndex,
        answer: answerText,
        addQuestionIndex: currentQ.addQuestionIndex || 0
      })

      // 处理后端返回的下一题
      if (response.hasNextQuestion && response.nextQuestion) {
        // 使用后端返回的 isFollowUp 字段判断问题类型
        const isFollowUp = response.nextQuestion.isFollowUp || false
        const normalizedNextQuestion = {
          ...response.nextQuestion,
          isFollowUp: isFollowUp,
          mainQuestionIndex: isFollowUp ? response.nextQuestion.parentQuestionIndex : undefined,
          addQuestionIndex: response.nextQuestion.addQuestionIndex ?? 0,
        }
        currentQuestionObj.value = normalizedNextQuestion
        // 返回问题和当前问题对象，用于预加载TTS
        return {
          text: response.nextQuestion.question,
          currentQ: normalizedNextQuestion
        }
      } else {
        // 面试已完成
        return null
      }
    } catch (error) {
      lastError = error
      console.error(`提交答案失败 (尝试 ${attempt}/${maxRetries}):`, error)

      // 检查是否是会话不存在错误，这种错误不需要重试
      const errorMsg = error.response?.data?.message || error.message || ''
      const isSessionNotFound = errorMsg.includes('会话不存在') ||
                                 errorMsg.includes('SESSION_NOT_FOUND') ||
                                 error.response?.status === 404

      if (isSessionNotFound) {
        // 会话已过期或被删除，提示用户
        alert('面试会话已过期，请重新开始面试')
        // 清除本地存储
        localStorage.removeItem('interviewConfig')
        // 返回null表示面试结束
        return null
      }

      if (attempt === maxRetries) {
        // 所有重试都失败了
        subtitle.value = '提交答案失败，请检查网络后重试'
        throw lastError
      }
    }
  }
}

// 检查是否有未完成的面试 - 完全照抄 EchoMind-feature-
async function checkUnfinishedSession() {
  if (!cfg.resumeId) return
  
  try {
    const foundSession = await interviewApi.findUnfinishedSession(cfg.resumeId)
    // 前端过滤：只把 IN_PROGRESS 状态的会话当成未完成
    // CREATED 状态是刚创建未开始答题，不算未完成
    if (foundSession && !forceCreateNew.value && foundSession.status === 'IN_PROGRESS') {
      unfinishedSession.value = foundSession
      showUnfinishedDialog.value = true
    }
  } catch (err) {
    console.error('检查未完成面试失败', err)
  }
}

// 继续未完成的面试 - 完全照抄 EchoMind-feature-
async function handleContinueUnfinished() {
  if (!unfinishedSession.value) return
  
  forceCreateNew.value = false
  await restoreSession(unfinishedSession.value)
  unfinishedSession.value = null
  showUnfinishedDialog.value = false
}

// 开始新面试（放弃未完成的）- 完全照抄 EchoMind-feature-
async function handleStartNew() {
  if (unfinishedSession.value) {
    // 删除未完成的面试会话
    try {
      await interviewApi.deleteSession(unfinishedSession.value.sessionId)
    } catch (err) {
      console.error('删除未完成面试失败:', err)
    }
  }
  
  unfinishedSession.value = null
  showUnfinishedDialog.value = false
  forceCreateNew.value = true
  
  // 清除localStorage中的旧会话
  localStorage.removeItem('interviewConfig')
  
  // 创建新会话
  await createNewSession()
}

// 恢复会话 - 完全照抄 EchoMind-feature-
async function restoreSession(sessionToRestore) {
  interviewSession.value = {
    sessionId: sessionToRestore.sessionId,
    jobId: sessionToRestore.jobId,
    questions: sessionToRestore.questions,
    currentQuestionIndex: sessionToRestore.currentQuestionIndex || 0,
    currentFollowupIndex: 0
  }
  
  // 恢复当前问题
  const currentQ = sessionToRestore.questions[sessionToRestore.currentQuestionIndex]
  if (currentQ) {
    currentQuestionObj.value = {
      questionIndex: currentQ.questionIndex || sessionToRestore.currentQuestionIndex,
      question: currentQ.question || '',
      type: currentQ.type || '',
      category: currentQ.category || '',
      addQuestionIndex: currentQ.addQuestionIndex || 0
    }
    
    // 恢复消息历史
    transcript.value = []
    // 先获取所有主问题，用于计算正确的mainIndex
    const mainQuestionsList = sessionToRestore.questions.filter(q => !q.isFollowUp)
    
    for (let i = 0; i <= sessionToRestore.currentQuestionIndex; i++) {
      const q = sessionToRestore.questions[i]
      if (q) {
        const isFollowUp = q.isFollowUp || (q.addQuestionIndex > 0)
        
        // 计算正确的主问题序号（与ask函数逻辑一致）
        let mainIndex
        if (isFollowUp) {
          // 追问问题，找到对应的主问题序号
          const parentIndex = q.mainQuestionIndex !== undefined ? q.mainQuestionIndex : q.questionIndex
          const idx = mainQuestionsList.findIndex(mq => 
            (mq.questionIndex !== undefined ? mq.questionIndex : sessionToRestore.questions.indexOf(mq)) === parentIndex
          )
          mainIndex = idx !== -1 ? idx + 1 : (parentIndex < mainQuestionsList.length ? parentIndex + 1 : mainQuestionsList.length)
        } else {
          // 主问题，计算这是第几个主问题
          const idx = mainQuestionsList.findIndex(mq => 
            (mq.questionIndex !== undefined ? mq.questionIndex : sessionToRestore.questions.indexOf(mq)) === q.questionIndex
          )
          mainIndex = idx !== -1 ? idx + 1 : 1
        }
        
        transcript.value.push({
          id: `${Date.now()}-${i}-q`,
          role: 'interviewer',
          text: q.question,
          category: q.category,
          isFollowUp: isFollowUp
        })
        if (q.userAnswer) {
          transcript.value.push({
            id: `${Date.now()}-${i}-a`,
            role: 'candidate',
            text: q.userAnswer
          })
        }
      }
    }
    // 恢复完成后滚动到底部
    scrollToBottom()
    
    // 保存到localStorage
    localStorage.setItem('interviewConfig', JSON.stringify({
      sessionId: sessionToRestore.sessionId,
      questions: sessionToRestore.questions,
      currentQuestionIndex: sessionToRestore.currentQuestionIndex,
      ...cfg
    }))
  }
  
  paused.value = false
  
  // 继续面试，问当前问题（使用预加载实现音频文本同步）
  phase.value = 'interviewer_thinking'
  await askWithPreload(currentQuestionObj.value.question, currentQuestionObj.value)
}

// 创建新会话
async function createNewSession() {
  transcript.value = []
  draft.value = ''
  listening.value = false
  subtitle.value = ''
  speaking.value = false
  
  const data = await createInterviewSession()
  
  // 初始化 currentQuestionObj 为第一题
  if (data.questions && data.questions.length > 0) {
    const firstQuestion = data.questions[0]
    currentQuestionObj.value = {
      questionIndex: firstQuestion.questionIndex !== undefined ? firstQuestion.questionIndex : 0,
      question: firstQuestion.question || '',
      type: firstQuestion.type || '',
      category: firstQuestion.category || '',
      isFollowUp: firstQuestion.isFollowUp || false,
      mainQuestionIndex: firstQuestion.parentQuestionIndex !== undefined ? firstQuestion.parentQuestionIndex : firstQuestion.mainQuestionIndex,
      addQuestionIndex: 0
    }
  }
  
  paused.value = false
  
  // 先问第一题（使用预加载实现音频文本同步）
  phase.value = 'interviewer_thinking'
  await askWithPreload(currentQuestion.value, currentQuestionObj.value)
}

// 验证会话是否有效
async function validateSession(sessionId) {
  try {
    // 本地会话直接返回有效
    if (sessionId.startsWith('local_')) {
      return true
    }
    const session = await interviewApi.getSession(sessionId)
    return session && (session.status === 'CREATED' || session.status === 'IN_PROGRESS')
  } catch (error) {
    console.error('验证会话失败:', error)
    return false
  }
}

// 初始化面试 - 完全照抄 EchoMind-feature-
async function initInterview() {
  // 首先检查是否有未完成的面试（无论是否有本地配置，都要检查后端）
  if (!forceCreateNew.value) {
    await checkUnfinishedSession()
    if (unfinishedSession.value) {
      // 有未完成面试，显示对话框让用户选择
      return
    }
  }
  
  // 从localStorage读取sessionId和问题列表
  const savedConfig = JSON.parse(localStorage.getItem('interviewConfig') || '{}')
  if (savedConfig.sessionId && savedConfig.questions && !forceCreateNew.value) {
    // 验证会话是否仍然有效
    const isValid = await validateSession(savedConfig.sessionId)
    if (!isValid) {
      console.warn('本地会话已过期，创建新会话')
      localStorage.removeItem('interviewConfig')
      await createNewSession()
      return
    }
    
    interviewSession.value.sessionId = savedConfig.sessionId
    interviewSession.value.questions = savedConfig.questions
    interviewSession.value.currentQuestionIndex = savedConfig.currentQuestionIndex || 0
    
    // 初始化 currentQuestionObj 为当前题
    const currentQ = savedConfig.questions[savedConfig.currentQuestionIndex || 0]
    if (currentQ) {
      currentQuestionObj.value = {
        questionIndex: currentQ.questionIndex !== undefined ? currentQ.questionIndex : (savedConfig.currentQuestionIndex || 0),
        question: currentQ.question || currentQ.text || '',
        type: currentQ.type || '',
        category: currentQ.category || '',
        isFollowUp: currentQ.isFollowUp || false,
        mainQuestionIndex: currentQ.mainQuestionIndex,
        addQuestionIndex: 0
      }
    }
    
    paused.value = false
    // 恢复面试时使用预加载
    phase.value = 'interviewer_thinking'
    await askWithPreload(currentQuestion.value, currentQuestionObj.value)
  } else {
    // 创建新会话
    await createNewSession()
  }
}

// askWithPreload():
// 1) 在"面试官思考中"状态下预加载TTS音频
// 2) 音频准备好后，流式显示问题并播放音频
// 3) 流式显示完成后进入 thinking 倒计时
// 等待 avatar 准备好
async function waitForAvatar(maxWait = 10000) {
  const startTime = Date.now()
  while (Date.now() - startTime < maxWait) {
    // 检查 avatar 是否已加载完成
    if (avatar.value && avatarLoaded.value) {
      return true
    }
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  console.warn('[waitForAvatar] 等待超时，avatarReady:', avatar.value !== null, 'avatarLoaded:', avatarLoaded.value)
  return false
}

function onAvatarLoaded() {
  console.log('[Live2D] 模型加载完成')
  avatarLoaded.value = true
}

async function askWithPreload(text, currentQ) {
  const isFollowUp = currentQ.isFollowUp || (currentQ.addQuestionIndex > 0)
  
  // 切换到提问阶段
  phase.value = 'asking'
  
  // 确保 avatar 已准备好
  const avatarReady = await waitForAvatar(3000)
  
  // 预加载TTS音频
  let ttsReady = false
  if (avatarReady && serverOk.value && avatar.value?.preloadTTS) {
    try {
      await avatar.value.preloadTTS(text, cfg.voice, cfg.rate)
      ttsReady = true
    } catch (e) {
      console.error('[TTS] 预加载失败:', e)
    }
  }

  // 播放预加载的音频（不等待，与流式显示并行）
  if (ttsReady && avatar.value?.playPrepared) {
    avatar.value.playPrepared().catch(e => {
      console.error('[TTS] 播放失败:', e)
    })
  } else if (avatarReady && serverOk.value && avatar.value?.speak) {
    // 兼容旧接口
    avatar.value.speak(text, cfg.voice, cfg.rate).catch(e => {
      console.error('[TTS] 播报失败:', e)
    })
  }

  // 流式显示问题并等待完成
  await streamText(text, currentQ.category, isFollowUp)

  // 流式显示完成后立即进入思考阶段
  startThinking()
}

// 流式显示文本的函数
async function streamText(text, category, isFollowUp) {
  const messageId = `${Date.now()}-${Math.random()}`
  
  // 初始化流式消息
  streamingMessage.value = {
    id: messageId,
    text: '',
    fullText: text,
    isStreaming: true,
    category: category,
    isFollowUp: isFollowUp
  }
  
  scrollToBottom()
  
  // 逐字显示
  const chars = text.split('')
  const delay = 50 // 每个字之间的延迟（毫秒）
  
  for (let i = 0; i < chars.length; i++) {
    if (!streamingMessage.value.isStreaming) {
      // 如果被中断，直接显示完整文本
      streamingMessage.value.text = text
      break
    }
    
    streamingMessage.value.text += chars[i]
    scrollToBottom()
    
    // 等待一段时间再显示下一个字
    await new Promise(resolve => setTimeout(resolve, delay))
  }
  
  // 流式显示完成，将消息添加到 transcript
  streamingMessage.value.isStreaming = false
  transcript.value.push({
    id: messageId,
    role: 'interviewer',
    text: text,
    category: category,
    isFollowUp: isFollowUp
  })
  
  // 清空流式消息
  streamingMessage.value = {
    id: null,
    text: '',
    fullText: '',
    isStreaming: false,
    category: null,
    isFollowUp: false
  }
}

// ask():
// 1) 把面试官问题推进 transcript（流式显示）
// 2) 尝试调用 Live2D 播报（如果后端已连接）- 与流式显示并行，不阻塞
// 3) 流式显示完成后进入 thinking 倒计时
async function ask(text){
  phase.value = 'asking'
  const currentQ = currentQuestionObj.value
  const isFollowUp = currentQ.isFollowUp || (currentQ.addQuestionIndex > 0)
  
  // 启动语音播报（不等待，与流式显示并行）
  if (serverOk.value && avatar.value?.speak) {
    // 使用 Promise 但不 await，让语音和流式显示同时进行
    avatar.value.speak(text, cfg.voice, cfg.rate).catch(e => {
      console.error('[TTS] 播报失败:', e)
    })
  }
  
  // 开始流式显示并等待完成
  await streamText(text, currentQ.category, isFollowUp)

  // 流式显示完成后立即进入思考阶段（不等待语音播报完成）
  startThinking()
}

function startThinking(){
  stopTimers()
  phase.value = 'thinking'
  thinkLeft.value = cfg.thinkSeconds
  if (paused.value) return
  thinkTimer = setInterval(() => {
    if (paused.value) return
    thinkLeft.value -= 1
    if (thinkLeft.value <= 0) {
      clearInterval(thinkTimer)
      thinkTimer = null
      startAnswer()
    }
  }, 1000)
}

function startAnswer(){
  stopTimers()
  phase.value = 'answering'
  answerSec.value = 0
  if (paused.value) return
  answerTimer = setInterval(() => {
    if (paused.value) return
    answerSec.value += 1

    // 超过建议时长(90秒)就提示（不强制结束，避免打断演示）
    if (answerSec.value === 90) {
      // 这里不弹窗，避免打扰；你可以改成 toast。
      subtitle.value = '提示：已达到建议回答时长，可以收尾给结论。'
    }
  }, 1000)
}

function stopTimers(){
  if (thinkTimer) { clearInterval(thinkTimer); thinkTimer = null }
  if (answerTimer) { clearInterval(answerTimer); answerTimer = null }
}

function togglePause(){
  paused.value = !paused.value
  // 暂停时：不销毁计时器，只让计时器 tick 时 "return"，保持 UI 稳定。
}

function applySettings(){
  openSettings.value = false
  paused.value = false
  
  // 如果当前正在思考阶段，立即应用新的思考时间
  if (phase.value === 'thinking' && thinkLeft.value > cfg.thinkSeconds) {
    thinkLeft.value = cfg.thinkSeconds
  }
}

async function testSpeak(){
  // 设置页里"试播一句"：用于验证 Live2D + TTS 链路是否工作。
  const text = '你好，我是今天的面试官。我们开始吧。'
  if (serverOk.value && avatar.value?.speak) {
    try {
      await avatar.value.speak(text, cfg.voice, cfg.rate)
    } catch (e) {
      console.error('[TTS] 试播失败:', e)
    }
  } else {
    console.warn('[TTS] 无法试播: serverOk=', serverOk.value, ', avatar.speak=', !!avatar.value?.speak)
  }
}

// 语音识别：调用ASR接口
// 后端返回 AsrAnalysisResult 对象，包含 transcription 等字段
async function transcribeAudio(audioBlob) {
  try {
    const formData = new FormData()
    formData.append('file', audioBlob, 'recording.wav')

    const response = await fetch(`${API_BASE}/api/interview/sessions/asr`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getToken() || ''}`  // 使用 JWT Token
      },
      body: formData
    })

    if (!response.ok) {
      throw new Error(`语音识别失败: ${response.status}`)
    }

    // 后端直接返回 AsrAnalysisResult 对象
    const result = await response.json()

    // 检查是否包含 transcription 字段（后端返回格式）
    if (result && result.transcription) {
      // 保存完整的语音分析结果供展示
      // 注意：后端返回 clarityScore（清晰度），不是 tensionScore（紧张度）
      lastVoiceAnalysis.value = {
        confidenceScore: result.confidenceScore,
        clarityScore: result.clarityScore,  // 修复：使用后端实际返回的字段
        speechSpeed: result.speechSpeed,
        analysisReason: result.analysisReason
      }
      return result.transcription
    }

    // 兼容旧格式（如果后端返回 { success, result, msg }）
    if (result.success && result.result) {
      return result.result
    }

    throw new Error('语音识别返回格式错误')
  } catch (error) {
    console.error('语音识别失败:', error)
    subtitle.value = '语音识别失败，请重试或使用文字输入'
    // 降级到显示时长
    return null
  }
}

// 录音相关变量
let mediaRecorder = null
let audioChunks = []

function toggleVoice(){
  if (!canAnswer.value) return
  if (!listening.value) {
    // 开始录音
    startRecording()
    return
  }
  // 结束录音
  stopRecording()
}

function startRecording() {
  navigator.mediaDevices.getUserMedia({ audio: true })
    .then(stream => {
      mediaRecorder = new MediaRecorder(stream)
      audioChunks = []
      
      mediaRecorder.ondataavailable = (event) => {
        audioChunks.push(event.data)
      }
      
      mediaRecorder.start()
      listening.value = true
      voiceStartedAt.value = Date.now()
    })
    .catch(error => {
      console.error('获取麦克风失败:', error)
      subtitle.value = '无法访问麦克风，请检查权限'
    })
}

async function stopRecording() {
  if (!mediaRecorder) return
  
  return new Promise((resolve) => {
    mediaRecorder.onstop = async () => {
      listening.value = false
      
      // 创建音频Blob
      const audioBlob = new Blob(audioChunks, { type: 'audio/wav' })
      
      // 调用STT接口
      const recognizedText = await transcribeAudio(audioBlob)
      
      if (recognizedText) {
        // 识别成功，使用识别文本，并传递ASR分析结果
        pushAnswerAndNext(recognizedText, lastVoiceAnalysis.value)
      } else {
        // 识别失败，使用时长占位
        const dur = Math.max(1, Math.round((Date.now() - voiceStartedAt.value) / 1000))
        pushAnswerAndNext(`已回答完毕，时长 ${dur}s`, null)
      }
      
      // 清空本次的ASR分析结果
      lastVoiceAnalysis.value = null
      
      resolve()
    }
    
    mediaRecorder.stop()
  })
}

function commitText(){
  const t = String(draft.value || '').trim() || '（未填写回答）'
  draft.value = ''
  handleSubmitAnswer(t)
}

// 统一的答案提交处理 - 完全照抄 EchoMind-feature-
async function handleSubmitAnswer(answerText, voiceAnalysis = null) {
  // 添加用户消息到聊天记录（如果是语音输入，附带ASR分析结果）
  transcript.value.push({ 
    id: `${Date.now()}-${Math.random()}`, 
    role: 'candidate', 
    text: answerText,
    voiceAnalysis: voiceAnalysis
  })
  scrollToBottom()
  
  // 停止计时器
  stopTimers()
  
  // 显示"面试官思考中"
  phase.value = 'interviewer_thinking'
  
  // 使用新的API模块提交答案并获取下一题
  const nextQuestionResult = await submitAnswerAndGetNext(answerText)
  
  if (nextQuestionResult) {
    // 还有下一题，预加载TTS后同时显示问题和播放
    await askWithPreload(nextQuestionResult.text, nextQuestionResult.currentQ)
  } else {
    // 面试已完成（正常结束，所有题目已回答完）
    // 与 EchoMind-feature- 一致：正常结束不调用 completeInterview，直接跳转
    await onInterviewComplete()
  }
}

// 保留旧函数名以兼容现有代码，但实际调用新的处理逻辑
async function pushAnswerAndNext(answerText, voiceAnalysis = null) {
  await handleSubmitAnswer(answerText, voiceAnalysis)
}

// 暂时退出面试 - 保存当前答案并退出
async function handleTempExit() {
  if (!interviewSession.value.sessionId) return
  
  isExiting.value = true
  try {
    // 1. 如果有未提交的答案，先暂存
    const currentQ = currentQuestionObj.value
    if (currentQ && currentQ.question) {
      const currentAnswer = transcript.value
        .filter(m => m.role === 'candidate')
        .pop()?.text
      
      if (currentAnswer && currentAnswer.trim()) {
        try {
          await interviewApi.saveAnswer({
            sessionId: interviewSession.value.sessionId,
            questionIndex: currentQ.questionIndex,
            answer: currentAnswer,
            addQuestionIndex: currentQ.addQuestionIndex || 0
          })
          console.log('暂存答案成功')
        } catch (err) {
          console.error('暂存答案失败:', err)
          // 继续退出流程，不阻塞用户
        }
      }
    }
    
    // 2. 清除 localStorage 中的会话数据
    localStorage.removeItem('interviewConfig')
    
    // 3. 关闭弹窗并返回首页
    showExitConfirm.value = false
    router.push('/app/home')
    
  } catch (err) {
    console.error('暂时退出失败:', err)
    subtitle.value = '退出失败，请重试'
  } finally {
    isExiting.value = false
  }
}

// 提前交卷 - 完全照抄 EchoMind-feature- 前端实现
// 调用 completeInterview 后跳转到面试记录页，评估在后台异步进行
async function finishInterview() {
  if (!interviewSession.value.sessionId) return

  isSubmitting.value = true
  try {
    showCompleteConfirm.value = false
    showExitConfirm.value = false
    
    // 面试已完成，评估将在后台进行，跳转到面试记录页
    // 注意：onInterviewComplete 内部会调用 completeInterview，这里不需要重复调用
    await onInterviewComplete()
  } catch (err) {
    subtitle.value = '提前交卷失败，请重试'
    console.error('[finishInterview] 提前交卷失败:', err)
  } finally {
    isSubmitting.value = false
  }
}

// 面试完成回调 - 关闭弹窗并返回主页面
function closeInterviewCompleteModal() {
  showInterviewCompleteModal.value = false
  interviewDetail.value = null
  interviewDetailError.value = null
  
  // 返回主页面
  router.push('/')
}

// 关闭面试评价成功提示弹窗 - 跳转到学习计划页面
function closeEvaluationSuccessModal() {
  showEvaluationSuccessModal.value = false
  // 跳转到学习计划页面
  router.push('/app/home/study-plan')
}

// 面试完成回调 - 跳转到报告加载页面
async function onInterviewComplete() {
  // 清除当前面试会话数据
  localStorage.removeItem('interviewConfig')

  // 保存当前会话ID和聊天记录，供报告页面使用
  localStorage.setItem('interviewTranscript', JSON.stringify(transcript.value))

  // 检查是否为本地会话
  const isLocalSession = interviewSession.value.sessionId.startsWith('local_')
  if (isLocalSession) {
    // 本地会话直接生成模拟数据并显示成功弹窗
    const mockReport = {
      overallScore: 85,
      overallFeedback: '整体表现良好，技术基础扎实，沟通表达清晰。',
      strengths: [
        '技术知识掌握全面',
        '问题分析能力强',
        '沟通表达清晰'
      ],
      improvements: [
        '需要加强项目经验的描述',
        '可以更详细地解释技术实现细节'
      ],
      answers: transcript.value.filter(msg => msg.role === 'interviewer').map((q, index) => {
        const answer = transcript.value.find(a => a.role === 'candidate' && a.id > q.id)
        return {
          questionIndex: index,
          question: q.text,
          userAnswer: answer ? answer.text : '(未回答)',
          feedback: '回答合理，思路清晰'
        }
      })
    }
    localStorage.setItem('interviewReport', JSON.stringify(mockReport))
    // 显示成功弹窗
    showEvaluationSuccessModal.value = true
    return
  }

  // 真实会话：先调用完成接口，然后跳转到报告加载页面
  try {
    await interviewApi.completeInterview(interviewSession.value.sessionId)
  } catch (err) {
    console.error('完成面试接口调用失败:', err)
  }

  // 跳转到报告加载页面，等待评估完成
  router.replace(`/app/home/interview/report-loading?sessionId=${interviewSession.value.sessionId}`)
}

// 初始化
onMounted(async () => {
  // 添加页面关闭确认和浏览器返回按钮监听
  window.addEventListener('beforeunload', handleBeforeUnload)
  // 添加历史记录状态，用于拦截返回按钮
  history.pushState(null, '', location.href)
  window.addEventListener('popstate', handlePopState)
  
  // 检测后端连接
  await checkServer()

  // 从localStorage读取已准备好的面试配置
  const savedConfig = JSON.parse(localStorage.getItem('interviewConfig') || '{}')
  if (savedConfig.sessionId && savedConfig.questions) {
    // 先检查是否有未完成的面试（优先检查后端的未完成会话）
    if (!forceCreateNew.value && cfg.resumeId) {
      await checkUnfinishedSession()
      if (unfinishedSession.value) {
        // 有未完成面试，显示对话框让用户选择，不自动开始
        return
      }
    }
    
    // 恢复会话状态
    interviewSession.value.sessionId = savedConfig.sessionId
    interviewSession.value.questions = savedConfig.questions
    interviewSession.value.currentQuestionIndex = savedConfig.currentQuestionIndex || 0

    // 初始化当前题目
    const currentQ = savedConfig.questions[savedConfig.currentQuestionIndex || 0]
    if (currentQ) {
      currentQuestionObj.value = {
        questionIndex: currentQ.questionIndex || savedConfig.currentQuestionIndex || 0,
        question: currentQ.question || currentQ.text || '',
        type: currentQ.type || '',
        category: currentQ.category || '',
        addQuestionIndex: 0
      }
    }

    // 开始面试，显示第一个问题（使用预加载实现音频文本同步）
    paused.value = false
    // 先显示"面试官思考中"，然后预加载TTS
    phase.value = 'interviewer_thinking'
    await askWithPreload(currentQuestion.value, currentQuestionObj.value)
  } else {
    // 没有本地配置，检查是否有未完成的面试
    if (!forceCreateNew.value && cfg.resumeId) {
      await checkUnfinishedSession()
      if (unfinishedSession.value) {
        // 有未完成面试，显示对话框让用户选择
        return
      }
    }
    // 没有准备好的配置，返回设置页面
    router.replace('/app/home/interview/settings')
  }

  // 添加按钮鼠标跟随效果
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

// 页面关闭/刷新前的确认提示
const handleBeforeUnload = (e) => {
  // 如果面试还在进行中，提示用户
  if (interviewSession.value.sessionId && 
      currentQuestionObj.value.question && 
      !showEvaluationSuccessModal.value) {
    e.preventDefault()
    e.returnValue = '面试还在进行中，确定要离开吗？您可以选择暂时退出保存进度。'
    return e.returnValue
  }
}

// 处理浏览器返回按钮 - 显示退出确认弹窗
const handlePopState = (e) => {
  // 如果面试还在进行中，阻止返回并显示确认弹窗
  if (interviewSession.value.sessionId && 
      currentQuestionObj.value.question && 
      !showEvaluationSuccessModal.value &&
      !showExitConfirm.value &&
      !showCompleteConfirm.value) {
    // 阻止默认返回行为
    history.pushState(null, '', location.href)
    // 显示退出确认弹窗
    showExitConfirm.value = true
  }
}

// 清理
onBeforeUnmount(() => {
  stopTimers()
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
  }
  // 移除事件监听器
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('popstate', handlePopState)
})
</script>

<style scoped>
.interview-room {
  min-height: 100%;
  background: var(--bg0);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-state {
  text-align: center;
  padding: 40px;
  color: #ef4444;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 10px;
}

.detail-content::-webkit-scrollbar {
  width: 6px;
}

.detail-content::-webkit-scrollbar-track {
  background: var(--panel2);
  border-radius: 3px;
}

.detail-content::-webkit-scrollbar-thumb {
  background: var(--stroke);
  border-radius: 3px;
}

.detail-content::-webkit-scrollbar-thumb:hover {
  background: var(--muted);
}

.score-section {
  text-align: center;
  padding: 20px;
  background: var(--panel);
  border-radius: 8px;
  border: 1px solid var(--stroke);
}

.score-section h4 {
  margin-bottom: 16px;
  color: var(--text);
  font-size: 16px;
  font-weight: 600;
}

.score-circle {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 16px;
}

.score-ring {
  transform: rotate(-90deg);
}

.score-progress {
  transition: stroke-dashoffset 0.8s ease;
}

.score-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.score-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--text);
}

.score-total {
  font-size: 14px;
  color: var(--muted);
}

.score-feedback {
  color: var(--text);
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}

.summary-section {
  margin-bottom: 20px;
}

.summary-card {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--panel);
  border-radius: 8px;
  border: 1px solid var(--stroke);
}

.summary-card h5 {
  margin-bottom: 8px;
  color: var(--text);
  font-size: 14px;
  font-weight: 600;
}

.summary-card ul {
  margin: 0;
  padding-left: 20px;
  color: var(--text);
  font-size: 13px;
  line-height: 1.4;
}

.summary-card li {
  margin-bottom: 4px;
}

.questions-summary {
  padding: 16px;
  background: var(--panel);
  border-radius: 8px;
  border: 1px solid var(--stroke);
}

.questions-summary h5 {
  margin-bottom: 8px;
  color: var(--text);
  font-size: 14px;
  font-weight: 600;
}

.questions-summary p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.answers-list {
  margin-top: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.answer-item {
  background: var(--panel2);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid var(--stroke);
}

.answer-question {
  font-weight: 500;
  margin-bottom: 6px;
  color: var(--text);
  font-size: 13px;
}

.question-index {
  color: var(--primary);
  font-weight: 600;
  margin-right: 6px;
}

.answer-content {
  margin-bottom: 6px;
  font-size: 13px;
}

.answer-label {
  color: var(--muted);
  margin-right: 6px;
  font-weight: 500;
}

.answer-text {
  color: var(--text);
  line-height: 1.4;
}

.answer-feedback {
  font-size: 12px;
  background: var(--panel);
  padding: 6px;
  border-radius: 4px;
  margin-top: 6px;
}

.feedback-label {
  color: var(--muted);
  margin-right: 6px;
  font-weight: 500;
}

.feedback-text {
  color: var(--text);
  line-height: 1.4;
}

/* 面试评价生成成功弹窗样式 */
.evaluation-success-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px;
}

.success-icon {
  font-size: 64px;
  margin-bottom: 16px;
  animation: bounce 1s ease infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.success-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 12px 0;
}

.success-message {
  font-size: 15px;
  color: var(--text);
  margin: 0 0 8px 0;
  line-height: 1.5;
}

.success-hint {
  font-size: 14px;
  color: var(--brand);
  margin: 0;
  padding: 10px 16px;
  background: color-mix(in srgb, var(--brand) 10%, transparent);
  border-radius: 8px;
  line-height: 1.5;
}

/* 背景光晕效果 */
.interview-room::before {
  content: '';
  position: fixed;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background:
    radial-gradient(ellipse at 20% 20%, rgba(100, 108, 255, 0.08) 0%, transparent 40%),
    radial-gradient(ellipse at 80% 80%, rgba(54, 211, 153, 0.05) 0%, transparent 40%),
    radial-gradient(ellipse at 50% 50%, rgba(255, 90, 90, 0.03) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}


.icon-btn {
  width: 40px;
  height: 40px;
  border: 1px solid var(--stroke);
  border-radius: 10px;
  background: var(--panel);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  color: var(--text);
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--panel2);
  border-color: var(--stroke2);
}

/* 问题进度面板 - 完全按照 EchoMind-feature- 样式 */
.question-progress-panel {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 320px;
  padding: 16px 20px;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 16px;
  z-index: 100;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}


.progress-percent {
  font-size: 14px;
  color: var(--muted);
}

.progress-track {
  width: 100%;
  height: 8px;
  background: var(--bg1);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

/* 问题分类标签 - 完全按照 EchoMind-feature- 样式 */
.question-category-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: var(--panel2);
  color: var(--brand);
  font-size: 12px;
  font-weight: 500;
  border-radius: 20px;
  margin-left: 8px;
}

/* 中间Live2D区域 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 60px;
  padding-bottom: 20px;
  position: relative;
  z-index: 1;
  max-height: 40vh;
  flex-shrink: 1;
}
/* 新增：按钮组样式 */
.avatar-header {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
}

.button-group {
  display: flex;
  gap: 8px;
}

/* 修改图标按钮样式 */
.icon-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--stroke);
  border-radius: 10px;
  background: var(--panel);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  color: var(--text);
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--panel2);
  border-color: var(--stroke2);
}

/* 确保全屏按钮适配 */
.avatar-header :deep(.fullscreen-btn) {
  position: static;
  width: 36px;
  height: 36px;
}

.live2d-stage {
  width: 200px;
  max-height: 260px;
  height: 30vh;
  min-height: 200px;
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(100, 108, 255, 0.1) 0%, rgba(54, 211, 153, 0.05) 100%);
  border: 1px solid var(--stroke);
}

.phase-indicator {
  margin-top: 16px;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 16px;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 20px;
}

/* 聊天区域 - QQ/微信风格：单列表 */
.chat-area {
  flex: 1;
  padding: 0 20px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg1);
  border-radius: 16px 16px 0 0;
  margin: 0 20px;
  min-height: 120px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  scroll-behavior: smooth;
}

.messages-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: auto;
}

/* 滚动锚点 */
.scroll-anchor {
  height: 1px;
  flex-shrink: 0;
}

/* 隐藏滚动条但保留功能 */
.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: var(--stroke);
  border-radius: 2px;
}

/* 消息行 - QQ/微信风格 */
.message-row {
  display: flex;
  width: 100%;
  animation: messageSlideIn 0.3s ease-out;
}

.message-row.interviewer {
  justify-content: flex-start;
}

.message-row.candidate {
  justify-content: flex-end;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 消息气泡 */
.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  box-shadow: var(--shadow);
  position: relative;
}

/* 面试官消息 - 白色气泡，左对齐 */
.message-bubble.interviewer {
  background: var(--panel);
  color: var(--text);
  border: 1px solid var(--stroke);
  border-top-left-radius: 4px;
}

/* 用户消息 - 绿色气泡，右对齐 */
.message-bubble.candidate {
  background: var(--ok);
  color: white;
  border: none;
  border-top-right-radius: 4px;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.message-sender {
  font-size: 12px;
  color: var(--muted);
  font-weight: 500;
}

.message-bubble.candidate .message-header {
  justify-content: flex-end;
}

.message-bubble.candidate .message-sender {
  color: rgba(255, 255, 255, 0.8);
}

/* 问题类型标签 */
.question-type-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
}

.question-type-tag.main {
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  color: white;
}

.question-type-tag.follow-up {
  background: color-mix(in srgb, var(--warning, #f59e0b) 15%, transparent);
  color: var(--warning, #f59e0b);
  border: 1px solid color-mix(in srgb, var(--warning, #f59e0b) 30%, transparent);
}

.message-text {
  white-space: pre-wrap;
}

/* 流式消息样式 */
.message-text.streaming {
  position: relative;
}

.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 1.2em;
  background: var(--brand, #646cff);
  margin-left: 2px;
  vertical-align: middle;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

/* 语音ASR分数显示 */
.voice-scores {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  flex-wrap: wrap;
}

.score-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.score-tag.confidence {
  background: rgba(255, 193, 7, 0.3);
}

.score-tag.clarity {
  background: rgba(33, 150, 243, 0.3);
}

.score-icon {
  font-size: 12px;
}

/* 占位符样式 */
.chat-placeholder {
  text-align: center;
  color: var(--muted);
  font-size: 14px;
  padding: 60px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.placeholder-icon {
  font-size: 48px;
  opacity: 0.5;
}

/* 兼容旧代码的消息行样式 */
.message-row {
  display: flex;
  width: 100%;
}

.message-row.left {
  justify-content: flex-start;
}

.message-row.right {
  justify-content: flex-end;
}

/* 底部输入区域 */
.input-section {
  padding: 12px 20px 24px;
  z-index: 1;
  margin-top: auto;
  min-height: 80px;
  width: 100%;
  box-sizing: border-box;
  background: var(--bg0);
  border-top: 1px solid var(--stroke);
  flex-shrink: 0;
}

.text-input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 24px;
  padding: 6px 6px 6px 12px;
}

.input-icon {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--muted);
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.input-icon:hover {
  color: var(--text);
}

.input-field {
  flex: 1;
  border: none;
  background: transparent;
  color: var(--text);
  font-size: 15px;
  resize: none;
  outline: none;
  padding: 8px 0;
  min-height: 20px;
  max-height: 100px;
}

.input-field::placeholder {
  color: var(--muted2);
}

.send-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: var(--brand);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 15px rgba(100, 108, 255, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 语音输入 */
.voice-input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
}

.voice-btn {
  flex: 1;
  height: 48px;
  border: 1px solid var(--stroke);
  border-radius: 24px;
  background: var(--panel);
  color: var(--text);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.voice-btn:hover:not(:disabled) {
  background: var(--panel2);
}

.voice-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.voice-btn.recording {
  background: rgba(255, 90, 90, 0.2);
  border-color: rgba(255, 90, 90, 0.5);
  color: var(--bad);
}

.recording-wave {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: var(--bad);
  border-radius: 50%;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

/* 设置弹窗样式 */
.row {
  display: flex;
  align-items: center;
}

.row.space {
  justify-content: space-between;
}

.row.gap10 {
  gap: 10px;
}

.row.wrap {
  flex-wrap: wrap;
}

.seg {
  display: inline-flex;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 999px;
  padding: 3px;
}

.seg-btn {
  padding: 6px 14px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.seg-btn.on {
  background: var(--brand);
  color: white;
}

.select {
  padding: 8px 12px;
  border-radius: 10px;
  border: 1px solid var(--stroke);
  background: var(--panel);
  color: var(--text);
  font-size: 14px;
  outline: none;
}

.btn {
  padding: 8px 16px;
  border-radius: 10px;
  border: 1px solid var(--stroke);
  background: var(--panel);
  color: var(--text);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:hover {
  background: var(--panel2);
}

.btn.primary {
  background: var(--brand);
  border-color: var(--brand);
  color: white;
}

.btn.danger {
  background: rgba(255, 90, 90, 0.2);
  border-color: rgba(255, 90, 90, 0.3);
  color: var(--bad);
}

.btn.ghost {
  background: transparent;
}

.muted2 {
  color: var(--muted2);
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: var(--stroke);
  border-radius: 2px;
}

/* 响应式 */
@media (max-width: 600px) {
  .live2d-stage {
    width: 160px;
    height: 200px;
  }
  
  .message-bubble {
    max-width: 85%;
    padding: 10px 14px;
    font-size: 13px;
  }
}

</style>
