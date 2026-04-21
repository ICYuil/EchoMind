<template>
  <div class="page-with-bg">
    <Topbar
      title="知识库"
      subtitle="管理文档，AI 将基于知识库内容回答您的问题。"
    >
      <div class="row gap10">
        <FullscreenButton 
          v-model="isFullscreen"
          @toggle="onFullscreenToggle"
        />
        <button class="btn" @click="showUpload = true">
          <UploadIcon class="icon" /> 上传文档
        </button>
        <button class="btn primary" @click="goToChat">
          <MessageSquareIcon class="icon" /> 知识问答
        </button>
      </div>
    </Topbar>

    <div class="container page-content">
      <!-- 统计卡片 -->
      <div class="grid4">
        <StatCard 
          label="总文档数" 
          :value="stats.totalCount || 0" 
          badge="全部"
          hint="已上传的知识库文档总数"
        />
        <StatCard 
          label="已完成" 
          :value="stats.completedCount || 0" 
          badge="可用"
          hint="向量化处理完成的文档"
        />
        <StatCard 
          label="处理中" 
          :value="stats.processingCount || 0" 
          badge="等待"
          hint="正在向量化的文档"
        />
        <StatCard 
          label="总提问数" 
          :value="stats.totalQuestionCount || 0" 
          badge="问答"
          hint="基于知识库的提问次数"
        />
      </div>

      <!-- 工具栏 -->
      <div class="card" style="margin-top:14px; padding:12px 14px;">
        <div class="row space center">
          <div class="row gap10">
            <input 
              v-model="searchKeyword" 
              placeholder="搜索知识库..." 
              class="input"
              @keyup.enter="handleSearch"
            />
            <button class="btn" @click="handleSearch">
              <SearchIcon class="icon" />
            </button>
          </div>
          <div class="row gap10">
            <select v-model="sortBy" class="select" @change="loadKnowledgeBases">
              <option value="time">按时间</option>
              <option value="size">按大小</option>
              <option value="access">按访问</option>
              <option value="question">按提问</option>
            </select>
            <select v-model="filterStatus" class="select" @change="loadKnowledgeBases">
              <option value="">全部状态</option>
              <option value="COMPLETED">已完成</option>
              <option value="PROCESSING">处理中</option>
              <option value="PENDING">待处理</option>
              <option value="FAILED">失败</option>
            </select>
            <button class="btn" @click="loadKnowledgeBases">
              <RefreshCwIcon class="icon" :class="{ spin: loading }" />
            </button>
          </div>
        </div>
      </div>

      <!-- 知识库列表 -->
      <div class="card" style="margin-top:14px;">
        <div v-if="loading" class="empty-state">
          <Loader2Icon class="icon spin" style="width:32px; height:32px;" />
          <div class="muted">加载中...</div>
        </div>

        <div v-else-if="knowledgeBases.length === 0" class="empty-state">
          <DatabaseIcon class="icon" style="width:48px; height:48px; opacity:0.3;" />
          <div class="muted">暂无知识库</div>
          <button class="btn primary" @click="showUpload = true" style="margin-top:12px;">
            上传第一个文档
          </button>
        </div>

        <div v-else class="list">
          <div 
            v-for="item in knowledgeBases" 
            :key="item.id" 
            class="item"
            :class="{ disabled: item.vectorStatus !== 'COMPLETED' }"
          >
            <div class="row space center">
              <div class="row gap10 center">
                <FileTextIcon class="icon" />
                <div>
                  <div style="font-weight:900;">{{ item.name }}</div>
                  <div class="muted2" style="font-size:12px; margin-top:4px;">
                    {{ formatFileSize(item.fileSize) }} · {{ formatDate(item.uploadedAt) }}
                  </div>
                </div>
              </div>
              <div class="row gap10 center">
                <StatusBadge :status="item.vectorStatus" />
                <div class="row gap6">
                  <button 
                    class="btn small" 
                    @click="downloadItem(item)"
                    :disabled="item.vectorStatus !== 'COMPLETED'"
                    title="下载"
                  >
                    <DownloadIcon class="icon" />
                  </button>
                  <button 
                    class="btn small" 
                    @click="chatWithItem(item)"
                    :disabled="item.vectorStatus !== 'COMPLETED'"
                    title="问答"
                  >
                    <MessageSquareIcon class="icon" />
                  </button>
                  <button 
                    class="btn small danger" 
                    @click="confirmDelete(item)"
                    title="删除"
                  >
                    <Trash2Icon class="icon" />
                  </button>
                </div>
              </div>
            </div>
            <div v-if="item.vectorError" class="error-text" style="margin-top:8px;">
              <AlertCircleIcon class="icon" /> {{ item.vectorError }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 上传弹窗 -->
    <Modal :open="showUpload" title="上传知识库" @close="showUpload = false">
      <KnowledgeBaseUpload 
        @upload-complete="onUploadComplete"
        @cancel="showUpload = false"
      />
    </Modal>

    <!-- 删除确认弹窗 -->
    <Modal :open="showDeleteConfirm" title="确认删除" @close="showDeleteConfirm = false">
      <div style="padding:20px;">
        <p>确定要删除知识库 <strong>{{ deleteTarget?.name }}</strong> 吗？</p>
        <p class="muted" style="margin-top:8px;">此操作不可恢复。</p>
        <div class="row gap10" style="margin-top:20px; justify-content:flex-end;">
          <button class="btn" @click="showDeleteConfirm = false">取消</button>
          <button class="btn danger" @click="doDelete" :disabled="deleting">
            {{ deleting ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, h } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import Topbar from '../components/ui/Topbar.vue'
import StatCard from '../components/ui/StatCard.vue'
import Modal from '../components/ui/Modal.vue'
import KnowledgeBaseUpload from './KnowledgeBaseUpload.vue'
import FullscreenButton from '../components/home/FullscreenButton.vue'
import { knowledgeBaseApi } from '../api/knowledgebase'
import {
  UploadIcon,
  MessageSquareIcon,
  SearchIcon,
  RefreshCwIcon,
  DatabaseIcon,
  FileTextIcon,
  DownloadIcon,
  Trash2Icon,
  Loader2Icon,
  AlertCircleIcon,
} from 'lucide-vue-next'

const router = useRouter()

// ===== 新增：全屏相关 =====
const emit = defineEmits(['fullscreen-change'])
const isFullscreen = ref(false)

const onFullscreenToggle = (value) => {
  isFullscreen.value = value
  emit('fullscreen-change', value)
}
// ===== 全屏相关结束 =====

// 状态
const loading = ref(false)
const knowledgeBases = ref([])
const stats = ref({})
const searchKeyword = ref('')
const sortBy = ref('time')
const filterStatus = ref('')
const showUpload = ref(false)
const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)

// 加载知识库列表
const loadKnowledgeBases = async () => {
  loading.value = true
  try {
    const list = await knowledgeBaseApi.getAllKnowledgeBases(
      sortBy.value,
      filterStatus.value || undefined
    )
    knowledgeBases.value = list
  } catch (err) {
    console.error('加载知识库失败:', err)
    alert('加载知识库失败: ' + err.message)
  } finally {
    loading.value = false
  }
}

// 加载统计
const loadStats = async () => {
  try {
    const data = await knowledgeBaseApi.getStatistics()
    stats.value = data
  } catch (err) {
    console.error('加载统计失败:', err)
  }
}

// 搜索
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    loadKnowledgeBases()
    return
  }
  loading.value = true
  try {
    const list = await knowledgeBaseApi.search(searchKeyword.value.trim())
    knowledgeBases.value = list
  } catch (err) {
    console.error('搜索失败:', err)
    alert('搜索失败: ' + err.message)
  } finally {
    loading.value = false
  }
}

