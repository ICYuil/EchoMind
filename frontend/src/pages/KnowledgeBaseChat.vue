<template>
  <div class="knowledge-chat-page" :class="{ 'fullscreen-mode': isFullscreen }">
    <!-- 右上角全屏按钮 -->
    
    <MotionDiv
      class="chat-header"
      :initial="{ opacity: 0, y: -20 }"
      :animate="{ opacity: 1, y: 0 }"
      :transition="{ duration: 0.5 }"
    >
      <!-- 左侧：返回按钮（全屏模式显示） -->
      <div class="header-left">
        <button v-if="isFullscreen" class="btn-icon back-btn" @click="goBack" title="返回">
          <ArrowLeft width="20" height="20" />
        </button>
        
        <!-- 标题 -->
        <div class="header-title">
          <h1 class="page-title">知识问答</h1>
          <p v-if="currentSession" class="session-subtitle">
            {{ currentSession.knowledgeBaseNames?.join(', ') || '通用问答' }}
          </p>
        </div>
      </div>
      <div class="header-actions">
        <button class="btn-secondary" @click="showNewSession = true">
          <Plus width="16" height="16" />
          新建会话
        </button>
        <button class="btn-icon" @click="sidebarOpen = !sidebarOpen">
          <PanelLeft width="20" height="20" />
        </button>
      </div>
    </MotionDiv>

    <div class="chat-layout">
      <!-- 侧边栏 - 会话列表 -->
      <Transition name="slide">
        <aside v-if="sidebarOpen" class="sidebar">
          <div class="sidebar-header">
            <h3>会话历史</h3>
            <button class="btn-icon small" @click="showNewSession = true">
              <Plus width="16" height="16" />
            </button>
          </div>
          <div class="session-list" ref="sessionListRef">
            <div
              v-for="(session, index) in sortedSessions"
              :key="session.id"
              class="session-item"
              :class="{ active: currentSessionId === session.id }"
              @click="switchSession(session.id)"
              v-motion
              :initial="{ opacity: 0, x: -20 }"
              :enter="{ opacity: 1, x: 0 }"
              :delay="index * 50"
            >
              <div class="session-content">
                <div class="session-title-row">
                  <MessageSquare width="14" height="14" class="session-icon" />
                  <span class="session-title" :title="session.title">
                    {{ session.title }}
                  </span>
                  <Pin
                    v-if="session.isPinned"
                    width="12"
                    height="12"
                    class="pin-icon"
                  />
                </div>
                <div class="session-meta">
                  {{ formatDate(session.updatedAt) }}
                </div>
              </div>
              <div class="session-actions" @click.stop>
                <button
                  class="action-btn"
                  :class="{ pinned: session.isPinned }"
                  @click="togglePin(session.id)"
                >
                  <Pin width="14" height="14" />
                </button>
                <button class="action-btn delete" @click="confirmDeleteSession(session)">
                  <Trash2 width="14" height="14" />
                </button>
              </div>
            </div>
            <div v-if="sessions.length === 0" class="empty-sessions">
              <Inbox width="32" height="32" />
              <p>暂无会话</p>
            </div>
          </div>
        </aside>
      </Transition>

      <!-- 主聊天区域 -->
      <main class="chat-main">
        <!-- 空状态 -->
        <div v-if="!currentSession" class="empty-state">
          <MotionDiv
            class="empty-content"
            :initial="{ opacity: 0, scale: 0.9 }"
            :animate="{ opacity: 1, scale: 1 }"
            :transition="{ duration: 0.5 }"
          >
            <div class="empty-icon">
              <Brain width="48" height="48" />
            </div>
            <h2>开始知识问答</h2>
            <p>基于您的知识库，与 AI 进行智能对话</p>
            <button class="btn-primary" @click="showNewSession = true">
              <Plus width="18" height="18" />
              新建会话
            </button>
          </MotionDiv>
        </div>

        <!-- 消息列表 -->
        <div v-else class="message-container">
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-icon">
              <Sparkles width="32" height="32" />
            </div>
            <h3>有什么可以帮您的？</h3>
            <p>基于选中的知识库，我可以回答您的任何问题</p>
          </div>

          <div v-else ref="messageListRef" class="message-list">
            <div
              v-for="(msg, index) in messagesWithStreaming"
              :key="msg.id || index"
              class="message-row"
              :class="msg.type"
            >
              <div class="message-bubble" :class="msg.type">
                <div class="message-header">
                  <span class="message-sender">{{ msg.type === 'user' ? '我' : 'AI助手' }}</span>
                </div>
                <div class="message-text" v-html="formatMessage(msg.content)"></div>
                <span v-if="msg.isStreaming" class="typing-cursor">|</span>
                <div class="message-meta">
                  <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
                  <span v-if="msg.sources && msg.sources.length > 0" class="message-sources">
                    <BookOpen width="12" height="12" />
                    引用 {{ msg.sources.length }} 个来源
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="input-area">
            <div v-if="selectedKnowledgeBases.length > 0" class="selected-kbs">
              <span class="kb-label">基于知识库:</span>
              <span
                v-for="kb in selectedKnowledgeBases"
                :key="kb.id"
                class="kb-tag"
              >
                <Database width="12" height="12" />
                {{ kb.name }}
              </span>
            </div>
            <div class="input-wrapper">
              <textarea
                v-model="inputMessage"
                placeholder="输入您的问题..."
                class="chat-input"
                rows="1"
                @keydown.enter.prevent="handleEnter"
                @input="autoResize"
                ref="inputRef"
              ></textarea>
              <button
                class="send-btn"
                @click="sendMessage"
                :disabled="!inputMessage.trim() || sending"
                :class="{ sending }"
              >
                <Send v-if="!sending" width="18" height="18" />
                <Loader2 v-else width="18" height="18" class="animate-spin" />
              </button>
            </div>
            <p class="input-hint">按 Enter 发送，Shift + Enter 换行</p>
          </div>
        </div>
      </main>
    </div>

    <!-- 新建会话弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showNewSession" class="modal-overlay" @click="showNewSession = false">
          <MotionDiv
            class="modal-content"
            :initial="{ opacity: 0, scale: 0.9 }"
            :animate="{ opacity: 1, scale: 1 }"
            :transition="{ type: 'spring', stiffness: 300, damping: 25 }"
            @click.stop
          >
            <div class="modal-header">
              <h3>新建会话</h3>
              <button class="btn-icon" @click="showNewSession = false">
                <X width="20" height="20" />
              </button>
            </div>
            <div class="modal-body">
              <div class="form-group">
                <label>会话名称（可选）</label>
                <input
                  v-model="newSessionTitle"
                  type="text"
                  placeholder="留空则自动生成"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label>选择知识库 <span class="required">*</span></label>
                <div v-if="loadingKnowledgeBases" class="loading-text">
                  <Loader2 width="16" height="16" class="animate-spin" />
                  加载中...
                </div>
                <div v-else-if="availableKnowledgeBases.length === 0" class="empty-text">
                  <FileX width="32" height="32" />
                  <p>暂无可用知识库</p>
                  <button class="btn-text" @click="router.push('/app/knowledge')">
                    去上传文档
                  </button>
                </div>
                <div v-else class="kb-list">
                  <label
                    v-for="kb in availableKnowledgeBases"
                    :key="kb.id"
                    class="kb-item"
                    :class="{ disabled: kb.vectorStatus !== 'COMPLETED' }"
                  >
                    <input
                      type="checkbox"
                      :value="kb.id"
                      v-model="selectedKbIds"
                      :disabled="kb.vectorStatus !== 'COMPLETED'"
                    />
                    <div class="kb-info">
                      <span class="kb-name">{{ kb.name }}</span>
                      <span v-if="kb.vectorStatus !== 'COMPLETED'" class="kb-status">
                        {{ kb.vectorStatus === 'PROCESSING' ? '处理中' : '不可用' }}
                      </span>
                    </div>
                  </label>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn-secondary" @click="showNewSession = false">取消</button>
              <button
                class="btn-primary"
                @click="createSession"
                :disabled="selectedKbIds.length === 0 || creating"
              >
                <Loader2 v-if="creating" width="16" height="16" class="animate-spin" />
                {{ creating ? '创建中...' : '创建会话' }}
              </button>
            </div>
          </MotionDiv>
        </div>
      </Transition>
    </Teleport>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showDeleteConfirm" class="modal-overlay" @click="showDeleteConfirm = false">
          <MotionDiv
            class="modal-content small"
            :initial="{ opacity: 0, scale: 0.9 }"
            :animate="{ opacity: 1, scale: 1 }"
            :transition="{ type: 'spring', stiffness: 300, damping: 25 }"
            @click.stop
          >
            <div class="modal-header">
              <AlertTriangle width="24" height="24" class="warning-icon" />
              <h3>确认删除</h3>
            </div>
            <div class="modal-body">
              <p>确定要删除会话 <strong>{{ deleteTarget?.title }}</strong> 吗？</p>
              <p class="hint">会话中的所有消息将被永久删除</p>
            </div>
            <div class="modal-footer">
              <button class="btn-secondary" @click="showDeleteConfirm = false">取消</button>
              <button class="btn-danger" @click="doDeleteSession">删除</button>
            </div>
          </MotionDiv>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Plus,
  PanelLeft,
  MessageSquare,
  Pin,
  Trash2,
  Brain,
  User,
  Bot,
  Send,
  Loader2,
  Database,
  Sparkles,
  BookOpen,
  Inbox,
  X,
  AlertTriangle,
  FileX
} from 'lucide-vue-next'
import { MotionDiv } from '../components/motion'
import VirtualList from '../components/VirtualList.vue'
import { ragChatApi } from '../api/ragChat'
import FullscreenButton from '../components/home/FullscreenButton.vue'  // 添加这行
import { knowledgeBaseApi } from '../api/knowledgebase'

