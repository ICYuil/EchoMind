<template>
  <div class="interview-settings-page">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="goBack">
      <span class="back-icon">←</span>
      <span>返回</span>
    </button>

    <div class="page-header">
      <h1 class="page-title">面试设置</h1>
      <p class="page-subtitle">配置您的 AI 模拟面试参数</p>
    </div>

    <!-- 已选简历和知识库信息 -->
    <div class="selected-files-section">
      <!-- 已选简历 -->
      <div v-if="selectedResume" class="selected-resume">
        <div class="section-title">已选简历</div>
        <div class="resume-info-card">
          <div class="resume-icon">📄</div>
          <div class="resume-details">
            <div class="resume-name">{{ selectedResume.filename }}</div>
            <div class="resume-meta">
              <span>{{ formatDate(selectedResume.uploadedAt) }}</span>
              <span>{{ formatFileSize(selectedResume.fileSize) }}</span>
            </div>
          </div>
          <button class="change-btn" @click="changeResume">更换</button>
        </div>
      </div>

      <!-- 已选知识库 -->
      <div v-if="selectedKnowledgeBases.length > 0" class="selected-knowledge">
        <div class="section-title">
          已选知识库
          <span class="kb-count">{{ selectedKnowledgeBases.length }} 个</span>
        </div>
        <div class="knowledge-list">
          <div 
            v-for="kb in selectedKnowledgeBases" 
            :key="kb.id" 
            class="knowledge-item"
            :class="{ active: currentKbId === kb.id }"
            @click="currentKbId = kb.id"
          >
            <span class="kb-icon">📚</span>
            <span class="kb-name">{{ kb.name || kb.filename || '未命名知识库' }}</span>
            <span v-if="currentKbId === kb.id" class="kb-active">✓</span>
          </div>
        </div>
        <button class="change-btn kb-change" @click="changeKnowledgeBase">更换知识库</button>
      </div>
    </div>

    <!-- 面试设置表单 -->
    <div class="settings-form">
      <div class="section-title">面试配置</div>
      
      <!-- 岗位类型 -->
      <div class="form-item">
        <label class="form-label">
          <span class="label-icon">💼</span>
          岗位类型
        </label>
        <div class="job-type-grid">
          <button
            v-for="type in jobTypes"
            :key="type.value"
            class="job-type-btn"
            :class="{ active: settings.jobId === type.value }"
            @click="settings.jobId = type.value"
          >
            <span class="job-icon">{{ type.icon }}</span>
            <span class="job-name">{{ type.label }}</span>
          </button>
        </div>
      </div>

      <!-- 面试时长/题数 -->
      <div class="form-item">
        <label class="form-label">
          <span class="label-icon">📝</span>
          题目数量
          <span class="question-count">{{ settings.questionCount }} 题</span>
        </label>
        <div class="slider-container">
          <input
            v-model.number="settings.questionCount"
            type="range"
            min="3"
            max="10"
            step="1"
            class="slider"
          />
          <div class="slider-labels">
            <span>3题</span>
            <span>10题</span>
          </div>
        </div>
        <p class="form-hint">建议 5-8 题，每题预计 3-5 分钟</p>
      </div>

      <!-- 面试房间类型 -->
      <div class="form-item">
        <label class="form-label">
          <span class="label-icon">🏠</span>
          面试房间类型
        </label>
        <div class="room-type-grid">
          <button
            v-for="type in roomTypes"
            :key="type.value"
            class="room-type-btn"
            :class="{ active: settings.roomType === type.value }"
            @click="settings.roomType = type.value"
          >
            <span class="room-icon">{{ type.icon }}</span>
            <span class="room-name">{{ type.label }}</span>
            <span class="room-desc">{{ type.desc }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <button class="btn secondary" @click="goBack">上一步</button>
      <button class="btn primary" @click="startInterview">
        <span class="btn-icon">🚀</span>
        开始面试
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 已选简历
const selectedResume = ref(null)
// 已选知识库
const selectedKnowledgeBases = ref([])
// 当前选中的知识库ID
const currentKbId = ref(null)

// 面试设置（仅保留后端支持的参数）
// jobId: 0=前端, 1=后端, 2=测试
const settings = ref({
  jobId: 0,
  questionCount: 5,
  roomType: 'normal' // normal 或 realistic
})

// 岗位类型选项（与后端 JobConstants 对应）
const jobTypes = [
  { value: 0, label: '前端开发', icon: '💻' },
  { value: 1, label: '后端开发', icon: '⚙️' },
  { value: 2, label: '测试工程师', icon: '🧪' }
]

// 面试房间类型选项
const roomTypes = [
  { value: 'normal', label: '普通面试房间', icon: '💬', desc: '支持文字和语音，有聊天界面' },
  { value: 'realistic', label: '真实模拟房间', icon: '🎭', desc: '纯语音交互，更真实的面试体验' }
]

// 加载已选简历和知识库
onMounted(() => {
  const savedResume = localStorage.getItem('selectedResume')
  if (savedResume) {
    try {
      selectedResume.value = JSON.parse(savedResume)
    } catch (e) {
      console.error('解析已选简历失败:', e)
    }
  }
  
  const savedKnowledgeBases = localStorage.getItem('selectedKnowledgeBases')
  if (savedKnowledgeBases) {
    try {
      const parsed = JSON.parse(savedKnowledgeBases)
      // 兼容处理：可能是ID数组[1,2,3]或对象数组[{id:1},{id:2}]
      if (parsed.length > 0 && typeof parsed[0] === 'number') {
        // ID数组，需要转换为对象数组
        selectedKnowledgeBases.value = parsed.map(id => ({ id }))
      } else {
        // 对象数组
        selectedKnowledgeBases.value = parsed
      }
      // 默认选中第一个知识库
      if (selectedKnowledgeBases.value.length > 0) {
        currentKbId.value = selectedKnowledgeBases.value[0].id
      }
    } catch (e) {
      console.error('解析已选知识库失败:', e)
    }
  }

  // 如果没有选中的简历，返回选择页面
  if (!selectedResume.value) {
    router.replace('/app/home/interview/resume-select')
  }

  // 如果没有选中的知识库，返回知识库选择页面
  if (selectedKnowledgeBases.value.length === 0) {
    router.replace('/app/home/interview/knowledgebase-select')
  }
})

// 返回上一页
const goBack = () => {
  router.push('/app/home/interview/knowledgebase-select')
}

// 更换简历
const changeResume = () => {
  router.push('/app/home/interview/select-resume')
}

// 更换知识库
const changeKnowledgeBase = () => {
  router.push('/app/home/interview/knowledgebase-select')
}

// 开始面试
const startInterview = () => {
  // 保存面试设置
  localStorage.setItem('interviewSettings', JSON.stringify({
    ...settings.value,
    resumeId: selectedResume.value.id,
    knowledgeBaseIds: selectedKnowledgeBases.value.map(kb => kb.id),
    currentKnowledgeBaseId: currentKbId.value
  }))

  // 跳转到加载页面，创建面试会话
  // 房间类型会在 InterviewLoading.vue 中读取并决定跳转到哪个房间
  router.push('/app/home/interview/loading')
}

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}
</script>

