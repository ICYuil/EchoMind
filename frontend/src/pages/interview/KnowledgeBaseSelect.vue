<template>
  <div class="knowledgebase-select-page">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="goBack">
      <span class="back-icon">←</span>
      <span>返回</span>
    </button>

    <div class="page-header">
      <h1 class="page-title">选择知识库</h1>
      <p class="page-subtitle">选择一个或多个知识库，AI将基于这些知识提问</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载知识库列表...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="knowledgeBases.length === 0" class="empty-state">
      <div class="empty-icon">📚</div>
      <h3>暂无知识库</h3>
      <p>请先上传知识库文档，再开始面试</p>
      <button class="btn primary" @click="showUploadModal = true">
        上传知识库
      </button>
    </div>

    <!-- 知识库列表 -->
    <div v-else class="knowledgebase-list">
      <div
        v-for="kb in knowledgeBases"
        :key="kb.id"
        class="knowledgebase-card"
        :class="{ selected: isSelected(kb.id) }"
        @click="toggleSelect(kb.id)"
      >
        <div class="knowledgebase-icon">📚</div>
        <div class="knowledgebase-info">
          <div class="knowledgebase-name">{{ kb.name }}</div>
          <div class="knowledgebase-meta">
            <span class="knowledgebase-date">{{ formatDate(kb.createdAt) }}</span>
            <span class="knowledgebase-size">{{ kb.fileSize ? formatFileSize(kb.fileSize) : '0 B' }}</span>
            <span class="knowledgebase-status" :class="getStatusClass(kb.vectorStatus)">
              {{ getStatusText(kb.vectorStatus) }}
            </span>
          </div>
        </div>
        <div class="knowledgebase-check">
          <div v-if="isSelected(kb.id)" class="check-icon">✓</div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div v-if="knowledgeBases.length > 0" class="action-bar">
      <button class="btn secondary" @click="goBack">取消</button>
      <button
        class="btn primary"
        :disabled="selectedKnowledgeBases.length === 0"
        @click="goToSettings"
      >
        下一步：面试设置
      </button>
    </div>

    <!-- 上传知识库弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showUploadModal" class="modal-overlay" @click="showUploadModal = false">
          <div class="modal-content" @click.stop>
            <div class="modal-header">
              <h3>上传知识库文档</h3>
              <button class="modal-close" @click="showUploadModal = false">×</button>
            </div>
            <div class="modal-body">
              <div class="upload-form">
                <div class="form-item">
                  <label>选择文件</label>
                  <div class="file-input-wrapper">
                    <input
                      ref="fileInput"
                      type="file"
                      accept=".pdf,.doc,.docx,.txt,.md"
                      @change="handleFileSelect"
                      style="display: none"
                    />
                    <button class="btn secondary" @click="$refs.fileInput.click()">
                      {{ uploadFile ? uploadFile.name : '选择文件' }}
                    </button>
                  </div>
                  <p class="form-hint">支持 PDF、DOC、DOCX、TXT、MD 格式</p>
                </div>

                <div class="form-item">
                  <label>文档名称</label>
                  <input
                    v-model="uploadName"
                    type="text"
                    placeholder="输入文档名称"
                    class="text-input"
                  />
                </div>

                <div class="form-item">
                  <label>分类（可选）</label>
                  <input
                    v-model="uploadCategory"
                    type="text"
                    placeholder="输入分类，如：技术、产品等"
                    class="text-input"
                  />
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn secondary" @click="showUploadModal = false">取消</button>
              <button
                class="btn primary"
                @click="uploadKnowledge"
                :disabled="!uploadFile || uploading"
              >
                {{ uploading ? '上传中...' : '上传' }}
              </button>
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
import { knowledgeBaseApi } from '../../api/knowledgebase.js'

const router = useRouter()

// 状态
const loading = ref(true)
const knowledgeBases = ref([])
const selectedKnowledgeBases = ref([])

// 上传弹窗状态
const showUploadModal = ref(false)
const uploadFile = ref(null)
const uploadName = ref('')
const uploadCategory = ref('')
const uploading = ref(false)

