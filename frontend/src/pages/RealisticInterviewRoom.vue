<template>
  <div class="realistic-interview-room page-with-bg" :class="{ 'fullscreen-mode': isFullscreen }">
    <!-- 中间：Live2D面试官 - 占据主要区域 -->
    <div class="avatar-section">
      <!-- 按钮组 -->
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
        <Live2DAvatar ref="avatar" @subtitle="onSubtitle" @speaking="speaking=$event" @loaded="onAvatarLoaded" />
      </div>
      
      <!-- 当前状态提示 -->
      <div class="phase-indicator" :style="phaseColor">
        {{ phaseLabel }}
      </div>
      
      <!-- 问题进度 -->
      <div v-if="interviewSession.questions.length > 0 && currentQuestionObj.question" class="question-progress">
        <span class="progress-text">{{ currentQuestionObj.questionIndex + 1 }} / {{ interviewSession.questions.length }}</span>
        <div class="progress-bar-mini">
          <div class="progress-fill-mini" :style="{ width: progressPercent + '%' }"></div>
        </div>
      </div>
    </div>

    <!-- 语音分析结果展示 -->
    <transition name="fade">
      <div v-if="showVoiceAnalysis && voiceAnalysis" class="voice-analysis-panel">
        <div class="voice-scores">
          <span class="score-tag confidence">
            <span class="score-icon">🎯</span>
            自信度: {{ voiceAnalysis.confidence }}/10
          </span>
          <span class="score-tag clarity">
            <span class="score-icon">🎤</span>
            清晰度: {{ voiceAnalysis.clarity }}/10
          </span>
          <span class="score-tag duration">
            <span class="score-icon">⏱️</span>
            时长: {{ voiceAnalysis.duration }}s
          </span>
        </div>
      </div>
    </transition>

    <!-- 底部：语音输入控制 -->
    <div class="voice-control-section">
      <div class="voice-status" v-if="phase === 'answering' && !paused">
        <div class="voice-wave" :class="{ active: listening }">
          <span v-for="i in 5" :key="i" :style="{ animationDelay: (i * 0.1) + 's' }"></span>
        </div>
        <span class="voice-hint">{{ listening ? '正在聆听...' : '点击麦克风开始回答' }}</span>
      </div>
      
      <button 
        v-if="phase === 'answering' && !paused"
        class="voice-btn"
        :class="{ active: listening }"
        @click="toggleVoice"
        :disabled="!canAnswer"
      >
        <span class="voice-icon">{{ listening ? '🔴' : '🎙️' }}</span>
      </button>
      
      <div v-else-if="phase === 'thinking' && !paused" class="waiting-hint">
        <span class="thinking-dots">思考中<span>.</span><span>.</span><span>.</span></span>
      </div>
      
      <div v-else-if="phase === 'asking' && !paused" class="waiting-hint">
        <span>面试官正在提问</span>
      </div>
      
      <div v-else-if="paused" class="waiting-hint">
        <span>已暂停</span>
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

          <div class="row gap10 wrap" style="margin-top:12px; justify-content: flex-end;">
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Modal from '../components/ui/Modal.vue'
import Live2DAvatar from '../components/Live2DAvatar.vue'
import FullscreenButton from '../components/home/FullscreenButton.vue'
import { interviewApi } from '../api/interview.js'
import { getToken } from '../lib/auth.js'

const API_BASE = 'http://localhost:8080'

const router = useRouter()
const route = useRoute()

// ============ 状态定义（与 InterviewRoom.vue 保持一致） ============
const isFullscreen = ref(false)
const emit = defineEmits(['fullscreen-change'])

const onFullscreenToggle = (value) => {
  isFullscreen.value = value
  emit('fullscreen-change', value)
}

// Live2D 实例
const avatar = ref(null)
const serverOk = ref(false)
const speaking = ref(false)

// 面试会话信息
const interviewSession = ref({
  sessionId: '',
  jobId: 0,
  questions: [],
  currentQuestionIndex: 0,
  currentFollowupIndex: 0
})

