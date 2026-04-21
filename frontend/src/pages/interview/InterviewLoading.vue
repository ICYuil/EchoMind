<template>
  <div class="interview-loading-page">
    <div class="loading-container">
      <!-- 动态圆圈动画 -->
      <div class="loading-animation">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
        <div class="circle circle-3"></div>
        <div class="circle circle-4"></div>
        <div class="center-icon">🎯</div>
      </div>

      <!-- 加载文字 -->
      <div class="loading-text">
        <h2 class="title">正在准备面试</h2>
        <p class="subtitle">{{ currentStep }}</p>
      </div>

      <!-- 进度条 -->
      <div class="progress-container">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <span class="progress-text">{{ progress }}%</span>
      </div>

      <!-- 准备事项列表 -->
      <div class="preparation-list">
        <div 
          v-for="(item, index) in preparationItems" 
          :key="index"
          class="prep-item"
          :class="{ completed: item.completed, active: item.active }"
        >
          <span class="prep-icon">{{ item.completed ? '✓' : (item.active ? '◉' : '○') }}</span>
          <span class="prep-text">{{ item.text }}</span>
        </div>
      </div>

      <!-- 提示信息 -->
      <div class="tips-section">
        <div class="tip-card">
          <span class="tip-icon">💡</span>
          <span class="tip-text">{{ currentTip }}</span>
        </div>
      </div>
    </div>

    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <!-- 未完成面试提示弹窗 -->
    <div v-if="showUnfinishedDialog" class="unfinished-dialog-overlay" @click="handleOverlayClick">
      <div class="unfinished-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-icon">📋</div>
          <h3 class="dialog-title">发现未完成的面试</h3>
        </div>
        <div class="dialog-content">
          <p class="dialog-message">
            您有一个进行中的面试（题目 {{ unfinishedSession?.currentQuestionIndex + 1 }}/{{ unfinishedSession?.questions?.length }}），是否继续？
          </p>
          <div class="dialog-info">
            <div class="info-item">
              <span class="info-label">当前进度：</span>
              <span class="info-value">{{ Math.round(((unfinishedSession?.currentQuestionIndex || 0) / (unfinishedSession?.questions?.length || 1)) * 100) }}%</span>
            </div>
            <div class="info-item">
              <span class="info-label">已答题目：</span>
              <span class="info-value">{{ unfinishedSession?.currentQuestionIndex || 0 }} 题</span>
            </div>
          </div>
        </div>
        <div class="dialog-actions">
          <button class="btn-secondary" @click="handleStartNew">
            <span class="btn-icon">🆕</span>
            开始新面试
          </button>
          <button class="btn-primary" @click="handleContinue">
            <span class="btn-icon">▶️</span>
            继续面试
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { interviewApi } from '../../api/interview.js'

const router = useRouter()

// 进度
const progress = ref(0)
const currentStep = ref('初始化面试环境...')

// 准备事项
const preparationItems = ref([
  { text: '读取简历信息', completed: false, active: true },
  { text: '加载知识库', completed: false, active: false },
  { text: '生成面试题目', completed: false, active: false },
  { text: '准备面试房间', completed: false, active: false }
])

// 提示语
const tips = [
  '面试过程中保持自信，展现真实的自己',
  '回答问题时尽量具体，用实际案例支撑',
  '遇到不会的问题可以坦诚说明，展示学习能力',
  '注意控制回答时间，突出重点',
  '面试前调整好摄像头和麦克风'
]
const currentTip = ref(tips[0])

// 未完成面试弹窗相关
const showUnfinishedDialog = ref(false)
const unfinishedSession = ref(null)
const sessionData = ref(null)

let progressTimer = null
let tipTimer = null

// 更新准备事项状态
const updatePreparationItem = (index, completed = true) => {
  if (index < preparationItems.value.length) {
    preparationItems.value[index].completed = completed
    preparationItems.value[index].active = false
    if (index + 1 < preparationItems.value.length) {
      preparationItems.value[index + 1].active = true
    }
  }
}