// 下载
const downloadItem = async (item) => {
  try {
    const blob = await knowledgeBaseApi.downloadKnowledgeBase(item.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = item.originalFilename || item.name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (err) {
    console.error('下载失败:', err)
    alert('下载失败: ' + err.message)
  }
}

// 问答
const chatWithItem = (item) => {
  router.push({
    path: '/app/knowledge-chat',
    query: { kbId: item.id, kbName: item.name }
  })
}

// 跳转到问答页面
const goToChat = () => {
  router.push('/app/knowledge-chat')
}

// 确认删除
const confirmDelete = (item) => {
  deleteTarget.value = item
  showDeleteConfirm.value = true
}

// 执行删除
const doDelete = async () => {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await knowledgeBaseApi.deleteKnowledgeBase(deleteTarget.value.id)
    showDeleteConfirm.value = false
    deleteTarget.value = null
    loadKnowledgeBases()
    loadStats()
  } catch (err) {
    console.error('删除失败:', err)
    alert('删除失败: ' + err.message)
  } finally {
    deleting.value = false
  }
}

// 上传完成
const onUploadComplete = () => {
  showUpload.value = false
  loadKnowledgeBases()
  loadStats()
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (dateStr) => {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

// 状态徽章组件
const StatusBadge = {
  props: ['status'],
  render() {
    const statusMap = {
      COMPLETED: { text: '已完成', class: 'success' },
      PROCESSING: { text: '处理中', class: 'warning' },
      PENDING: { text: '待处理', class: 'info' },
      FAILED: { text: '失败', class: 'danger' },
    }
    const s = statusMap[this.status] || { text: '未知', class: '' }
    return h('span', { class: `badge ${s.class}` }, s.text)
  }
}

// 轮询定时器
let pollTimer = null

// 轮询刷新（当有 PENDING 或 PROCESSING 状态时）
const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(() => {
    const hasPending = knowledgeBases.value.some(
      kb => kb.vectorStatus === 'PENDING' || kb.vectorStatus === 'PROCESSING'
    )
    if (hasPending) {
      loadKnowledgeBases()
      loadStats()
    }
  }, 5000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(() => {
  loadKnowledgeBases()
  loadStats()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.grid4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

@media (max-width: 1024px) {
  .grid4 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .grid4 {
    grid-template-columns: 1fr;
  }
}

.input {
  padding: 8px 12px;
  border: 1px solid var(--stroke);
  border-radius: 8px;
  background: var(--bg);
  color: var(--text);
  min-width: 200px;
}

.select {
  padding: 8px 12px;
  border: 1px solid var(--stroke);
  border-radius: 8px;
  background: var(--bg);
  color: var(--text);
}

.list {
  padding: 12px 14px;
}

.item {
  padding: 14px;
  border-bottom: 1px solid var(--stroke);
  transition: background 0.2s;
}

.item:last-child {
  border-bottom: none;
}

.item:hover {
  background: var(--bg-soft);
}

.item.disabled {
  opacity: 0.6;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.error-text {
  color: var(--danger);
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn.small {
  padding: 6px 10px;
  font-size: 12px;
}

.btn.danger {
  background: var(--danger);
  color: white;
}

.badge.success { background: #10b981; color: white; }
.badge.warning { background: #f59e0b; color: white; }
.badge.info { background: #3b82f6; color: white; }
.badge.danger { background: #ef4444; color: white; }
</style>