const route = useRoute()
const router = useRouter()
// 添加全屏状态
const isFullscreen = ref(false)
// 定义事件
const emit = defineEmits(['fullscreen-change'])

// 添加全屏事件处理
const onFullscreenToggle = (value) => {
  isFullscreen.value = value
  emit('fullscreen-change', value)
}

// 状态
const sidebarOpen = ref(true)
const sessions = ref([])
const currentSessionId = ref(null)
const currentSession = ref(null)
const messages = ref([])
const inputMessage = ref('')
const sending = ref(false)
const streaming = ref(false)
const streamingContent = ref('')
const messageListRef = ref(null)
const inputRef = ref(null)

// 自动滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动
watch(() => messages.value.length, () => {
  scrollToBottom()
}, { flush: 'post' })
// 修改返回函数
const goBack = () => {
  // 判断是从哪里来的
  if (window.location.pathname.includes('/app/home/')) {
    // 如果是从主页的子路由来的，返回主页
    router.push('/app/home')
  } else {
    // 否则返回知识库列表
    router.push('/app/knowledge')
  }
}

// 新建会话
const showNewSession = ref(false)
const newSessionTitle = ref('')
const selectedKbIds = ref([])
const availableKnowledgeBases = ref([])
const loadingKnowledgeBases = ref(false)
const creating = ref(false)