// 模拟进度增长
const startProgress = () => {
  progressTimer = setInterval(() => {
    if (progress.value < 90) {
      progress.value += Math.random() * 3
      if (progress.value > 90) progress.value = 90
    }
  }, 200)
}

// 切换提示
const startTipRotation = () => {
  let tipIndex = 0
  tipTimer = setInterval(() => {
    tipIndex = (tipIndex + 1) % tips.length
    currentTip.value = tips[tipIndex]
  }, 4000)
}

// 检查返回的是否是未完成会话（已答题进度 > 0）
const isUnfinishedSession = (data) => {
  // 如果 currentQuestionIndex > 0，说明已经答过题了，是未完成会话
  return data.currentQuestionIndex > 0
}

// 处理继续面试
const handleContinue = () => {
  if (!sessionData.value) return
  
  // 保存会话信息到 localStorage
  const settings = JSON.parse(localStorage.getItem('interviewSettings') || '{}')
  const resumeText = localStorage.getItem('resumeText') || ''
  
  localStorage.setItem('interviewConfig', JSON.stringify({
    sessionId: sessionData.value.sessionId,
    questions: sessionData.value.questions.map(q => ({
      text: q.question,
      category: q.category,
      type: q.type,
      questionIndex: q.questionIndex,
      isFollowUp: q.isFollowUp || false,
      mainQuestionIndex: q.parentQuestionIndex !== undefined ? q.parentQuestionIndex : q.mainQuestionIndex,
      followupQuestions: []
    })),
    currentQuestionIndex: sessionData.value.currentQuestionIndex || 0,
    resumeText: resumeText,
    jobId: settings.jobId
  }))
  
  // 关闭弹窗并跳转
  showUnfinishedDialog.value = false
  router.replace('/app/home/interview/room')
}

// 处理开始新面试（强制创建新会话）
const handleStartNew = async () => {
  showUnfinishedDialog.value = false
  
  try {
    currentStep.value = '正在创建新面试...'
    const settings = JSON.parse(localStorage.getItem('interviewSettings') || '{}')
    const resumeText = localStorage.getItem('resumeText') || ''
    const questionCount = parseInt(settings.questionCount) || 8
    
    // 1. 先删除原来的未完成会话（如果有）
    if (sessionData.value?.sessionId) {
      try {
        await interviewApi.deleteSession(sessionData.value.sessionId)
        console.log('已删除原未完成会话:', sessionData.value.sessionId)
      } catch (err) {
        console.error('删除原未完成会话失败:', err)
        // 继续创建新会话，不阻塞流程
      }
    }
    
    // 2. 强制创建新会话
    const data = await interviewApi.createSession({
      resumeText: resumeText,
      questionCount: questionCount,
      resumeId: parseInt(settings.resumeId) || null,
      jobId: parseInt(settings.jobId) || 0,
      knowledgeBaseIds: settings.knowledgeBaseIds || [],
      forceCreate: true  // 强制创建新会话
    })
    
    // 3. 保存新会话信息
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
      currentQuestionIndex: 0,  // 新会话从第一题开始
      resumeText: resumeText,
      jobId: settings.jobId
    }))
    
    // 4. 清理临时数据
    unfinishedSession.value = null
    sessionData.value = null
    
    // 5. 跳转到面试房间
    router.replace('/app/home/interview/room')
  } catch (error) {
    console.error('创建新面试失败:', error)
    currentStep.value = '创建新面试失败，请刷新页面重试'
  }
}

// 处理点击遮罩层（阻止关闭）
const handleOverlayClick = () => {
  // 点击遮罩层不关闭弹窗，必须让用户做出选择
}

