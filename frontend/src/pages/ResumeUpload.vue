<template>
  <div class="resume-upload-page">
    <!-- 标题区域 -->
    <div class="upload-header">
      <h1 class="upload-title">开始您的 AI 模拟面试</h1>
      <p class="upload-subtitle">上传 PDF 或 Word 简历，AI 将为您定制专属面试方案</p>
    </div>

    <!-- 上传区域 -->
    <div 
      class="upload-zone"
      :class="{ 'drag-over': dragOver, 'has-file': selectedFile }"
      @dragover.prevent="handleDragOver"
      @dragleave.prevent="handleDragLeave"
      @drop.prevent="handleDrop"
      @click="triggerFileInput"
    >
      <input 
        type="file" 
        ref="fileInput"
        class="file-input"
        accept=".pdf,.doc,.docx,.txt"
        @change="handleFileChange"
        :disabled="uploading"
      />

      <!-- 渐变边框效果 -->
      <div class="upload-border"></div>

      <div class="upload-content">
        <!-- 已选择文件状态 -->
        <template v-if="selectedFile">
          <div class="file-selected">
            <div class="file-icon-large">
              <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
                <polyline points="10 9 9 9 8 9"/>
              </svg>
            </div>
            <div class="file-info">
              <div class="file-name">{{ selectedFile.name }}</div>
              <div class="file-size">{{ formatFileSize(selectedFile.size) }}</div>
            </div>
            <button 
              class="file-remove"
              @click.stop="removeFile"
              :disabled="uploading"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </template>

        <!-- 未选择文件状态 -->
        <template v-else>
          <div class="upload-icon" :class="{ 'drag-active': dragOver }">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </div>
          <div class="upload-hint">
            <span class="hint-primary">点击或拖拽文件到此处</span>
            <span class="hint-secondary">支持 PDF, DOCX, TXT 格式</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 格式提示 -->
    <div class="format-hints">
      <div class="hint-item">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
        </svg>
        <span>支持 PDF, DOCX, TXT</span>
      </div>
      <div class="hint-item">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <polyline points="12 6 12 12 16 14"/>
        </svg>
        <span>最大 10MB</span>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- 操作按钮 -->
    <div class="upload-actions">
      <button 
        class="btn-upload"
        :class="{ 'uploading': uploading, 'disabled': !selectedFile || uploading }"
        @click="handleUpload"
        :disabled="!selectedFile || uploading"
      >
        <template v-if="uploading">
          <span class="spinner"></span>
          <span>上传分析中...</span>
        </template>
        <template v-else>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <span>开始上传</span>
        </template>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { resumeApi } from '../api/resume.js'

const router = useRouter()
const fileInput = ref(null)
const selectedFile = ref(null)
const dragOver = ref(false)
const uploading = ref(false)
const error = ref('')

// 触发文件选择
const triggerFileInput = () => {
  if (!uploading.value) {
    fileInput.value?.click()
  }
}

// 处理文件选择
const handleFileChange = (e) => {
  const files = e.target.files
  if (files && files.length > 0) {
    validateAndSetFile(files[0])
  }
}

// 拖拽相关
const handleDragOver = () => {
  dragOver.value = true
}

const handleDragLeave = () => {
  dragOver.value = false
}

const handleDrop = (e) => {
  dragOver.value = false
  const files = e.dataTransfer.files
  if (files && files.length > 0) {
    validateAndSetFile(files[0])
  }
}

// 验证并设置文件
const validateAndSetFile = (file) => {
  error.value = ''
  
  // 检查文件类型
  const validTypes = ['application/pdf', 'application/msword', 
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain']
  const validExtensions = ['.pdf', '.doc', '.docx', '.txt']
  const fileExtension = '.' + file.name.split('.').pop().toLowerCase()
  
  if (!validTypes.includes(file.type) && !validExtensions.includes(fileExtension)) {
    error.value = '不支持的文件格式，请上传 PDF, DOCX 或 TXT 文件'
    return
  }
  
  // 检查文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    error.value = '文件大小超过 10MB 限制'
    return
  }
  
  selectedFile.value = file
}