// 当前问题
const currentQuestionObj = ref({
  questionIndex: 0,
  question: '',
  type: '',
  category: '',
  addQuestionIndex: 0
})

// 从 localStorage 读取配置
const savedSettings = JSON.parse(localStorage.getItem('interviewSettings') || '{}')
const savedConfig = JSON.parse(localStorage.getItem('interviewConfig') || '{}')

// 面试配置
const cfg = reactive({
  type: savedSettings.jobType || savedConfig.type || 'frontend',
  voice: savedConfig.voice || 'anna',
  rate: savedConfig.rate || 1.0,
  thinkSeconds: savedConfig.thinkSeconds || 20,
  jobId: savedSettings.jobId || savedConfig.jobId || 0,
  resumeText: savedConfig.resumeText || '简历文本',
  resumeId: savedSettings.resumeId || savedConfig.resumeId || 1,
  questionCount: savedSettings.questionCount || savedConfig.questionCount || 5,
  knowledgeBaseIds: savedSettings.knowledgeBaseIds || []
})

// 倍速
const ttsRateNum = ref(1.0)
watch(ttsRateNum, (v) => {
  cfg.rate = Number(v || 1.0)
})

// 运行时状态
const phase = ref('asking')
const paused = ref(false)
const listening = ref(false)
const thinkLeft = ref(0)
let thinkingTimer = null

// 输入方式
const inputMode = ref('voice') // 'voice' 或 'text'

// 弹窗状态
const openSettings = ref(false)
const showCompleteConfirm = ref(false)
const showExitConfirm = ref(false)
const isSubmitting = ref(false)
const isExiting = ref(false)
const showUnfinishedDialog = ref(false)
const unfinishedSession = ref(null)
const showInterviewCompleteModal = ref(false)
const interviewDetail = ref(null)
const interviewDetailLoading = ref(false)
const interviewDetailError = ref(null)
const showEvaluationSuccessModal = ref(false)

// 强制创建新会话标志
const forceCreateNew = ref(false)

// 语音录制
let mediaRecorder = null
let audioChunks = []
let recordingStartTime = null

// 语音分析结果
const voiceAnalysis = ref(null)
const showVoiceAnalysis = ref(false)

// Live2D 加载状态
const avatarLoaded = ref(false)

// 语音识别结果
const lastVoiceAnalysis = ref(null)

// ============ 计算属性 ============
const currentQuestion = computed(() => {
  return currentQuestionObj.value.question || '请做一个自我介绍。'
})

const progressPercent = computed(() => {
  const totalQuestions = interviewSession.value.questions.length
  if (!totalQuestions || !currentQuestionObj.value.question) return 0
  const currentIndex = currentQuestionObj.value.questionIndex ?? 0
  return ((currentIndex + 1) / totalQuestions) * 100
})

const canAnswer = computed(() => phase.value === 'answering' && !paused.value)

const phaseLabel = computed(() => {
  if (paused.value) return '已暂停'
  if (phase.value === 'thinking') return `思考中 ${thinkLeft.value}s`
  const labels = {
    asking: '面试官提问中',
    answering: '回答中'
  }
  return labels[phase.value] || '准备开始'
})

const phaseColor = computed(() => {
  if (paused.value) return 'background: var(--muted)'
  const colors = {
    asking: 'background: linear-gradient(135deg, var(--brand), var(--brand-2))',
    thinking: 'background: linear-gradient(135deg, #f59e0b, #d97706)',
    answering: 'background: linear-gradient(135deg, var(--ok), #22c55e)'
  }
  return colors[phase.value] || 'background: var(--muted)'
})