<style scoped>
.interview-settings-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px;
  background: var(--bg0);
  position: relative;
  overflow-y: auto;
}

/* 返回按钮 */
.back-btn {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 10;
}

.back-btn:hover {
  background: var(--panel2);
  color: var(--brand);
  transform: translateX(-2px);
}

.back-icon {
  font-size: 16px;
  line-height: 1;
}

/* 页面头部 */
.page-header {
  text-align: center;
  margin-bottom: 24px;
  margin-top: 40px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 8px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--muted);
  margin: 0;
}

/* 区块标题 */
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--muted);
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 已选简历 */
.selected-resume {
  margin-bottom: 24px;
}

.resume-info-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--panel-hover);
  border: 1px solid var(--stroke);
  border-radius: 16px;
}

.resume-icon {
  font-size: 32px;
}

.resume-details {
  flex: 1;
  min-width: 0;
}

.resume-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.resume-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--muted);
}

.change-btn {
  padding: 8px 16px;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 8px;
  font-size: 13px;
  color: var(--text);
  cursor: pointer;
  transition: all 0.2s ease;
}

.change-btn:hover {
  border-color: var(--brand);
  background: var(--panel2);
}

/* 已选知识库 */
.selected-knowledge {
  margin-bottom: 24px;
}

.selected-knowledge .section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kb-count {
  font-size: 12px;
  color: var(--muted);
  font-weight: 500;
  text-transform: none;
  letter-spacing: 0;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.knowledge-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--panel);
  border: 2px solid var(--stroke);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.knowledge-item:hover {
  border-color: var(--brand);
  background: var(--panel-hover);
}