// 删除会话
const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)

// 选中的知识库详情
const selectedKnowledgeBases = ref([])

// 排序后的会话列表（置顶的在前）
const sortedSessions = computed(() => {
  return [...sessions.value].sort((a, b) => {
    if (a.isPinned && !b.isPinned) return -1
    if (!a.isPinned && b.isPinned) return 1
    return new Date(b.updatedAt) - new Date(a.updatedAt)
  })
})

// 包含流式消息的消息列表
const messagesWithStreaming = computed(() => {
  const list = [...messages.value]
  if (streaming.value && streamingContent.value) {
    list.push({
      id: 'streaming',
      type: 'assistant',
      content: streamingContent.value,
      createdAt: new Date().toISOString(),
      isStreaming: true
    })
  }
  return list
})

// 加载会话列表
const loadSessions = async () => {
  try {
    const data = await ragChatApi.listSessions()
    sessions.value = data
  } catch (err) {
    console.error('加载会话列表失败:', err)
  }
}

// 加载可用知识库
const loadAvailableKnowledgeBases = async () => {
  loadingKnowledgeBases.value = true
  try {
    const list = await knowledgeBaseApi.getAllKnowledgeBases('time', 'COMPLETED')
    availableKnowledgeBases.value = list
  } catch (err) {
    console.error('加载知识库失败:', err)
  } finally {
    loadingKnowledgeBases.value = false
  }
}