// ============ 方法 ============
function getScoreColor(score) {
  if (score >= 80) return '#22c55e'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

function onSubtitle(text) {
  // 真实模拟房间不显示字幕
  console.log('[字幕]', text)
}

function onAvatarLoaded() {
  console.log('[Live2D] 模型加载完成')
  avatarLoaded.value = true
}

function unlock() {
  try { avatar.value?.unlockAudio?.() } catch {}
}

function testSpeak() {
  if (avatar.value?.speak) {
    avatar.value.speak('这是语音测试', cfg.voice, cfg.rate)
  }
}

function applySettings() {
  openSettings.value = false
  paused.value = false
}

watch(openSettings, (v) => {
  if (v) paused.value = true
})

// 检查后端服务
async function checkServer() {
  serverOk.value = true
}

// 检查未完成会话
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

// 恢复会话
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
  }
  
  // 更新 localStorage
  localStorage.setItem('interviewConfig', JSON.stringify({
    sessionId: sessionToRestore.sessionId,
    questions: sessionToRestore.questions.map(q => ({
      text: q.question,
      category: q.category,
      type: q.type,
      questionIndex: q.questionIndex,
      isFollowUp: q.isFollowUp || false,
      mainQuestionIndex: q.parentQuestionIndex !== undefined ? q.parentQuestionIndex : q.mainQuestionIndex,
      followupQuestions: []
    })),
    currentQuestionIndex: sessionToRestore.currentQuestionIndex || 0,
    resumeText: cfg.resumeText,
    jobId: sessionToRestore.jobId
  }))
  
  paused.value = false
  await askWithPreload(currentQuestion.value, currentQuestionObj.value)
}

// 处理继续未完成面试
async function handleContinueUnfinished() {
  if (!unfinishedSession.value) return
  
  forceCreateNew.value = false
  await restoreSession(unfinishedSession.value)
  unfinishedSession.value = null
  showUnfinishedDialog.value = false
}

// 处理开始新面试
async function handleStartNew() {
  showUnfinishedDialog.value = false
  forceCreateNew.value = true
  
  if (unfinishedSession.value?.sessionId) {
    try {
      await interviewApi.deleteSession(unfinishedSession.value.sessionId)
    } catch (err) {
      console.error('删除原会话失败:', err)
    }
  }
  
  unfinishedSession.value = null
  
  // 清除localStorage中的旧会话
  localStorage.removeItem('interviewConfig')
  
  // 重新初始化（会创建新会话）
  await initInterview()
}

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

// 提问（带预加载）
async function askWithPreload(text, questionObj) {
  phase.value = 'asking'
  
  // 确保 avatar 已准备好
  const avatarReady = await waitForAvatar(3000)
  
  if (!avatarReady) {
    console.warn('[TTS] Avatar 未准备好，跳过语音播放')
  } else if (serverOk.value && avatar.value?.preloadTTS) {
    try {
      await avatar.value.preloadTTS(text, cfg.voice, cfg.rate)
      if (avatar.value?.playPrepared) {
        avatar.value.playPrepared().catch(e => console.error('[TTS] 播放失败:', e))
      }
    } catch (e) {
      console.error('[TTS] 预加载失败:', e)
      if (avatar.value?.speak) {
        avatar.value.speak(text, cfg.voice, cfg.rate).catch(e => console.error('[TTS] 播报失败:', e))
      }
    }
  } else if (avatar.value?.speak) {
    avatar.value.speak(text, cfg.voice, cfg.rate).catch(e => console.error('[TTS] 播报失败:', e))
  }
  
  // 语音播放完成后进入思考阶段
  setTimeout(() => {
    if (!paused.value) {
      phase.value = 'thinking'
      startThinking()
      setTimeout(() => {
        if (!paused.value) {
          phase.value = 'answering'
          stopTimers()
        }
      }, cfg.thinkSeconds * 1000)
    }
  }, 3000)
}

// 切换语音输入
async function toggleVoice() {
  if (!canAnswer.value) return
  
  if (listening.value) {
    stopRecording()
  } else {
    startRecording()
  }
}

