<template>
  <div class="resume-select-page">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="goBack">
      <span class="back-icon">←</span>
      <span>返回</span>
    </button>

    <div class="page-header">
      <h1 class="page-title">选择简历</h1>
      <p class="page-subtitle">选择一份简历开始您的 AI 模拟面试</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载简历列表...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="resumes.length === 0" class="empty-state">
      <div class="empty-icon">📄</div>
      <h3>暂无简历</h3>
      <p>请先上传简历，再开始面试</p>
      <button class="btn primary" @click="showUploadModal = true">
        上传简历
      </button>
    </div>

    <!-- 简历列表 -->
    <div v-else class="resume-list">
      <div
        v-for="resume in resumes"
        :key="resume.id"
        class="resume-card"
        :class="{ selected: selectedResume?.id === resume.id }"
        @click="selectResume(resume)"
      >
        <div class="resume-icon">📄</div>
        <div class="resume-info">
          <div class="resume-name">{{ resume.filename || resume.name }}</div>
          <div class="resume-meta">
            <span class="resume-date">{{ formatDate(resume.uploadedAt) }}</span>
            <span class="resume-size">{{ formatFileSize(resume.fileSize) }}</span>
          </div>
        </div>
        <div class="resume-check">
          <div v-if="selectedResume?.id === resume.id" class="check-icon">✓</div>
        </div>
      </div>
      
      <!-- 添加新简历卡片 -->
      <div class="resume-card add-new" @click="showUploadModal = true">
        <div class="resume-icon add-icon">+</div>
        <div class="resume-info">
          <div class="resume-name">上传新简历</div>
          <div class="resume-meta">
            <span>支持 PDF、DOC、DOCX 格式</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div v-if="resumes.length > 0" class="action-bar">
      <button class="btn secondary" @click="goBack">取消</button>
      <button
        class="btn primary"
        :disabled="!selectedResume"
        @click="goToKnowledgeBase"
      >
        下一步：选择知识库
      </button>
    </div>

    <!-- 上传简历弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showUploadModal" class="modal-overlay" @click="showUploadModal = false">
          <div class="modal-content" @click.stop>
            <div class="modal-header">
              <h3>上传简历</h3>
              <button class="modal-close" @click="showUploadModal = false">×</button>
            </div>
            <div class="modal-body">
              <div 
                class="upload-dropzone"
                :class="{ 'drag-over': dragOver, 'uploading': uploading }"
                @dragenter.prevent="dragOver = true"
                @dragleave.prevent="dragOver = false"
                @dragover.prevent
                @drop.prevent="handleDrop"
                @click="!uploading && $refs.fileInput?.click()"
              >
                <input 
                  ref="fileInput"
                  type="file" 
                  accept=".pdf,.doc,.docx"
                  @change="handleFileSelect"
                  style="display: none"
                />
                <div v-if="!uploading" class="upload-content">
                  <svg class="upload-icon-large" viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17,8 12,3 7,8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                  <p class="upload-text">点击或拖拽文件到此处</p>
                  <p class="upload-hint">支持 PDF、DOC、DOCX 格式，最大 10MB</p>
                </div>
                <div v-else class="upload-progress">
                  <div class="progress-ring">
                    <svg viewBox="0 0 36 36" class="progress-circle">
                      <path class="progress-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                      <path class="progress-fill" :stroke-dasharray="uploadProgress + ', 100'" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                    </svg>
                    <span class="progress-text">{{ Math.round(uploadProgress) }}%</span>
                  </div>
                  <p class="progress-hint">正在上传并分析...</p>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn secondary" @click="showUploadModal = false" :disabled="uploading">取消</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 成功提示弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showSuccessModal" class="modal-overlay" @click="showSuccessModal = false">
          <div class="modal-content" @click.stop>
            <div class="modal-header success">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22,4 12,14.01 9,11.01"/>
              </svg>
              <h3>上传成功</h3>
            </div>
            <div class="modal-body">
              <div class="success-content">
                <p>上传简历并分析成功，你可以在简历库中找到相应简历进行查看</p>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn primary" @click="showSuccessModal = false">确定</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { resumeApi } from '../../api/resume.js'

const router = useRouter()

// 状态
const loading = ref(true)
const resumes = ref([])
const selectedResume = ref(null)

// 上传弹窗状态
const showUploadModal = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const dragOver = ref(false)
const fileInput = ref(null)

// 成功提示弹窗
const showSuccessModal = ref(false)