// 初始化面试
const initInterview = async () => {
  try {
    // 读取面试设置
    const settings = JSON.parse(localStorage.getItem('interviewSettings') || '{}')
    
    if (!settings.resumeId) {
      router.replace('/app/home/interview/select-resume')
      return
    }

    // 步骤1: 读取简历信息
    currentStep.value = '正在读取简历信息...'
    await new Promise(resolve => setTimeout(resolve, 500))
    updatePreparationItem(0)
    progress.value = 20

    // 步骤2: 加载知识库
    currentStep.value = '正在加载知识库...'
    await new Promise(resolve => setTimeout(resolve, 600))
    updatePreparationItem(1)
    progress.value = 35

    // 步骤3: 创建面试会话（生成题目）
    currentStep.value = 'AI 正在生成个性化面试题目...'
    updatePreparationItem(2)
    
    const resumeText = localStorage.getItem('resumeText') || ''
    
    // 确保 questionCount 是数字，使用用户选择的值
    const questionCount = parseInt(settings.questionCount) || 8
    console.log('用户选择的题目数量:', settings.questionCount, '转换后:', questionCount)
    
    const data = await interviewApi.createSession({
      resumeText: resumeText,
      questionCount: questionCount,
      resumeId: parseInt(settings.resumeId) || null,
      jobId: parseInt(settings.jobId) || 0,
      knowledgeBaseIds: settings.knowledgeBaseIds || [],
      forceCreate: false
    })
    
    console.log('后端返回的题目数量:', data.questions?.length)
    console.log('后端返回的当前题目索引:', data.currentQuestionIndex)
    console.log('后端返回的题目列表:', data.questions)

    // 检查是否是未完成会话
    if (isUnfinishedSession(data)) {
      console.log('检测到未完成会话，显示选择弹窗')
      sessionData.value = data
      unfinishedSession.value = {
        currentQuestionIndex: data.currentQuestionIndex,
        questions: data.questions
      }
      showUnfinishedDialog.value = true
      return  // 停止后续流程，等待用户选择
    }

    // 是新会话，直接保存并跳转
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
      jobId: settings.jobId
    }))

    progress.value = 85
    updatePreparationItem(2, true)

    // 步骤4: 准备面试房间
    currentStep.value = '正在准备面试房间...'
    await new Promise(resolve => setTimeout(resolve, 500))
    updatePreparationItem(3)
    progress.value = 100

    // 完成，根据房间类型跳转到不同的面试房间
    const interviewSettings = JSON.parse(localStorage.getItem('interviewSettings') || '{}')
    const roomType = interviewSettings.roomType || 'normal'
    
    setTimeout(() => {
      if (roomType === 'realistic') {
        router.replace('/app/home/interview/room-realistic')
      } else {
        router.replace('/app/home/interview/room')
      }
    }, 500)

  } catch (error) {
    console.error('初始化面试失败:', error)
    currentStep.value = '连接失败，正在重试...'
    
    // 显示错误但继续重试，而不是降级
    preparationItems.value[2].active = true
    preparationItems.value[2].completed = false
    
    // 延迟后重试
    setTimeout(() => {
      initInterview()
    }, 3000)
    return
  }
}

onMounted(() => {
  startProgress()
  startTipRotation()
  initInterview()
})

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
  if (tipTimer) clearInterval(tipTimer)
})
</script>

<style scoped>
.interview-loading-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--bg) 0%, var(--panel) 100%);
  position: relative;
  overflow: hidden;
}

.loading-container {
  text-align: center;
  z-index: 10;
  padding: 40px;
  max-width: 500px;
  width: 100%;
}

/* 动态圆圈动画 */
.loading-animation {
  position: relative;
  width: 150px;
  height: 150px;
  margin: 0 auto 40px;
}

.circle {
  position: absolute;
  border-radius: 50%;
  border: 3px solid transparent;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.circle-1 {
  width: 150px;
  height: 150px;
  border-top-color: var(--brand);
  border-right-color: var(--brand);
  animation: spin 2s linear infinite;
}

.circle-2 {
  width: 120px;
  height: 120px;
  border-bottom-color: #8b5cf6;
  border-left-color: #8b5cf6;
  animation: spin-reverse 1.5s linear infinite;
}

.circle-3 {
  width: 90px;
  height: 90px;
  border-top-color: #06b6d4;
  border-right-color: #06b6d4;
  animation: spin 1s linear infinite;
}

.circle-4 {
  width: 60px;
  height: 60px;
  border-bottom-color: #10b981;
  border-left-color: #10b981;
  animation: spin-reverse 0.8s linear infinite;
}

.center-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes spin {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

@keyframes spin-reverse {
  from { transform: translate(-50%, -50%) rotate(360deg); }
  to { transform: translate(-50%, -50%) rotate(0deg); }
}

@keyframes pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.2); }
}