.knowledge-item.active {
  border-color: var(--brand);
  background: var(--panel-hover);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
}

.kb-icon {
  font-size: 24px;
}

.kb-name {
  flex: 1;
  font-size: 14px;
  color: var(--text);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-active {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand);
  color: white;
  border-radius: 50%;
  font-size: 12px;
}

.change-btn.kb-change {
  width: 100%;
}

/* 设置表单 */
.settings-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.label-icon {
  font-size: 18px;
}

.question-count {
  margin-left: auto;
  padding: 4px 12px;
  background: var(--brand);
  color: white;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.form-hint {
  font-size: 12px;
  color: var(--muted);
  margin: 0;
}

/* 岗位类型网格 */
.job-type-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.job-type-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: var(--panel);
  border: 2px solid var(--stroke);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.job-type-btn:hover {
  border-color: var(--brand);
  transform: translateY(-2px);
}

.job-type-btn.active {
  border-color: var(--brand);
  background: var(--panel-hover);
  box-shadow: 0 8px 24px var(--shadow-hover);
}

.job-icon {
  font-size: 24px;
}

.job-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
}

/* 房间类型网格 */
.room-type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.room-type-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  background: var(--panel);
  border: 2px solid var(--stroke);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.room-type-btn:hover {
  border-color: var(--brand);
  transform: translateY(-2px);
}

.room-type-btn.active {
  border-color: var(--brand);
  background: var(--panel-hover);
  box-shadow: 0 8px 24px var(--shadow-hover);
}

.room-icon {
  font-size: 32px;
}

.room-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.room-desc {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.4;
}

/* 滑块 */
.slider-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.slider {
  width: 100%;
  height: 8px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--panel);
  border-radius: 4px;
  outline: none;
}

.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 24px;
  height: 24px;
  background: var(--brand);
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 4px 12px var(--shadow);
  transition: all 0.2s ease;
}

.slider::-webkit-slider-thumb:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px var(--shadow-hover);
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--muted);
}

/* 底部操作栏 */
.action-bar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid var(--stroke);
  margin-top: 24px;
}

/* 按钮样式 */
.btn {
  padding: 14px 32px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn.primary {
  background: var(--brand);
  color: white;
  box-shadow: 0 4px 16px var(--shadow);
}

.btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px var(--shadow-hover);
}

.btn.secondary {
  background: var(--panel);
  color: var(--text);
  border: 1px solid var(--stroke);
}

.btn.secondary:hover {
  background: var(--panel2);
  border-color: var(--brand);
}

.btn-icon {
  font-size: 18px;
}

/* 滚动条样式 */
.interview-settings-page::-webkit-scrollbar {
  width: 6px;
}

.interview-settings-page::-webkit-scrollbar-track {
  background: transparent;
}

.interview-settings-page::-webkit-scrollbar-thumb {
  background: var(--stroke);
  border-radius: 3px;
}

.interview-settings-page::-webkit-scrollbar-thumb:hover {
  background: var(--muted);
}
</style>
