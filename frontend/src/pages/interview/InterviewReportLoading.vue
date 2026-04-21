<template>
  <div class="report-loading-page">
    <!-- 粒子背景 -->
    <div class="particles-container">
      <div v-for="n in 30" :key="n" class="particle" :style="getParticleStyle(n)"></div>
    </div>

    <!-- 成功弹窗 -->
    <div v-if="showSuccessModal" class="success-modal-overlay" @click="closeSuccessModal">
      <div class="success-modal" @click.stop>
        <div class="success-icon">✅</div>
        <h2 class="success-title">面试分析完成！</h2>
        <p class="success-message">AI 已完成对您面试表现的深度分析</p>
        <div class="success-actions">
          <button class="btn-primary" @click="goToHome">返回主界面</button>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="loading-content" v-if="!showSuccessModal">
      <!-- 中央动画区域 -->
      <div class="center-animation">
        <!-- 外圈光环 -->
        <div class="orbit-ring ring-1"></div>
        <div class="orbit-ring ring-2"></div>
        <div class="orbit-ring ring-3"></div>

        <!-- 核心球体 -->
        <div class="core-sphere">
          <div class="sphere-inner">
            <span class="brain-icon">🧠</span>
          </div>
          <div class="sphere-glow"></div>
        </div>

        <!-- 环绕粒子 -->
        <div class="orbiting-particles">
          <div class="orbit-particle p1">✨</div>
          <div class="orbit-particle p2">💫</div>
          <div class="orbit-particle p3">⭐</div>
          <div class="orbit-particle p4">🌟</div>
        </div>
      </div>

      <!-- 标题和状态 -->
      <div class="status-section">
        <h1 class="main-title">AI 正在深度分析</h1>
        <p class="status-text">{{ currentStatus }}</p>
      </div>

      <!-- 波浪进度条 -->
      <div class="wave-progress-container">
        <div class="wave-progress">
          <div class="wave wave-1" :style="{ left: -100 + progress + '%' }"></div>
          <div class="wave wave-2" :style="{ left: -100 + progress + '%' }"></div>
        </div>
        <span class="progress-percent">{{ Math.floor(progress) }}%</span>
      </div>

      <!-- 分析维度卡片 -->
      <div class="analysis-dimensions">
        <div
          v-for="(dim, index) in dimensions"
          :key="index"
          class="dimension-card"
          :class="{ active: dim.active, completed: dim.completed }"
        >
          <div class="dim-icon">{{ dim.icon }}</div>
          <div class="dim-content">
            <span class="dim-name">{{ dim.name }}</span>
            <div class="dim-bar">
              <div class="dim-fill" :style="{ width: dim.progress + '%' }"></div>
            </div>
          </div>
          <div class="dim-status">
            <span v-if="dim.completed" class="check">✓</span>
            <span v-else-if="dim.active" class="loading-dot">●</span>
            <span v-else class="wait">○</span>
          </div>
        </div>
      </div>

      <!-- 动态提示语 -->
      <div class="tips-carousel">
        <div class="tip-item" :class="{ fade: tipFading }">
          <span class="tip-bulb">💡</span>
          <span class="tip-content">{{ currentTip }}</span>
        </div>
      </div>
    </div>

    <!-- 底部装饰 -->
    <div class="bottom-decoration">
      <div class="glow-line"></div>
      <div class="glow-line delay-1"></div>
      <div class="glow-line delay-2"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { interviewApi } from '../../api/interview.js'

const router = useRouter()
const route = useRoute()

// 进度
const progress = ref(0)
const currentStatus = ref('正在整理面试数据...')

// 成功弹窗
const showSuccessModal = ref(false)

// 是否已获取到报告（防止重复处理）
const hasReport = ref(false)

// 关闭弹窗
const closeSuccessModal = () => {
  showSuccessModal.value = false
}

// 返回主界面
const goToHome = () => {
  showSuccessModal.value = false
  router.replace('/app/home')
}

// 分析维度
const dimensions = ref([
  { name: '回答内容分析', icon: '📝', progress: 0, active: true, completed: false },
  { name: '技术能力评估', icon: '🔧', progress: 0, active: false, completed: false },
  { name: '沟通表达评分', icon: '🗣️', progress: 0, active: false, completed: false },
  { name: '逻辑思维检测', icon: '🧩', progress: 0, active: false, completed: false },
  { name: '综合潜力评估', icon: '🚀', progress: 0, active: false, completed: false }
])