// 切换会话
const switchSession = async (sessionId) => {
  currentSessionId.value = sessionId
  try {
    const detail = await ragChatApi.getSessionDetail(sessionId)
    currentSession.value = detail
    messages.value = detail.messages || []
    selectedKnowledgeBases.value = detail.knowledgeBases || []
  } catch (err) {
    console.error('加载会话详情失败:', err)
    alert('加载会话失败: ' + err.message)
  }
}

// 创建会话
const createSession = async () => {
  if (selectedKbIds.value.length === 0) return
  creating.value = true
  try {
    const session = await ragChatApi.createSession(
      selectedKbIds.value,
      newSessionTitle.value || undefined
    )
    showNewSession.value = false
    newSessionTitle.value = ''
    selectedKbIds.value = []
    await loadSessions()
    await switchSession(session.id)
  } catch (err) {
    console.error('创建会话失败:', err)
    alert('创建会话失败: ' + err.message)
  } finally {
    creating.value = false
  }
}

// 置顶/取消置顶
const togglePin = async (sessionId) => {
  try {
    await ragChatApi.togglePin(sessionId)
    loadSessions()
  } catch (err) {
    console.error('置顶失败:', err)
  }
}

// 确认删除
const confirmDeleteSession = (session) => {
  deleteTarget.value = session
  showDeleteConfirm.value = true
}

// 删除会话
const doDeleteSession = async () => {
  if (!deleteTarget.value) return
  try {
    await ragChatApi.deleteSession(deleteTarget.value.id)
    showDeleteConfirm.value = false
    if (currentSessionId.value === deleteTarget.value?.id) {
      currentSessionId.value = null
      currentSession.value = null
      messages.value = []
    }
    deleteTarget.value = null
    loadSessions()
  } catch (err) {
    console.error('删除会话失败:', err)
    alert('删除失败: ' + err.message)
  }
}

// 处理 Enter 键
const handleEnter = (e) => {
  if (!e.shiftKey) {
    sendMessage()
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || sending.value) return
  const content = inputMessage.value.trim()
  inputMessage.value = ''
  sending.value = true

  // 重置输入框高度
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }

  try {
    // 先添加用户消息到列表
    const userMsg = {
      id: Date.now(),
      type: 'user',
      content,
      createdAt: new Date().toISOString(),
    }
    messages.value.push(userMsg)
    scrollToBottom()

    // 流式接收AI回复
    streaming.value = true
    streamingContent.value = ''

    await ragChatApi.sendMessageStream(
      currentSessionId.value,
      content,
      (chunk) => {
        streamingContent.value += chunk
      },
      () => {
        // 完成回调
        streaming.value = false
        streamingContent.value = ''
        switchSession(currentSessionId.value)
      },
      (error) => {
        // 错误回调
        console.error('发送消息失败:', error)
        streaming.value = false
        streamingContent.value = ''
        alert('发送失败: ' + error.message)
      }
    )
  } catch (err) {
    console.error('发送消息失败:', err)
    streaming.value = false
    streamingContent.value = ''
    alert('发送失败: ' + err.message)
  } finally {
    sending.value = false
  }
}

// 自动调整输入框高度
const autoResize = () => {
  const el = inputRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 150) + 'px'
  }
}