// 开始录音
async function startRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    audioChunks = []
    recordingStartTime = Date.now()
    
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    
    mediaRecorder.onstop = async () => {
      const audioBlob = new Blob(audioChunks, { type: 'audio/wav' })
      await handleVoiceSubmit(audioBlob)
    }
    
    mediaRecorder.start()
    listening.value = true
  } catch (err) {
    console.error('录音失败:', err)
    alert('无法访问麦克风，请检查权限设置')
  }
}

// 停止录音
function stopRecording() {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
    mediaRecorder.stream.getTracks().forEach(track => track.stop())
  }
  listening.value = false
}

// 分析语音（模拟）
function analyzeVoice(audioBlob, durationMs) {
  // 实际项目中应该调用语音分析 API
  // 这里模拟分析结果：基于音频大小和时长计算
  const durationSec = Math.round(durationMs / 1000)
  
  // 模拟评分逻辑（1-10分制，与 InterviewRoom.vue 一致）
  // 假设：回答时长在 30-120 秒之间为最佳
  let confidenceScore = 6 + Math.random() * 4 // 6-10分
  let clarityScore = 5 + Math.random() * 5    // 5-10分
  
  // 时长影响评分
  if (durationSec < 10) {
    confidenceScore -= 2
    clarityScore -= 1.5
  } else if (durationSec > 180) {
    confidenceScore -= 0.5
  }
  
  // 确保分数在合理范围（1-10）
  confidenceScore = Math.min(10, Math.max(1, Math.round(confidenceScore)))
  clarityScore = Math.min(10, Math.max(1, Math.round(clarityScore)))
  
  return {
    confidence: confidenceScore,
    clarity: clarityScore,
    duration: durationSec
  }
}