// 加载知识库列表
const loadKnowledgeBases = async () => {
  loading.value = true
  try {
    const data = await knowledgeBaseApi.getAllKnowledgeBases()
    knowledgeBases.value = data || []
  } catch (err) {
    console.error('加载知识库列表失败:', err)
    alert('加载知识库列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 检查是否选中
const isSelected = (id) => {
  return selectedKnowledgeBases.value.includes(id)
}

// 切换选择状态
const toggleSelect = (id) => {
  const index = selectedKnowledgeBases.value.indexOf(id)
  if (index > -1) {
    selectedKnowledgeBases.value.splice(index, 1)
  } else {
    selectedKnowledgeBases.value.push(id)
  }
}

// 返回上一页
const goBack = () => {
  router.push('/app/home/interview/resume-select')
}

// 处理文件选择
const handleFileSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    uploadFile.value = file
    uploadName.value = file.name.replace(/\.[^/.]+$/, '')
  }
}

// 上传知识库
const uploadKnowledge = async () => {
  if (!uploadFile.value) return

  uploading.value = true
  try {
    await knowledgeBaseApi.uploadKnowledgeBase(
      uploadFile.value,
      uploadName.value || uploadFile.value.name,
      uploadCategory.value
    )
    showUploadModal.value = false
    uploadFile.value = null
    uploadName.value = ''
    uploadCategory.value = ''
    // 重新加载知识库列表
    await loadKnowledgeBases()
  } catch (err) {
    console.error('上传知识库失败:', err)
    alert('上传失败: ' + (err.message || '请稍后重试'))
  } finally {
    uploading.value = false
  }
}

// 前往面试设置
const goToSettings = () => {
  if (selectedKnowledgeBases.value.length === 0) return
  
  // 将选中的知识库ID存储到 localStorage
  localStorage.setItem('selectedKnowledgeBases', JSON.stringify(selectedKnowledgeBases.value))
  
  router.push('/app/home/interview/settings')
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

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return statusMap[status] || '未知'
}

// 获取状态样式类
const getStatusClass = (status) => {
  const classMap = {
    PENDING: 'status-pending',
    PROCESSING: 'status-processing',
    COMPLETED: 'status-completed',
    FAILED: 'status-failed'
  }
  return classMap[status] || ''
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped>
.knowledgebase-select-page {
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

/* 知识库列表 */
.knowledgebase-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 8px;
}

.knowledgebase-card {
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

.knowledgebase-card:hover {
  border-color: var(--brand);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px var(--shadow);
}

.knowledgebase-card.selected {
  border-color: var(--brand);
  background: var(--panel-hover);
  box-shadow: 0 8px 24px var(--shadow-hover);
}

.knowledgebase-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.knowledgebase-info {
  flex: 1;
  min-width: 0;
}

.knowledgebase-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.knowledgebase-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--muted);
  flex-wrap: wrap;
}

.knowledgebase-status {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.status-pending {
  background: rgba(251, 191, 36, 0.1);
  color: #f59e0b;
}

.status-processing {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.status-completed {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.status-failed {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.knowledgebase-check {
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
.knowledgebase-list::-webkit-scrollbar {
  width: 6px;
}

.knowledgebase-list::-webkit-scrollbar-track {
  background: transparent;
}

.knowledgebase-list::-webkit-scrollbar-thumb {
  background: var(--stroke);
  border-radius: 3px;
}

.knowledgebase-list::-webkit-scrollbar-thumb:hover {
  background: var(--muted);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: var(--panel);
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--stroke);
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: 8px;
  font-size: 20px;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: var(--bg1);
  color: var(--text);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--stroke);
  background: var(--bg0);
}

/* 表单样式 */
.upload-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.file-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-hint {
  font-size: 12px;
  color: var(--muted);
  margin: 0;
}

.text-input {
  padding: 12px 16px;
  background: var(--bg0);
  border: 1px solid var(--stroke);
  border-radius: 10px;
  font-size: 14px;
  color: var(--text);
  transition: all 0.2s ease;
}

.text-input:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--shadow-hover);
}

.text-input::placeholder {
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