/* 加载文字 */
.loading-text {
  margin-bottom: 30px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 12px;
  background: linear-gradient(135deg, var(--brand) 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 16px;
  color: var(--muted);
  animation: fadeInOut 2s ease-in-out infinite;
}

@keyframes fadeInOut {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* 进度条 */
.progress-container {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 30px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: var(--stroke);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--brand) 0%, #8b5cf6 50%, #06b6d4 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.5);
}

.progress-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--brand);
  min-width: 40px;
}

/* 准备事项列表 */
.preparation-list {
  background: var(--panel);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid var(--stroke);
}

.prep-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--stroke);
  transition: all 0.3s ease;
}

.prep-item:last-child {
  border-bottom: none;
}

.prep-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 14px;
  transition: all 0.3s ease;
}

.prep-item.completed .prep-icon {
  background: #10b981;
  color: white;
}

.prep-item.active .prep-icon {
  background: var(--brand);
  color: white;
  animation: pulse-icon 1s ease-in-out infinite;
}

.prep-item:not(.completed):not(.active) .prep-icon {
  color: var(--muted);
}

@keyframes pulse-icon {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.prep-text {
  font-size: 14px;
  color: var(--text);
  transition: all 0.3s ease;
}

.prep-item.completed .prep-text {
  color: var(--muted);
  text-decoration: line-through;
}

.prep-item.active .prep-text {
  color: var(--brand);
  font-weight: 500;
}

/* 提示信息 */
.tips-section {
  margin-top: 20px;
}

.tip-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 12px;
  animation: slideIn 0.5s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.tip-icon {
  font-size: 20px;
  animation: bounce 1s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.tip-text {
  font-size: 14px;
  color: var(--text);
  text-align: left;
  line-height: 1.5;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}

.bg-circle-1 {
  width: 400px;
  height: 400px;
  background: var(--brand);
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.bg-circle-2 {
  width: 300px;
  height: 300px;
  background: #8b5cf6;
  bottom: -50px;
  left: -50px;
  animation: float 10s ease-in-out infinite reverse;
}

.bg-circle-3 {
  width: 200px;
  height: 200px;
  background: #06b6d4;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 12s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
}

/* 未完成面试弹窗样式 */
.unfinished-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.unfinished-dialog {
  background: var(--panel);
  border-radius: 20px;
  padding: 32px;
  max-width: 480px;
  width: 90%;
  border: 1px solid var(--stroke);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dialog-header {
  text-align: center;
  margin-bottom: 24px;
}

.dialog-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.dialog-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.dialog-content {
  margin-bottom: 24px;
}

.dialog-message {
  font-size: 16px;
  color: var(--text);
  line-height: 1.6;
  margin-bottom: 20px;
  text-align: center;
}

.dialog-info {
  background: var(--bg);
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-around;
  gap: 20px;
}

.info-item {
  text-align: center;
}

.info-label {
  font-size: 13px;
  color: var(--muted);
  display: block;
  margin-bottom: 4px;
}

.info-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--brand);
}

.dialog-actions {
  display: flex;
  gap: 12px;
}

.btn-secondary,
.btn-primary {
  flex: 1;
  padding: 14px 20px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
}

.btn-secondary {
  background: var(--bg);
  color: var(--text);
  border: 1px solid var(--stroke);
}

.btn-secondary:hover {
  background: var(--panel);
  transform: translateY(-2px);
}

.btn-primary {
  background: linear-gradient(135deg, var(--brand) 0%, #8b5cf6 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.4);
}

.btn-icon {
  font-size: 16px;
}
</style>