// 语音识别：调用ASR接口
async function transcribeAudio(audioBlob) {
  try {
    const formData = new FormData()
    formData.append('file', audioBlob, 'recording.wav')

    const response = await fetch(`${API_BASE}/api/interview/sessions/asr`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getToken() || ''}`
      },
      body: formData
    })

    if (!response.ok) {
      throw new Error(`语音识别失败: ${response.status}`)
    }

    const result = await response.json()

    // 检查是否包含 transcription 字段
    if (result && result.transcription) {
      // 保存完整的语音分析结果
      lastVoiceAnalysis.value = {
        confidenceScore: result.confidenceScore,
        clarityScore: result.clarityScore,
        speechSpeed: result.speechSpeed,
        analysisReason: result.analysisReason
      }
      return result.transcription
    }

    // 兼容旧格式
    if (result.success && result.result) {
      return result.result
    }

    throw new Error('语音识别返回格式错误')
  } catch (error) {
    console.error('语音识别失败:', error)
    return null
  }
}

// 处理语音提交
async function handleVoiceSubmit(audioBlob) {
  if (!audioBlob) return
  
  // 计算录音时长
  const durationMs = recordingStartTime ? Date.now() - recordingStartTime : 0
  recordingStartTime = null
  
  // 调用语音识别
  const recognizedText = await transcribeAudio(audioBlob)
  
  // 使用后端返回的语音分析数据，如果没有则使用前端模拟
  const analysis = lastVoiceAnalysis.value
  if (analysis) {
    // 后端返回的是 0-100 分制，转换为 1-10 分制显示
    voiceAnalysis.value = {
      confidence: Math.min(10, Math.max(1, Math.round(analysis.confidenceScore / 10))),
      clarity: Math.min(10, Math.max(1, Math.round(analysis.clarityScore / 10))),
      duration: Math.round(durationMs / 1000)
    }
    showVoiceAnalysis.value = true
    
    // 3秒后自动隐藏分析结果
    setTimeout(() => {
      showVoiceAnalysis.value = false
    }, 3000)
  }
  
  if (recognizedText) {
    // 识别成功，使用识别文本
    console.log('[语音识别成功]', recognizedText)
    await submitAnswerAndGetNext(recognizedText)
  } else {
    // 识别失败，使用时长占位
    const dur = Math.max(1, Math.round(durationMs / 1000))
    const fallbackText = `已回答完毕，时长 ${dur}s`
    console.log('[语音识别失败，使用占位]', fallbackText)
    await submitAnswerAndGetNext(fallbackText)
  }
  
  // 清空本次的ASR分析结果
  lastVoiceAnalysis.value = null
}

// 提交答案并获取下一题
async function submitAnswerAndGetNext(answerText, maxRetries = 3) {
  const currentQ = currentQuestionObj.value
  
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const response = await interviewApi.submitAnswer({
        sessionId: interviewSession.value.sessionId,
        questionIndex: currentQ.questionIndex,
        answer: answerText,
        addQuestionIndex: currentQ.addQuestionIndex || 0
      })
      
      // 处理后端返回的下一题
      if (response.hasNextQuestion && response.nextQuestion) {
        const newQuestion = {
          ...response.nextQuestion,
          isFollowUp: response.nextQuestion.isFollowUp || false,
          mainQuestionIndex: response.nextQuestion.isFollowUp ? response.nextQuestion.parentQuestionIndex : undefined
        }
        interviewSession.value.questions.push(newQuestion)
      }
      
      // 进入下一题
      await nextQuestion()
      return
    } catch (err) {
      console.error(`提交答案失败 (尝试 ${attempt}/${maxRetries}):`, err)
      if (attempt === maxRetries) {
        // 最后一次尝试失败，直接进入下一题
        await nextQuestion()
      } else {
        await new Promise(resolve => setTimeout(resolve, 1500 * attempt))
      }
    }
  }
}

// 下一题
async function nextQuestion() {
  const nextIndex = interviewSession.value.currentQuestionIndex + 1
  
  if (nextIndex >= interviewSession.value.questions.length) {
    await onInterviewComplete()
    return
  }
  
  interviewSession.value.currentQuestionIndex = nextIndex
  
  const nextQ = interviewSession.value.questions[nextIndex]
  currentQuestionObj.value = {
    questionIndex: nextQ.questionIndex || nextIndex,
    question: nextQ.question || nextQ.text || '',
    type: nextQ.type || '',
    category: nextQ.category || '',
    addQuestionIndex: 0
  }
  
  await askWithPreload(currentQuestion.value, currentQuestionObj.value)
}

// 暂停/继续
function togglePause() {
  paused.value = !paused.value
}

// 格式化时间
function fmt(sec) {
  const s = Math.max(0, Math.floor(sec || 0))
  const mm = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${mm}:${ss}`
}

// 开始思考倒计时
function startThinking() {
  thinkLeft.value = cfg.thinkSeconds
  if (thinkingTimer) clearInterval(thinkingTimer)
  thinkingTimer = setInterval(() => {
    if (!paused.value) {
      thinkLeft.value -= 1
      if (thinkLeft.value <= 0) {
        clearInterval(thinkingTimer)
        thinkingTimer = null
      }
    }
  }, 1000)
}

// 停止计时器
function stopTimers() {
  if (thinkingTimer) {
    clearInterval(thinkingTimer)
    thinkingTimer = null
  }
}

// 暂时退出
async function handleTempExit() {
  isExiting.value = true
  try {
    const currentQ = currentQuestionObj.value
    if (currentQ.question) {
      try {
        await interviewApi.saveAnswer({
          sessionId: interviewSession.value.sessionId,
          questionIndex: currentQ.questionIndex,
          answer: '（用户暂时退出）',
          addQuestionIndex: currentQ.addQuestionIndex || 0
        })
      } catch (err) {
        console.error('暂存答案失败:', err)
      }
    }
    
    localStorage.removeItem('interviewConfig')
    showExitConfirm.value = false
    router.push('/app/home')
  } catch (err) {
    console.error('暂时退出失败:', err)
  } finally {
    isExiting.value = false
  }
}

// 提前交卷
async function finishInterview() {
  if (!interviewSession.value.sessionId) return
  
  isSubmitting.value = true
  try {
    showCompleteConfirm.value = false
    showExitConfirm.value = false
    await onInterviewComplete()
  } catch (err) {
    console.error('提前交卷失败:', err)
  } finally {
    isSubmitting.value = false
  }
}

// 面试完成
async function onInterviewComplete() {
  localStorage.removeItem('interviewConfig')
  
  const isLocalSession = interviewSession.value.sessionId.startsWith('local_')
  if (isLocalSession) {
    showEvaluationSuccessModal.value = true
    return
  }
  
  try {
    await interviewApi.completeInterview(interviewSession.value.sessionId)
  } catch (err) {
    console.error('完成面试接口调用失败:', err)
  }
  
  router.replace(`/app/home/interview/report-loading?sessionId=${interviewSession.value.sessionId}`)
}

// 关闭弹窗
function closeInterviewCompleteModal() {
  showInterviewCompleteModal.value = false
  interviewDetail.value = null
  interviewDetailError.value = null
  router.push('/app/home')
}

function closeEvaluationSuccessModal() {
  showEvaluationSuccessModal.value = false
  router.push('/app/home/study-plan')
}

// 创建新面试会话
async function createNewSession() {
  const savedSettings = JSON.parse(localStorage.getItem('interviewSettings') || '{}')
  const resumeText = savedSettings.resumeText || ''
  const questionCount = parseInt(savedSettings.questionCount) || 8
  
  const data = await interviewApi.createSession({
    resumeText: resumeText,
    questionCount: questionCount,
    resumeId: parseInt(savedSettings.resumeId) || null,
    jobId: parseInt(savedSettings.jobId) || 0,
    knowledgeBaseIds: savedSettings.knowledgeBaseIds || [],
    forceCreate: true
  })
  
  // 保存配置
  localStorage.setItem('interviewConfig', JSON.stringify({
    sessionId: data.sessionId,
    questions: data.questions.map(q => ({
      text: q.question,
      category: q.category,
      type: q.type,
      questionIndex: q.questionIndex,
      isFollowUp: q.isFollowUp || false,
      mainQuestionIndex: q.parentQuestionIndex !== undefined ? q.parentQuestionIndex : q.mainQuestionIndex,
      followupQuestions: []
    })),
    currentQuestionIndex: data.currentQuestionIndex || 0,
    resumeText: resumeText,
    jobId: savedSettings.jobId
  }))
  
  // 初始化会话 - 确保每个问题都有 questionIndex
  interviewSession.value.sessionId = data.sessionId
  interviewSession.value.questions = data.questions.map((q, index) => ({
    ...q,
    questionIndex: q.questionIndex !== undefined ? q.questionIndex : index
  }))
  interviewSession.value.currentQuestionIndex = data.currentQuestionIndex || 0
  
  // 初始化当前问题
  if (data.questions && data.questions.length > 0) {
    const firstQuestion = interviewSession.value.questions[0]
    currentQuestionObj.value = {
      questionIndex: firstQuestion.questionIndex,
      question: firstQuestion.question || '',
      type: firstQuestion.type || '',
      category: firstQuestion.category || '',
      addQuestionIndex: 0
    }
  }
  
  paused.value = false
  await askWithPreload(currentQuestion.value, currentQuestionObj.value)
}

// 初始化面试
async function initInterview() {
  // 首先检查是否有未完成的面试
  if (!forceCreateNew.value && cfg.resumeId) {
    await checkUnfinishedSession()
    if (unfinishedSession.value) {
      return
    }
  }
  
  const savedConfig = JSON.parse(localStorage.getItem('interviewConfig') || '{}')
  
  if (!savedConfig.sessionId || !savedConfig.questions || forceCreateNew.value) {
    // 没有配置或强制创建新会话，创建新会话
    await createNewSession()
    return
  }
  
  // 使用已有配置 - 确保每个问题都有 questionIndex
  interviewSession.value.sessionId = savedConfig.sessionId
  interviewSession.value.questions = savedConfig.questions.map((q, index) => ({
    ...q,
    questionIndex: q.questionIndex !== undefined ? q.questionIndex : index
  }))
  interviewSession.value.currentQuestionIndex = savedConfig.currentQuestionIndex || 0
  
  const currentQ = interviewSession.value.questions[savedConfig.currentQuestionIndex || 0]
  if (currentQ) {
    currentQuestionObj.value = {
      questionIndex: currentQ.questionIndex,
      question: currentQ.question || currentQ.text || '',
      type: currentQ.type || '',
      category: currentQ.category || '',
      addQuestionIndex: 0
    }
  }
  
  paused.value = false
  await askWithPreload(currentQuestion.value, currentQuestionObj.value)
}

// 页面事件处理
const handleBeforeUnload = (e) => {
  if (interviewSession.value.sessionId && currentQuestionObj.value.question) {
    e.preventDefault()
    e.returnValue = '面试还在进行中，确定要离开吗？'
    return e.returnValue
  }
}

const handlePopState = (e) => {
  if (interviewSession.value.sessionId && 
      currentQuestionObj.value.question && 
      !showExitConfirm.value &&
      !showCompleteConfirm.value) {
    history.pushState(null, '', location.href)
    showExitConfirm.value = true
  }
}

// 生命周期
onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  history.pushState(null, '', location.href)
  window.addEventListener('popstate', handlePopState)
  
  await checkServer()
  await initInterview()
})