// 移除文件
const removeFile = () => {
  selectedFile.value = null
  error.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 上传文件
const handleUpload = async () => {
  if (!selectedFile.value || uploading.value) return
  
  uploading.value = true
  error.value = ''
  
  try {
    const data = await resumeApi.uploadAndAnalyze(selectedFile.value)
    
    // 检查上传是否成功
    if (!data.storage || !data.storage.resumeId) {
      throw new Error('上传失败，请重试')
    }
    
    // 上传成功，跳转到简历分析页面
    router.push(`/app/resume-analysis/${data.storage.resumeId}`)
  } catch (err) {
    error.value = err.message || '上传失败，请重试'
    uploading.value = false
  }
}
</script>

<style scoped>
.resume-upload-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.upload-header {
  text-align: center;
  margin-bottom: 48px;
}

.upload-title {
  font-size: 36px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 12px;
  letter-spacing: -0.02em;
}

.upload-subtitle {
  font-size: 18px;
  color: #64748b;
  font-weight: 400;
}

.upload-zone {
  position: relative;
  width: 100%;
  max-width: 600px;
  background: white;
  border-radius: 20px;
  padding: 60px 40px;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
}

.upload-border {
  position: absolute;
  inset: 0;
  border-radius: 20px;
  padding: 2px;
  background: linear-gradient(135deg, #c7d2fe 0%, #e9d5ff 50%, #c7d2fe 100%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  transition: all 0.3s ease;
}

.upload-zone.drag-over .upload-border {
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 50%, #6366f1 100%);
}

.upload-zone:hover .upload-border {
  background: linear-gradient(135deg, #818cf8 0%, #c084fc 50%, #818cf8 100%);
}

.file-input {
  display: none;
}

.upload-content {
  position: relative;
  z-index: 1;
}

.upload-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: #f1f5f9;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  transition: all 0.3s ease;
}

.upload-icon.drag-active {
  background: #e0e7ff;
  color: #6366f1;
  transform: translateY(-5px);
}

.upload-hint {
  text-align: center;
}

.hint-primary {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.hint-secondary {
  display: block;
  font-size: 14px;
  color: #94a3b8;
}

/* 已选择文件样式 */
.file-selected {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.file-icon-large {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #e0e7ff 0%, #f3e8ff 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366f1;
  flex-shrink: 0;
}

.file-info {
  text-align: left;
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.file-size {
  font-size: 14px;
  color: #64748b;
}

.file-remove {
  width: 36px;
  height: 36px;
  background: #fee2e2;
  border: none;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.file-remove:hover {
  background: #fecaca;
}

.file-remove:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 格式提示 */
.format-hints {
  display: flex;
  gap: 24px;
  margin-top: 24px;
  justify-content: center;
}

.hint-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.hint-item svg {
  color: #94a3b8;
}

/* 错误提示 */
.error-message {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  padding: 12px 20px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
  color: #dc2626;
  font-size: 14px;
}

.error-message svg {
  flex-shrink: 0;
}

/* 上传按钮 */
.upload-actions {
  margin-top: 32px;
}

.btn-upload {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 32px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
}

.btn-upload:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

.btn-upload.disabled {
  background: #cbd5e1;
  box-shadow: none;
  cursor: not-allowed;
}

.btn-upload.uploading {
  background: linear-gradient(135deg, #818cf8 0%, #a78bfa 100%);
  cursor: wait;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 640px) {
  .upload-title {
    font-size: 28px;
  }
  
  .upload-subtitle {
    font-size: 16px;
  }
  
  .upload-zone {
    padding: 40px 24px;
  }
  
  .file-selected {
    flex-direction: column;
    text-align: center;
  }
  
  .file-info {
    text-align: center;
  }
  
  .format-hints {
    flex-direction: column;
    gap: 12px;
    align-items: center;
  }
}
</style>