// 提示语
const tips = [
  'AI 正在分析您的每一次回答，挖掘您的技术深度',
  '不仅看答案对错，更关注您的思考过程和表达方式',
  '综合评估您的专业能力、沟通技巧和成长潜力',
  '生成个性化的改进建议，助力您的职业发展',
  '每一个维度都经过多轮验证，确保评价公正客观'
]
const currentTip = ref(tips[0])
const tipFading = ref(false)

// 粒子样式
const getParticleStyle = (n) => {
  const delay = Math.random() * 5
  const duration = 3 + Math.random() * 4
  const size = 2 + Math.random() * 6
  const left = Math.random() * 100
  return {
    left: `${left}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

let progressTimer = null
let tipTimer = null
let dimensionTimer = null

// 更新维度进度
const updateDimensions = () => {
  let activeIndex = dimensions.value.findIndex(d => d.active)
  if (activeIndex === -1) activeIndex = 0

  const activeDim = dimensions.value[activeIndex]
  if (activeDim.progress < 100) {
    activeDim.progress += Math.random() * 8 + 2
    if (activeDim.progress >= 100) {
      activeDim.progress = 100
      activeDim.completed = true
      activeDim.active = false

      // 激活下一个维度
      if (activeIndex < dimensions.value.length - 1) {
        dimensions.value[activeIndex + 1].active = true
      }
    }
  }
}

// 切换提示
const rotateTips = () => {
  let tipIndex = 0
  tipTimer = setInterval(() => {
    tipFading.value = true
    setTimeout(() => {
      tipIndex = (tipIndex + 1) % tips.length
      currentTip.value = tips[tipIndex]
      tipFading.value = false
    }, 300)
  }, 5000)
}

// 获取面试报告
const fetchReport = async () => {
  const sessionId = route.query.sessionId
  if (!sessionId) {
    router.replace('/app/home')
    return
  }

  // 检查是否已经在评估中（避免重复轮询）
  const evaluatingKey = `evaluating_${sessionId}`
  const isAlreadyEvaluating = localStorage.getItem(evaluatingKey)
  
  if (isAlreadyEvaluating) {
    // 已经在评估中，显示提示并继续等待，不创建新的定时器
    currentStatus.value = '评估已在进行中，继续等待结果...'
    return
  }
  
  // 标记评估开始
  localStorage.setItem(evaluatingKey, 'true')

  // 模拟进度增长
  progressTimer = setInterval(() => {
    if (progress.value < 85) {
      progress.value += Math.random() * 2
      updateDimensions()
    }
  }, 200)

  // 轮询获取报告
  let pollingCount = 0
  const maxPolling = 60

  const poll = async () => {
    // 如果已经获取到报告，不再继续轮询
    if (hasReport.value) {
      return
    }
    
    try {
      const report = await interviewApi.getReport(sessionId)

      if (report && report.overallScore !== undefined) {
        // 标记已获取报告
        hasReport.value = true
        
        // 报告已生成，停止进度动画
        if (progressTimer) {
          clearInterval(progressTimer)
          progressTimer = null
        }
        
        progress.value = 100
        currentStatus.value = '分析完成！'

        // 所有维度完成
        dimensions.value.forEach(d => {
          d.progress = 100
          d.completed = true
          d.active = false
        })

        // 保存报告
        localStorage.setItem('interviewReport', JSON.stringify(report))
        localStorage.setItem('currentSessionId', sessionId)
        
        // 清除评估中标记
        localStorage.removeItem(evaluatingKey)

        // 显示成功弹窗
        setTimeout(() => {
          showSuccessModal.value = true
        }, 500)
        return
      }

      pollingCount++
      if (pollingCount < maxPolling) {
        // 更新状态文字
        const statuses = [
          '正在整理面试数据...',
          'AI 分析回答内容中...',
          '评估技术能力维度...',
          '检测逻辑思维水平...',
          '生成综合评价报告...'
        ]
        currentStatus.value = statuses[Math.floor(pollingCount / 12) % statuses.length]

        setTimeout(poll, 1000)
      } else {
        // 超时，停止进度动画并显示弹窗
        if (progressTimer) {
          clearInterval(progressTimer)
          progressTimer = null
        }
        localStorage.removeItem(evaluatingKey)
        showSuccessModal.value = true
      }
    } catch (err) {
      console.error('获取报告失败:', err)
      
      // 如果已经获取到报告，不再处理错误
      if (hasReport.value) {
        return
      }
      
      pollingCount++
      if (pollingCount < maxPolling) {
        setTimeout(poll, 1000)
      } else {
        // 超时，停止进度动画并显示弹窗
        if (progressTimer) {
          clearInterval(progressTimer)
          progressTimer = null
        }
        localStorage.removeItem(evaluatingKey)
        showSuccessModal.value = true
      }
    }
  }

  // 延迟开始轮询，让用户看到动画
  setTimeout(poll, 2000)
}

// 页面关闭前的确认提示
const handleBeforeUnload = (e) => {
  // 如果评估还在进行中，提示用户
  if (progress.value < 100) {
    e.preventDefault()
    e.returnValue = '评估正在后台进行，离开页面后评估仍会继续。确定要离开吗？'
    return e.returnValue
  }
}

onMounted(() => {
  // 添加页面关闭确认
  window.addEventListener('beforeunload', handleBeforeUnload)
  
  rotateTips()
  fetchReport()
})

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
  if (tipTimer) clearInterval(tipTimer)
  
  // 移除页面关闭确认
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.report-loading-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 50%, #16213e 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 粒子背景 */
.particles-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  animation: float-up linear infinite;
}

@keyframes float-up {
  0% {
    transform: translateY(100vh) scale(0);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100px) scale(1);
    opacity: 0;
  }
}

/* 主内容 */
.loading-content {
  text-align: center;
  z-index: 10;
  padding: 40px;
  max-width: 600px;
  width: 100%;
}

/* 中央动画 */
.center-animation {
  position: relative;
  width: 200px;
  height: 200px;
  margin: 0 auto 40px;
}

/* 外圈光环 */
.orbit-ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.ring-1 {
  width: 200px;
  height: 200px;
  border-top-color: #6366f1;
  border-right-color: #6366f1;
  animation: spin 3s linear infinite;
  box-shadow: 0 0 30px rgba(99, 102, 241, 0.3);
}

.ring-2 {
  width: 170px;
  height: 170px;
  border-bottom-color: #8b5cf6;
  border-left-color: #8b5cf6;
  animation: spin-reverse 2.5s linear infinite;
  box-shadow: 0 0 25px rgba(139, 92, 246, 0.3);
}

.ring-3 {
  width: 140px;
  height: 140px;
  border-top-color: #06b6d4;
  border-right-color: #06b6d4;
  animation: spin 2s linear infinite;
  box-shadow: 0 0 20px rgba(6, 182, 212, 0.3);
}

@keyframes spin {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

@keyframes spin-reverse {
  from { transform: translate(-50%, -50%) rotate(360deg); }
  to { transform: translate(-50%, -50%) rotate(0deg); }
}

/* 核心球体 */
.core-sphere {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
}

.sphere-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  animation: pulse-sphere 2s ease-in-out infinite;
}

@keyframes pulse-sphere {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.brain-icon {
  font-size: 48px;
  filter: drop-shadow(0 0 10px rgba(255, 255, 255, 0.5));
}

.sphere-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.4) 0%, transparent 70%);
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% { opacity: 0.5; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.2); }
}

/* 环绕粒子 */
.orbiting-particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.orbit-particle {
  position: absolute;
  font-size: 20px;
  animation: orbit 4s linear infinite;
}

.orbit-particle.p1 { animation-delay: 0s; }
.orbit-particle.p2 { animation-delay: 1s; }
.orbit-particle.p3 { animation-delay: 2s; }
.orbit-particle.p4 { animation-delay: 3s; }

@keyframes orbit {
  0% {
    top: 50%;
    left: 0;
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 0;
  }
  10% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
  25% {
    top: 0;
    left: 50%;
  }
  50% {
    top: 50%;
    left: 100%;
  }
  75% {
    top: 100%;
    left: 50%;
  }
  90% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
  100% {
    top: 50%;
    left: 0;
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 0;
  }
}

/* 标题和状态 */
.status-section {
  margin-bottom: 30px;
}

.main-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  text-shadow: 0 0 30px rgba(99, 102, 241, 0.5);
  background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.status-text {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  animation: fade-pulse 1.5s ease-in-out infinite;
}

@keyframes fade-pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

/* 波浪进度条 */
.wave-progress-container {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 40px;
}

.wave-progress {
  flex: 1;
  height: 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  overflow: hidden;
  position: relative;
}

.wave {
  position: absolute;
  top: 0;
  width: 200%;
  height: 100%;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 50%, #06b6d4 100%);
  border-radius: 6px;
  transition: left 0.3s ease;
}

.wave-1 {
  opacity: 0.8;
  animation: wave-move 2s ease-in-out infinite;
}

.wave-2 {
  opacity: 0.4;
  animation: wave-move 2s ease-in-out infinite reverse;
}

@keyframes wave-move {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(-25px); }
}

.progress-percent {
  font-size: 18px;
  font-weight: 700;
  color: #6366f1;
  min-width: 50px;
}

/* 分析维度卡片 */
.analysis-dimensions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 30px;
}

.dimension-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.dimension-card.active {
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.2);
}

.dimension-card.completed {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
}

.dim-icon {
  font-size: 24px;
  width: 40px;
  text-align: center;
}

.dim-content {
  flex: 1;
  text-align: left;
}

.dim-name {
  display: block;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 6px;
}

.dim-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.dim-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 2px;
  transition: width 0.3s ease;
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.5);
}

.dimension-card.completed .dim-fill {
  background: linear-gradient(90deg, #10b981 0%, #34d399 100%);
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
}

.dim-status {
  width: 24px;
  text-align: center;
}

.dim-status .check {
  color: #10b981;
  font-weight: bold;
}

.dim-status .loading-dot {
  color: #6366f1;
  animation: blink 1s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.dim-status .wait {
  color: rgba(255, 255, 255, 0.3);
}

/* 提示轮播 */
.tips-carousel {
  margin-top: 20px;
}

.tip-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 24px;
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 12px;
  transition: opacity 0.3s ease;
}

.tip-item.fade {
  opacity: 0;
}

.tip-bulb {
  font-size: 20px;
  animation: bulb-glow 2s ease-in-out infinite;
}

@keyframes bulb-glow {
  0%, 100% { filter: drop-shadow(0 0 5px rgba(255, 215, 0, 0.5)); }
  50% { filter: drop-shadow(0 0 15px rgba(255, 215, 0, 0.8)); }
}

.tip-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

/* 底部装饰 */
.bottom-decoration {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100px;
  pointer-events: none;
  overflow: hidden;
}

.glow-line {
  position: absolute;
  bottom: 0;
  left: -100%;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, #6366f1 50%, transparent 100%);
  animation: glow-line-move 3s ease-in-out infinite;
}

.glow-line.delay-1 {
  animation-delay: 1s;
  opacity: 0.5;
}

.glow-line.delay-2 {
  animation-delay: 2s;
  opacity: 0.3;
}

@keyframes glow-line-move {
  0% { left: -100%; }
  100% { left: 100%; }
}

/* 成功弹窗样式 */
.success-modal-overlay {
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
  animation: fade-in 0.3s ease;
}

.success-modal {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  border: 1px solid rgba(99, 102, 241, 0.3);
  border-radius: 24px;
  padding: 48px;
  text-align: center;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5),
              0 0 60px rgba(99, 102, 241, 0.2);
  animation: modal-pop 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.success-icon {
  font-size: 64px;
  margin-bottom: 24px;
  animation: icon-bounce 0.6s ease;
}

.success-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.success-message {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 32px;
  line-height: 1.6;
}

.success-actions {
  display: flex;
  justify-content: center;
}

.btn-primary {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.4);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(99, 102, 241, 0.5);
}

.btn-primary:active {
  transform: translateY(0);
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modal-pop {
  0% {
    opacity: 0;
    transform: scale(0.8) translateY(20px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes icon-bounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}
</style>