onBeforeUnmount(() => {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
  }
  stopTimers()
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('popstate', handlePopState)
})
</script>

<style scoped>
.realistic-interview-room {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  background: var(--bg0);
  position: relative;
  overflow: hidden;
}

/* 全屏模式 */
.realistic-interview-room.fullscreen-mode {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  height: 100vh;
  width: 100vw;
}

/* 按钮组样式 - 参照 InterviewRoom.vue */
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

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--stroke);
  border-radius: 10px;
  background: var(--panel);
  color: var(--text);
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--panel2);
  border-color: var(--stroke2);
  transform: translateY(-2px);
}

.icon-btn.danger:hover {
  background: rgba(239,68,68,0.3);
  border-color: rgba(239,68,68,0.5);
}

/* 全屏模式下图标按钮样式 */
.fullscreen-mode .icon-btn {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 12px;
  background: rgba(0,0,0,0.3);
  color: white;
  font-size: 18px;
  backdrop-filter: blur(10px);
}

.fullscreen-mode .icon-btn:hover {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.4);
}

/* Live2D区域 - 参照 InterviewRoom.vue 的实现 */
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

.live2d-stage {
  width: 300px;
  max-height: 390px;
  height: 45vh;
  min-height: 300px;
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(100, 108, 255, 0.1) 0%, rgba(54, 211, 153, 0.05) 100%);
  border: 1px solid var(--stroke);
  margin-top: 20px;
}