// 格式化消息（处理代码块和换行）
const formatMessage = (content) => {
  if (!content) return ''
  // 转义 HTML
  let formatted = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  
  // 处理代码块
  formatted = formatted.replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre class="code-block"><code>$2</code></pre>')
  
  // 处理行内代码
  formatted = formatted.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
  
  // 处理换行
  formatted = formatted.replace(/\n/g, '<br>')
  
  return formatted
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(() => {
  loadSessions()
  loadAvailableKnowledgeBases()

  // 如果URL中有kbId，自动创建会话
  const kbId = route.query.kbId
  const kbName = route.query.kbName
  if (kbId) {
    selectedKbIds.value = [parseInt(kbId)]
    newSessionTitle.value = `关于 ${kbName || '文档'} 的问答`
    showNewSession.value = true
  }
})
</script>

<style scoped>
.knowledge-chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg0);
}

/* 头部 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: var(--panel);
  border-bottom: 1px solid var(--stroke);
  flex-shrink: 0;
  min-height: 72px; /* 给返回按钮留出空间 */
}
.header-left-placeholder {
  width: 120px; /* 与右侧操作区域宽度对应，保持平衡 */
}
.header-title {
  text-align: center;
  flex: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid var(--stroke);
  background: var(--panel);
  color: var(--text);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--panel-hover);
  border-color: var(--brand);
  transform: translateX(-2px);
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.session-subtitle {
  font-size: 13px;
  color: var(--muted);
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 180px; /* 固定宽度，保证左侧留白能平衡 */
  justify-content: flex-end;
}
/* 响应式调整 */
@media (max-width: 768px) {
  .chat-header {
    flex-direction: column;
    gap: 12px;
    padding: 12px 16px;
  }
  
  .header-left-placeholder {
    display: none;
  }
  
  .header-actions {
    width: 100%;
    justify-content: center;
  }
}

/* 按钮样式 */
.btn-icon {
  width: 40px;
  height: 40px;
  border: 1px solid var(--stroke);
  border-radius: 10px;
  background: var(--panel);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: var(--panel2);
  border-color: var(--stroke2);
  color: var(--text);
}

.btn-icon.small {
  width: 32px;
  height: 32px;
}

.btn-secondary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 10px;
  color: var(--text);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: var(--panel2);
  border-color: var(--stroke2);
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: var(--brand);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-hover);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-danger {
  padding: 10px 20px;
  background: var(--bad);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-danger:hover {
  background: var(--bad);
  opacity: 0.9;
}

.btn-text {
  padding: 8px 16px;
  background: transparent;
  border: none;
  color: var(--brand);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-text:hover {
  color: var(--brand);
  opacity: 0.8;
  text-decoration: underline;
}

/* 聊天布局 */
.chat-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 280px;
  background: var(--panel);
  border-right: 1px solid var(--stroke);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--stroke);
}

.sidebar-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.session-item:hover {
  background: var(--panel2);
}

.session-item.active {
  background: var(--panel2);
}

.session-content {
  flex: 1;
  min-width: 0;
}

.session-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-icon {
  color: var(--muted2);
  flex-shrink: 0;
}

.session-title {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-item.active .session-title {
  color: var(--brand);
}

.pin-icon {
  color: var(--warn);
  flex-shrink: 0;
}

.session-meta {
  font-size: 12px;
  color: var(--muted2);
  margin-top: 4px;
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.action-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted2);
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: var(--panel2);
  color: var(--muted);
}

.action-btn.pinned {
  color: var(--warn);
}

.action-btn.delete:hover {
  background: rgba(255, 90, 90, 0.1);
  color: var(--bad);
}

.empty-sessions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--muted2);
  gap: 8px;
}

.empty-sessions p {
  font-size: 13px;
  margin: 0;
}

/* 主聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 16px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: var(--brand);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.empty-content h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.empty-content p {
  font-size: 14px;
  color: var(--muted);
  margin: 0;
}

/* 消息容器 */
.message-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.welcome-message {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px;
  gap: 12px;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  background: var(--panel2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand);
}

