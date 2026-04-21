<template>
  <div style="padding:20px;">
    <div class="upload-area" 
         :class="{ dragover: isDragOver, uploading: uploading }"
         @dragover.prevent="isDragOver = true"
         @dragleave.prevent="isDragOver = false"
         @drop.prevent="handleDrop"
    >
      <input 
        ref="fileInput"
        type="file" 
        style="display:none"
        accept=".pdf,.doc,.docx,.txt,.md"
        @change="handleFileSelect"
      />
      
      <div v-if="!uploading && !currentFile" class="upload-placeholder" @click="selectFile">
        <UploadCloudIcon class="icon" style="width:48px; height:48px; opacity:0.5;" />
        <div style="font-weight:900; margin-top:12px;">点击或拖拽上传文档</div>
        <div class="muted" style="margin-top:8px;">
          支持 PDF、DOCX、DOC、TXT、MD 格式，最大 50MB
        </div>
      </div>

      <div v-else-if="currentFile && !uploading" class="file-preview">
        <FileTextIcon class="icon" style="width:40px; height:40px;" />
        <div style="margin-top:8px;">
          <div style="font-weight:900;">{{ currentFile.name }}</div>
          <div class="muted" style="font-size:12px;">{{ formatFileSize(currentFile.size) }}</div>
        </div>
      </div>

      <div v-else-if="uploading" class="uploading-state">
        <Loader2Icon class="icon spin" style="width:32px; height:32px;" />
        <div style="margin-top:12px;">上传中...</div>
        <div class="muted" style="font-size:12px; margin-top:4px;">正在处理文档，请稍候</div>
      </div>
    </div>

    <div v-if="currentFile && !uploading" style="margin-top:16px;">
      <div class="form-row">
        <label>文档名称（可选）</label>
        <input 
          v-model="fileName" 
          placeholder="留空则使用文件名"
          class="input"
        />
      </div>
      <div class="form-row" style="margin-top:12px;">
        <label>分类（可选）</label>
        <input 
          v-model="category" 
          placeholder="例如：技术文档、产品手册"
          class="input"
        />
      </div>
    </div>

    <div v-if="error" class="error-message" style="margin-top:12px;">
      <AlertCircleIcon class="icon" /> {{ error }}
    </div>

    <div class="row gap10" style="margin-top:20px; justify-content:flex-end;">
      <button class="btn" @click="cancel" :disabled="uploading">取消</button>
      <button 
        class="btn primary" 
        @click="upload"
        :disabled="!currentFile || uploading"
      >
        {{ uploading ? '上传中...' : '开始上传' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { knowledgeBaseApi } from '../api/knowledgebase'
import {
  UploadCloudIcon,
  FileTextIcon,
  Loader2Icon,
  AlertCircleIcon,
} from 'lucide-vue-next'

const emit = defineEmits(['upload-complete', 'cancel'])

const fileInput = ref(null)
const isDragOver = ref(false)
const uploading = ref(false)
const currentFile = ref(null)
const fileName = ref('')
const category = ref('')
const error = ref('')

const selectFile = () => {
  fileInput.value?.click()
}

const handleFileSelect = (e) => {
  const file = e.target.files?.[0]
  if (file) {
    validateAndSetFile(file)
  }
}

const handleDrop = (e) => {
  isDragOver.value = false
  const file = e.dataTransfer.files?.[0]
  if (file) {
    validateAndSetFile(file)
  }
}

const validateAndSetFile = (file) => {
  error.value = ''
  
  // 检查文件大小（最大50MB）
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    error.value = '文件大小不能超过50MB'
    return
  }
  
  // 检查文件类型
  const allowedTypes = ['.pdf', '.docx', '.doc', '.txt', '.md']
  const fileName = file.name.toLowerCase()
  const isValidType = allowedTypes.some(type => fileName.endsWith(type))
  if (!isValidType) {
    error.value = `不支持的文件格式，请上传：${allowedTypes.join('、')}`
    return
  }
  
  currentFile.value = file
}

const upload = async () => {
  if (!currentFile.value) return
  
  uploading.value = true
  error.value = ''
  
  try {
    const data = await knowledgeBaseApi.uploadKnowledgeBase(
      currentFile.value,
      fileName.value || undefined,
      category.value || undefined
    )
    emit('upload-complete', data)
    reset()
  } catch (err) {
    console.error('上传失败:', err)
    error.value = err.message || '上传失败，请重试'
  } finally {
    uploading.value = false
  }
}

const cancel = () => {
  if (!uploading.value) {
    reset()
    emit('cancel')
  }
}

const reset = () => {
  currentFile.value = null
  fileName.value = ''
  category.value = ''
  error.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}
</script>

<style scoped>
.upload-area {
  border: 2px dashed var(--stroke);
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-area:hover,
.upload-area.dragover {
  border-color: var(--primary);
  background: var(--bg-soft);
}

.upload-area.uploading {
  cursor: default;
  border-color: var(--stroke);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.file-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.uploading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}

.input {
  padding: 10px 12px;
  border: 1px solid var(--stroke);
  border-radius: 8px;
  background: var(--bg);
  color: var(--text);
  font-size: 14px;
}

.input:focus {
  outline: none;
  border-color: var(--primary);
}

.error-message {
  padding: 10px 12px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid var(--danger);
  border-radius: 8px;
  color: var(--danger);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