// 加载简历列表
const loadResumes = async () => {
  loading.value = true
  try {
    const data = await resumeApi.getResumes()
    resumes.value = data || []
  } catch (err) {
    console.error('加载简历列表失败:', err)
    alert('加载简历列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 选择简历
const selectResume = (resume) => {
  selectedResume.value = resume
}

// 返回上一页
const goBack = () => {
  router.push('/app/home')
}

// 处理文件选择
const handleFileSelect = (e) => {
  const file = e.target.files[0]
  if (file) {
    uploadResume(file)
  }
}

// 处理拖拽上传
const handleDrop = (e) => {
  dragOver.value = false
  const file = e.dataTransfer.files[0]
  if (file) {
    uploadResume(file)
  }
}

// 上传简历
const uploadResume = async (file) => {
  // 验证文件类型
  const validExtensions = ['.pdf', '.doc', '.docx']
  const fileExtension = '.' + file.name.split('.').pop().toLowerCase()
  
  if (!validExtensions.includes(fileExtension)) {
    alert('不支持的文件格式，请上传 PDF、DOC 或 DOCX 文件')
    return
  }
  
  // 验证文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    alert('文件大小超过 10MB 限制')
    return
  }
  
  uploading.value = true
  uploadProgress.value = 0
  
  // 模拟上传进度
  const progressInterval = setInterval(() => {
    if (uploadProgress.value < 90) {
      uploadProgress.value += Math.random() * 15
    }
  }, 200)
  
  try {
    console.log('开始上传简历:', file.name)
    const result = await resumeApi.uploadAndAnalyze(file)
    clearInterval(progressInterval)
    uploadProgress.value = 100
    
    console.log('上传成功，返回结果:', result)
    
    // 上传完成后刷新列表并显示成功提示
    setTimeout(() => {
      uploading.value = false
      showUploadModal.value = false
      console.log('刷新简历列表...')
      loadResumes()
      // 如果有返回的简历信息，自动选中
      if (result && result.id) {
        console.log('自动选中新上传的简历:', result.id)
        selectedResume.value = result
      }
      // 显示成功提示弹窗
      showSuccessModal.value = true
    }, 500)
  } catch (err) {
    clearInterval(progressInterval)
    uploading.value = false
    console.error('上传失败:', err)
    alert('上传失败，请稍后重试：' + (err.message || '未知错误'))
  }
}

// 前往知识库选择
const goToKnowledgeBase = () => {
  if (!selectedResume.value) return

  // 将选中的简历信息存储到 localStorage
  localStorage.setItem('selectedResume', JSON.stringify({
    id: selectedResume.value.id,
    filename: selectedResume.value.filename || selectedResume.value.name,
    fileSize: selectedResume.value.fileSize,
    uploadedAt: selectedResume.value.uploadedAt
  }))

  router.push('/app/home/interview/knowledgebase-select')
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

onMounted(() => {
  loadResumes()
})
</script>

<style scoped>
.resume-select-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px;
  background: var(--bg0);
  position: relative;
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

/* 加载状态 */
.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--muted);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--stroke);
  border-top-color: var(--brand);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--muted);
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* 简历列表 */
.resume-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 8px;
}

.resume-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--panel);
  border: 2px solid var(--stroke);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.resume-card:hover {
  border-color: var(--brand);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px var(--shadow);
}

.resume-card.selected {
  border-color: var(--brand);
  background: var(--panel-hover);
  box-shadow: 0 8px 24px var(--shadow-hover);
}

.resume-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.resume-info {
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

.resume-check {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.check-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand);
  color: white;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 600;
}

/* 底部操作栏 */
.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--stroke);
  margin-top: 16px;
}

/* 按钮样式 */
.btn {
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
}

.btn.primary {
  background: var(--brand);
  color: white;
}

.btn.primary:hover:not(:disabled) {
  background: var(--brand-hover);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px var(--shadow-hover);
}

.btn.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

/* 滚动条样式 */
.resume-list::-webkit-scrollbar {
  width: 6px;
}

.resume-list::-webkit-scrollbar-track {
  background: transparent;
}

.resume-list::-webkit-scrollbar-thumb {
  background: var(--stroke);
  border-radius: 3px;
}

.resume-list::-webkit-scrollbar-thumb:hover {
  background: var(--muted);
}

/* 添加新简历卡片 */
.resume-card.add-new {
  border: 2px dashed var(--stroke);
  background: transparent;
}

.resume-card.add-new:hover {
  border-color: var(--brand);
  background: rgba(100, 108, 255, 0.05);
}

.resume-icon.add-icon {
  font-size: 32px;
  font-weight: 300;
  color: var(--brand);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: var(--bg0);
  border-radius: 16px;
  border: 1px solid var(--stroke);
  width: 100%;
  max-width: 480px;
  overflow: hidden;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--stroke);
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.modal-header.success {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  border-bottom: 1px solid rgba(16, 185, 129, 0.2);
}

.modal-header.success h3 {
  color: #10b981;
}

.success-content {
  text-align: center;
  padding: 20px 0;
}

.success-content p {
  font-size: 15px;
  color: var(--text);
  line-height: 1.5;
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--muted);
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: var(--panel);
  color: var(--text);
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--stroke);
}

/* 上传拖放区域 */
.upload-dropzone {
  border: 2px dashed var(--stroke);
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-dropzone:hover:not(.uploading) {
  border-color: var(--brand);
  background: rgba(100, 108, 255, 0.05);
}

.upload-dropzone.drag-over {
  border-color: var(--brand);
  background: rgba(100, 108, 255, 0.1);
}

.upload-dropzone.uploading {
  cursor: default;
  border-color: var(--stroke);
}

.upload-icon-large {
  color: var(--brand);
  margin-bottom: 16px;
}

.upload-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 13px;
  color: var(--muted);
}

/* 上传进度 */
.upload-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.progress-ring {
  position: relative;
  width: 80px;
  height: 80px;
}

.progress-circle {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.progress-bg {
  fill: none;
  stroke: var(--stroke);
  stroke-width: 3;
}

.progress-fill {
  fill: none;
  stroke: var(--brand);
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dasharray 0.3s ease;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
}

.progress-hint {
  font-size: 14px;
  color: var(--muted);
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