/* 全屏模式下Live2D区域占满空间 */
.fullscreen-mode .avatar-section {
  flex: 1;
  max-height: none;
  padding: 40px 40px 120px;
  justify-content: flex-start;
}

.fullscreen-mode .live2d-stage {
  width: 50%;
  max-width: 600px;
  height: auto;
  aspect-ratio: 3/4;
  background: transparent;
  border: none;
}

.live2d-stage :deep(canvas) {
  width: 100% !important;
  height: 100% !important;
  object-fit: contain;
}

/* 状态指示器 - 参照 InterviewRoom.vue */
.phase-indicator {
  margin-top: 16px;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 16px;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 20px;
  color: var(--text);
}

/* 全屏模式下状态指示器样式 */
.fullscreen-mode .phase-indicator {
  position: absolute;
  bottom: 220px;
  padding: 12px 28px;
  border-radius: 30px;
  color: white;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  animation: pulse 2s infinite;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  border: none;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

/* 进度显示 */
.question-progress {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* 全屏模式下进度显示 */
.fullscreen-mode .question-progress {
  position: absolute;
  bottom: 140px;
}

.progress-text {
  color: rgba(255,255,255,0.8);
  font-size: 14px;
  font-weight: 500;
}

.progress-bar-mini {
  width: 120px;
  height: 4px;
  background: rgba(255,255,255,0.2);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill-mini {
  height: 100%;
  background: var(--brand);
  border-radius: 2px;
  transition: width 0.3s ease;
}

/* 底部语音控制区 - 非全屏时显示在内容下方 */
.voice-control-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px;
  z-index: 100;
  margin-top: 100px;
}

/* 全屏模式下语音控制区固定在底部 */
.fullscreen-mode .voice-control-section {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  padding: 0;
}

.voice-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.voice-wave {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 40px;
}

.voice-wave span {
  width: 4px;
  height: 20px;
  background: var(--brand);
  border-radius: 2px;
  animation: wave 1s ease-in-out infinite;
}

.voice-wave.active span {
  animation: wave-active 0.5s ease-in-out infinite;
}

@keyframes wave {
  0%, 100% { height: 20px; }
  50% { height: 30px; }
}

@keyframes wave-active {
  0%, 100% { height: 20px; opacity: 0.5; }
  50% { height: 40px; opacity: 1; }
}

.voice-hint {
  color: rgba(255,255,255,0.8);
  font-size: 14px;
  letter-spacing: 1px;
}

/* 语音分析面板 - 非全屏时显示在内容下方 */
.voice-analysis-panel {
  margin: 16px auto;
  background: var(--panel);
  border-radius: 16px;
  padding: 16px 20px;
  border: 1px solid var(--stroke);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  z-index: 50;
  max-width: 400px;
}

/* 全屏模式下语音分析面板固定在底部 */
.fullscreen-mode .voice-analysis-panel {
  position: absolute;
  bottom: 180px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.1);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3);
  margin: 0;
}