.welcome-message h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.welcome-message p {
  font-size: 14px;
  color: var(--muted);
  margin: 0;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* 消息行 - 与面试对话一致 */
.message-row {
  display: flex;
  margin-bottom: 16px;
  animation: messageSlideIn 0.3s ease-out;
}

.message-row.assistant {
  justify-content: flex-start;
}

.message-row.user {
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

/* 消息气泡 - 与面试对话一致 */
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

/* AI助手消息 - 白色气泡，左对齐 */
.message-bubble.assistant {
  background: var(--panel);
  color: var(--text);
  border: 1px solid var(--stroke);
  border-top-left-radius: 4px;
}

/* 用户消息 - 绿色气泡，右对齐 */
.message-bubble.user {
  background: var(--ok);
  color: white;
  border: none;
  border-top-right-radius: 4px;
}

.message-bubble.streaming {
  background: var(--panel2);
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

.message-bubble.user .message-header {
  justify-content: flex-end;
}

.message-bubble.user .message-sender {
  color: rgba(255, 255, 255, 0.8);
}

.message-text {
  word-break: break-word;
}

.message-text :deep(.code-block) {
  background: var(--bg0);
  color: var(--text);
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
  font-size: 13px;
  line-height: 1.5;
}

.message-text :deep(.inline-code) {
  background: var(--panel2);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  color: var(--brand);
}

.message-bubble.user .message-text :deep(.inline-code) {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.typing-cursor {
  animation: blink 1s infinite;
  color: var(--brand);
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--muted2);
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--stroke);
}

.message-bubble.user .message-meta {
  justify-content: flex-end;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);
}

.message-sources {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--brand);
}

.message-bubble.user .message-sources {
  color: rgba(255, 255, 255, 0.9);
}

/* 输入区域 */
.input-area {
  padding: 20px 24px;
  background: var(--panel);
  border-top: 1px solid var(--stroke);
}

.selected-kbs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.kb-label {
  font-size: 12px;
  color: var(--muted2);
}

.kb-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: var(--panel2);
  border-radius: 6px;
  font-size: 12px;
  color: var(--brand);
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid var(--stroke);
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  min-height: 48px;
  max-height: 150px;
  font-family: inherit;
  transition: all 0.2s;
  background: var(--panel);
  color: var(--text);
}

.chat-input:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(100, 108, 255, 0.1);
}

.send-btn {
  width: 48px;
  height: 48px;
  background: var(--brand);
  border: none;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: var(--shadow-hover);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn.sending {
  background: var(--muted2);
}

.input-hint {
  font-size: 12px;
  color: var(--muted2);
  margin: 8px 0 0;
  text-align: center;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  inset: 0;
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
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-content.small {
  max-width: 400px;
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

.warning-icon {
  color: var(--warn);
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
}

/* 表单 */
.form-group {
  margin-bottom: 20px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 8px;
}

.required {
  color: var(--bad);
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--stroke);
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
  box-sizing: border-box;
  background: var(--panel);
  color: var(--text);
}

.form-input:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(100, 108, 255, 0.1);
}

.loading-text {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 14px;
}

.empty-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px;
  color: var(--muted2);
  gap: 12px;
}

.empty-text p {
  margin: 0;
}

.kb-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--stroke);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.kb-item:hover:not(.disabled) {
  border-color: var(--brand);
  background: var(--panel2);
}

.kb-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.kb-item input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: var(--brand);
}

.kb-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.kb-name {
  font-size: 14px;
  color: var(--text);
}

.kb-status {
  font-size: 12px;
  color: var(--muted2);
}

.hint {
  font-size: 13px;
  color: var(--muted);
  margin: 8px 0 0;
}

/* 过渡动画 */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(-100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 73px;
    bottom: 0;
    z-index: 100;
    box-shadow: 4px 0 12px rgba(0, 0, 0, 0.1);
  }

  .message-content {
    max-width: 85%;
  }
}
/* 添加全屏按钮样式 */
.chat-header-fullscreen {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 100;
}


</style>