.voice-scores {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

.score-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  white-space: nowrap;
}

.score-tag.confidence {
  background: rgba(255, 193, 7, 0.3);
}

.score-tag.clarity {
  background: rgba(33, 150, 243, 0.3);
}

.score-tag.duration {
  background: rgba(76, 175, 80, 0.3);
}

.score-icon {
  font-size: 12px;
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(10px);
}

.voice-btn {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid rgba(255,255,255,0.3);
  background: rgba(0,0,0,0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  backdrop-filter: blur(10px);
}

.voice-btn:hover {
  transform: scale(1.05);
  border-color: rgba(255,255,255,0.5);
}

.voice-btn.active {
  border-color: #ef4444;
  background: rgba(239,68,68,0.2);
  animation: recording-pulse 1.5s infinite;
}

@keyframes recording-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239,68,68,0.4); }
  50% { box-shadow: 0 0 0 20px rgba(239,68,68,0); }
}

.voice-icon {
  font-size: 36px;
}

.waiting-hint {
  color: rgba(255,255,255,0.7);
  font-size: 16px;
  letter-spacing: 1px;
}

.thinking-dots span {
  animation: dots 1.4s infinite;
  opacity: 0;
}

.thinking-dots span:nth-child(2) { animation-delay: 0.2s; }
.thinking-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dots {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}

/* 全屏模式 */
.fullscreen-mode {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
}

/* 弹窗内样式 */
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

.score-section {
  text-align: center;
  padding: 20px;
  background: var(--panel);
  border-radius: 8px;
  border: 1px solid var(--stroke);
}

.score-circle {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 20px auto;
}

.score-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.score-progress {
  transition: stroke-dashoffset 1s ease;
}

.score-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.score-number {
  font-size: 36px;
  font-weight: 700;
  color: var(--text);
}

.score-total {
  font-size: 16px;
  color: var(--muted);
}

.score-feedback {
  color: var(--muted);
  font-size: 14px;
  line-height: 1.6;
}

.evaluation-success-content {
  text-align: center;
  padding: 30px;
}

.success-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.success-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 8px;
}

.success-message {
  color: var(--muted);
  margin-bottom: 8px;
}

.success-hint {
  color: var(--brand);
  font-size: 14px;
}
</style>
