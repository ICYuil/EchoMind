<template>
  <div class="study-plan-page">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <div class="header-left">
        <BookOpen width="24" height="24" class="header-icon" />
        <div class="header-titles">
          <h1 class="page-title">学习计划</h1>
          <p class="page-subtitle">根据面试表现生成的个性化学习建议</p>
        </div>
      </div>
      <div class="header-actions">
        <button class="btn-icon" @click="sidebarOpen = !sidebarOpen">
          <PanelLeft width="20" height="20" />
        </button>
      </div>
    </div>

    <div class="page-layout">
      <!-- 左侧侧边栏：面试记录列表 -->
      <Transition name="slide">
        <aside v-if="sidebarOpen" class="sidebar">
          <div class="sidebar-header">
            <h3>面试记录</h3>
            <span class="record-count">{{ interviewRecords.length }} 场</span>
          </div>
          <div class="records-list">
            <div
              v-for="(record, index) in interviewRecords"
              :key="record.sessionId"
              class="record-item"
              :class="{ 
                active: selectedRecord?.sessionId === record.sessionId,
                completed: isRecordCompleted(record.sessionId)
              }"
              @click="selectRecord(record)"
              v-motion
              :initial="{ opacity: 0, x: -20 }"
              :enter="{ opacity: 1, x: 0 }"
              :delay="index * 50"
            >
              <div class="record-content">
                <div class="record-title-row">
                  <FileText width="14" height="14" class="record-icon" />
                  <span class="record-title" :title="record.resumeFilename || '未知简历'">
                    {{ record.resumeFilename || '未知简历' }}
                  </span>
                  <span v-if="record.overallScore" class="score-badge">
                    {{ record.overallScore }}分
                  </span>
                </div>
                <div class="record-meta">
                  <Calendar width="12" height="12" />
                  {{ formatDate(record.createdAt) }}
                </div>
              </div>
              <div class="record-status-icon">
                <CheckCircle2 
                  v-if="isRecordCompleted(record.sessionId)" 
                  width="18" 
                  height="18" 
                  class="status-completed" 
                />
                <Circle v-else width="18" height="18" class="status-pending" />
              </div>
            </div>
            
            <div v-if="interviewRecords.length === 0" class="empty-records">
              <Inbox width="32" height="32" />
              <p>暂无面试记录</p>
              <button class="btn-primary small" @click="goToInterview">
                <Plus width="14" height="14" />
                去面试
              </button>
            </div>
          </div>
        </aside>
      </Transition>

      <!-- 右侧内容区：学习计划内容 -->
      <main class="main-content">
        <div v-if="!selectedRecord" class="empty-state">
          <div class="empty-content">
            <div class="empty-icon">
              <GraduationCap width="48" height="48" />
            </div>
            <h2>开始学习计划</h2>
            <p>从左侧选择一个面试记录，查看个性化学习建议</p>
          </div>
        </div>

        <template v-else>
          <div class="content-header">
            <div class="header-info">
              <h2 class="content-title">{{ selectedRecord.resumeFilename || '面试' }}学习建议</h2>
              <p class="content-subtitle">基于您的面试表现，为您推荐以下学习内容</p>
            </div>
            <div class="header-actions">
              <button
                v-if="!isRecordCompleted(selectedRecord.sessionId)"
                class="btn-complete"
                @click="markAsCompleted"
              >
                <CheckCircle2 width="16" height="16" />
                <span>标记为已完成</span>
              </button>
              <button
                v-else
                class="btn-completed"
                @click="markAsUncompleted"
              >
                <RotateCcw width="16" height="16" />
                <span>标记为未完成</span>
              </button>
            </div>
          </div>

          <div class="learning-content">
            <!-- Markdown 内容显示 -->
            <div v-if="learningContent" class="markdown-content" v-html="renderedMarkdown"></div>
            
            <!-- 学习计划列表 -->
            <div v-else-if="learningPlanList.length > 0" class="learning-plan-list">
              <div
                v-for="(plan, index) in learningPlanList"
                :key="index"
                class="plan-item"
              >
                <div class="plan-number">{{ index + 1 }}</div>
                <div class="plan-text">{{ plan }}</div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else class="empty-learning">
              <Lightbulb width="48" height="48" />
              <h3>暂无学习计划</h3>
              <p>该面试记录暂未生成学习建议</p>
            </div>
          </div>
        </template>
      </main>
    </div>

    <!-- 确认对话框 -->
    <Teleport to="body">
      <div v-if="showConfirmDialog" class="confirm-dialog-overlay" @click="cancelComplete">
        <div class="confirm-dialog" @click.stop>
          <div class="dialog-icon">🎉</div>
          <h3>确认完成学习计划？</h3>
          <p>标记完成后，该面试记录的学习计划将被标记为已完成</p>
          <div class="dialog-buttons">
            <button class="dialog-btn cancel" @click="cancelComplete">取消</button>
            <button class="dialog-btn confirm" @click="confirmComplete">确定</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  BookOpen, 
  PanelLeft, 
  FileText, 
  Calendar, 
  Inbox, 
  Plus,
  GraduationCap,
  CheckCircle2,
  Circle,
  RotateCcw,
  Lightbulb
} from 'lucide-vue-next'
import { interviewApi } from '../../api/interview.js'

const router = useRouter()
const route = useRoute()

// 侧边栏开关
const sidebarOpen = ref(true)

// 面试记录列表
const interviewRecords = ref([])
const selectedRecord = ref(null)

// 学习计划内容
const learningContent = ref('')
const learningPlanList = ref([])

// 完成状态（存储在 localStorage）
const completedRecords = ref(new Set())

// 确认对话框
const showConfirmDialog = ref(false)
const pendingCompleteId = ref(null)

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '未知时间'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric'
  })
}

// 加载完成状态
const loadCompletedRecords = () => {
  const saved = localStorage.getItem('completedStudyRecords')
  if (saved) {
    completedRecords.value = new Set(JSON.parse(saved))
  }
}

// 保存完成状态
const saveCompletedRecords = () => {
  localStorage.setItem('completedStudyRecords', JSON.stringify([...completedRecords.value]))
}

// 检查面试记录是否已完成
const isRecordCompleted = (sessionId) => {
  return completedRecords.value.has(sessionId)
}

// 标记为已完成
const markAsCompleted = () => {
  if (!selectedRecord.value) return
  pendingCompleteId.value = selectedRecord.value.sessionId
  showConfirmDialog.value = true
}

// 确认完成
const confirmComplete = () => {
  if (pendingCompleteId.value) {
    completedRecords.value.add(pendingCompleteId.value)
    saveCompletedRecords()
  }
  showConfirmDialog.value = false
  pendingCompleteId.value = null
}

// 取消完成
const cancelComplete = () => {
  showConfirmDialog.value = false
  pendingCompleteId.value = null
}

// 标记为未完成
const markAsUncompleted = () => {
  if (!selectedRecord.value) return
  completedRecords.value.delete(selectedRecord.value.sessionId)
  saveCompletedRecords()
}

// 将 learningPlan 列表转换为 Markdown 格式
const generateMarkdownFromLearningPlan = (learningPlan, improvements) => {
  if (!learningPlan || learningPlan.length === 0) return ''
  
  let markdown = '# 📚 个性化学习计划\n\n'
  
  // 添加待改进点
  if (improvements && improvements.length > 0) {
    markdown += '## 🎯 待提升领域\n\n'
    improvements.forEach((item, index) => {
      markdown += `${index + 1}. ${item}\n`
    })
    markdown += '\n---\n\n'
  }
  
  // 处理学习计划（支持嵌套结构）
  markdown += '## 📋 学习建议\n\n'
  markdown += parseLearningPlanToMarkdown(learningPlan)
  
  return markdown
}

// 递归解析学习计划为 Markdown
const parseLearningPlanToMarkdown = (items, level = 0) => {
  if (!items || items.length === 0) return ''
  
  let markdown = ''
  
  items.forEach((item, index) => {
    if (typeof item === 'string') {
      // 处理字符串项
      const trimmedItem = item.trim()
      if (trimmedItem.startsWith('### ')) {
        // 三级标题
        markdown += `${trimmedItem}\n\n`
      } else if (trimmedItem.startsWith('## ')) {
        // 二级标题
        markdown += `${trimmedItem}\n\n`
      } else if (trimmedItem.startsWith('- ')) {
        // 列表项
        markdown += `${trimmedItem}\n`
      } else if (trimmedItem.startsWith('```')) {
        // 代码块开始/结束
        markdown += `${trimmedItem}\n`
      } else if (trimmedItem.match(/^\d+\./)) {
        // 有序列表
        markdown += `${trimmedItem}\n`
      } else if (trimmedItem === '---') {
        // 分隔线
        markdown += `${trimmedItem}\n\n`
      } else {
        // 普通文本
        markdown += `${trimmedItem}\n\n`
      }
    } else if (Array.isArray(item)) {
      // 递归处理嵌套数组
      markdown += parseLearningPlanToMarkdown(item, level + 1)
    } else if (typeof item === 'object' && item !== null) {
      // 处理对象类型（如果有）
      if (item.text) {
        markdown += `${item.text}\n\n`
      }
    }
  })
  
  return markdown
}

// 增强的 Markdown 渲染
const renderedMarkdown = computed(() => {
  if (!learningContent.value) return ''
  
  let html = learningContent.value
  
  // 处理代码块（必须在行内代码之前）
  html = html.replace(/```(\w+)?\n([\s\S]*?)```/g, (match, lang, code) => {
    const language = lang || 'text'
    return `<div class="md-code-block"><div class="md-code-header"><span class="md-code-lang">${language}</span><span class="md-code-copy">复制</span></div><pre class="md-code-content"><code>${escapeHtml(code.trim())}</code></pre></div>`
  })
  
  // 处理行内代码
  html = html.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>')
  
  // 处理标题（带图标和样式）
  html = html.replace(/^###\s*✅?\s*(.*$)/gim, '<h3 class="md-h3"><span class="md-h3-icon">📋</span>$1</h3>')
  html = html.replace(/^##\s*(.*$)/gim, '<h2 class="md-h2"><span class="md-h2-line"></span>$1</h2>')
  html = html.replace(/^#\s*(.*$)/gim, '<h1 class="md-h1">$1</h1>')
  
  // 处理分隔线
  html = html.replace(/^\s*---\s*$/gim, '<hr class="md-hr">')
  
  // 处理引用块
  html = html.replace(/^>\s*(.*$)/gim, '<blockquote class="md-quote">$1</blockquote>')
  
  // 处理粗体（带高亮效果）
  html = html.replace(/\*\*\s*(.*?)\s*\*\*/g, '<strong class="md-strong">$1</strong>')
  
  // 处理斜体
  html = html.replace(/\*(.*?)\*/g, '<em class="md-em">$1</em>')
  
  // 处理复选框列表
  html = html.replace(/^\s*-\s*\[x\]\s+(.*$)/gim, '<div class="md-task md-task-checked"><span class="md-checkbox">✓</span><span>$1</span></div>')
  html = html.replace(/^\s*-\s*\[\s*\]\s+(.*$)/gim, '<div class="md-task"><span class="md-checkbox">○</span><span>$1</span></div>')
  
  // 处理无序列表（带自定义样式）
  html = html.replace(/(^|\n)\s*[-*+]\s+(.*)/g, (match, prefix, content) => {
    return `${prefix}<li class="md-li"><span class="md-li-bullet">•</span><span class="md-li-content">${content}</span></li>`
  })
  
  // 处理有序列表
  let orderIndex = 0
  html = html.replace(/(^|\n)\s*(\d+)\.\s+(.*)/g, (match, prefix, num, content) => {
    return `${prefix}<li class="md-oli"><span class="md-oli-num">${num}</span><span class="md-oli-content">${content}</span></li>`
  })
  
  // 包装列表
  html = html.replace(/(<li class="md-li[^"]*>.*?<\/li>)+/gs, '<ul class="md-ul">$&</ul>')
  html = html.replace(/(<li class="md-oli[^"]*>.*?<\/li>)+/gs, '<ol class="md-ol">$&</ol>')
  
  // 处理段落（按空行分割）
  const paragraphs = html.split(/\n\n+/)
  html = paragraphs.map(p => {
    p = p.trim()
    if (!p) return ''
    // 如果已经是块级元素，不包装
    if (/^<(h[1-6]|div|ul|ol|blockquote|hr|pre)/.test(p)) return p
    // 将换行转为 <br>
    p = p.replace(/\n/g, '<br>')
    return `<p class="md-p">${p}</p>`
  }).join('')
  
  // 包装整个内容
  return `<div class="md-container">${html}</div>`
})

// HTML 转义函数
const escapeHtml = (text) => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

// 选择面试记录
const selectRecord = async (record) => {
  selectedRecord.value = record
  learningContent.value = ''
  learningPlanList.value = []
  
  try {
    // 获取面试详情
    const detail = await interviewApi.getInterviewDetail(record.sessionId)
    if (detail) {
      // 使用 learningPlan 列表生成 Markdown 内容
      if (detail.learningPlan && detail.learningPlan.length > 0) {
        // 将列表转换为 Markdown 格式
        const markdownContent = generateMarkdownFromLearningPlan(detail.learningPlan, detail.improvements)
        learningContent.value = markdownContent
      }
    }
  } catch (err) {
    console.error('获取学习建议失败:', err)
  }
}

// 加载面试记录 - 从后端获取
const loadRecords = async () => {
  try {
    const records = await interviewApi.getInterviewHistory()
    if (records && records.length > 0) {
      interviewRecords.value = records.map(r => ({
        sessionId: r.sessionId,
        createdAt: r.createdAt,
        resumeFilename: r.resumeFilename,
        overallScore: r.overallScore,
        status: r.status
      }))
      
      // 如果有 URL 参数指定 sessionId，自动选中
      const { sessionId } = route.query
      if (sessionId) {
        const targetRecord = interviewRecords.value.find(r => r.sessionId === sessionId)
        if (targetRecord) {
          selectRecord(targetRecord)
        }
      }
    }
  } catch (err) {
    console.error('加载面试记录失败:', err)
  }
}

// 去面试
const goToInterview = () => {
  router.push('/app/home/interview/select-resume')
}

onMounted(() => {
  loadRecords()
  loadCompletedRecords()
})
</script>

<style scoped>
.study-plan-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg);
  overflow: hidden;
}

/* 顶部标题栏 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--stroke);
  background: var(--panel);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  color: var(--brand);
}

.header-titles {
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

.page-subtitle {
  font-size: 13px;
  color: var(--muted);
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--stroke);
  border-radius: 10px;
  background: var(--panel);
  color: var(--text);
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-icon:hover {
  background: var(--panel2);
  border-color: var(--brand);
  color: var(--brand);
}

/* 页面布局 */
.page-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 300px;
  background: var(--panel);
  border-right: 1px solid var(--stroke);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--stroke);
}

.sidebar-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.record-count {
  font-size: 12px;
  color: var(--muted);
  background: var(--panel2);
  padding: 2px 8px;
  border-radius: 10px;
}

.records-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 8px;
  border: 2px solid transparent;
}

/* 未完成 - 使用 bad 颜色变量 */
.record-item:not(.completed) {
  border-color: color-mix(in srgb, var(--bad) 30%, transparent);
  background: color-mix(in srgb, var(--bad) 3%, transparent);
}

.record-item:not(.completed):hover {
  border-color: color-mix(in srgb, var(--bad) 50%, transparent);
  background: color-mix(in srgb, var(--bad) 6%, transparent);
}

/* 已完成 - 使用 ok 颜色变量 */
.record-item.completed {
  border-color: color-mix(in srgb, var(--ok) 40%, transparent);
  background: color-mix(in srgb, var(--ok) 5%, transparent);
}

.record-item.completed:hover {
  border-color: color-mix(in srgb, var(--ok) 60%, transparent);
  background: color-mix(in srgb, var(--ok) 8%, transparent);
}

.record-item.active {
  background: var(--brand-bg) !important;
  border-color: var(--brand) !important;
}

.record-content {
  flex: 1;
  min-width: 0;
}

.record-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.record-icon {
  color: var(--muted);
  flex-shrink: 0;
}

.record-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.score-badge {
  font-size: 11px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  padding: 2px 6px;
  border-radius: 6px;
  flex-shrink: 0;
}

.record-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--muted);
}

.record-status-icon {
  flex-shrink: 0;
}

.status-completed {
  color: var(--ok);
}

.status-pending {
  color: var(--muted2);
}

.empty-records {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--muted);
  text-align: center;
}

.empty-records p {
  margin: 12px 0;
  font-size: 14px;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: var(--brand);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.btn-primary.small {
  padding: 8px 12px;
  font-size: 12px;
}

/* 主内容区 */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.empty-content {
  text-align: center;
  color: var(--muted);
}

.empty-icon {
  color: var(--brand);
  margin-bottom: 16px;
}

.empty-content h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 8px 0;
}

.empty-content p {
  font-size: 14px;
  margin: 0;
}

/* 内容头部 */
.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--stroke);
}

.header-info {
  flex: 1;
}

.content-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 6px 0;
}

.content-subtitle {
  font-size: 14px;
  color: var(--muted);
  margin: 0;
}

.btn-complete {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, var(--brand), color-mix(in srgb, var(--brand) 80%, black));
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-complete:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--brand) 30%, transparent);
}

.btn-completed {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: color-mix(in srgb, var(--ok) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--ok) 30%, transparent);
  border-radius: 10px;
  color: var(--ok);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-completed:hover {
  background: color-mix(in srgb, var(--ok) 15%, transparent);
  border-color: color-mix(in srgb, var(--ok) 50%, transparent);
}

/* 学习内容 */
.learning-content {
  max-width: 900px;
}

/* Markdown 内容样式 - 简洁优雅设计 */
.markdown-content {
  font-size: 16px;
  line-height: 1.8;
  color: var(--text);
}

/* Markdown 容器 */
.markdown-content :deep(.md-container) {
  padding: 8px 0;
}

/* 一级标题 */
.markdown-content :deep(.md-h1) {
  font-size: 26px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--brand);
}

/* 二级标题 */
.markdown-content :deep(.md-h2) {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin: 28px 0 16px 0;
  padding: 0;
}

.markdown-content :deep(.md-h2-line) {
  width: 4px;
  height: 22px;
  background: var(--brand);
  border-radius: 2px;
}

/* 三级标题 */
.markdown-content :deep(.md-h3) {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
  margin: 20px 0 12px 0;
  padding: 0;
}

.markdown-content :deep(.md-h3-icon) {
  font-size: 18px;
}

/* 段落 */
.markdown-content :deep(.md-p) {
  margin: 12px 0;
  color: var(--text);
  line-height: 1.8;
}

/* 分隔线 */
.markdown-content :deep(.md-hr) {
  border: none;
  height: 1px;
  background: var(--stroke);
  margin: 24px 0;
}

/* 引用块 */
.markdown-content :deep(.md-quote) {
  margin: 16px 0;
  padding: 12px 16px;
  background: color-mix(in srgb, var(--brand) 4%, transparent);
  border-left: 3px solid var(--brand);
  border-radius: 0 8px 8px 0;
  color: var(--text);
  font-style: italic;
}

/* 粗体高亮 */
.markdown-content :deep(.md-strong) {
  color: var(--brand);
  font-weight: 600;
}

/* 斜体 */
.markdown-content :deep(.md-em) {
  color: var(--muted);
  font-style: italic;
}

/* 代码块容器 */
.markdown-content :deep(.md-code-block) {
  margin: 16px 0;
  background: var(--panel2);
  border: 1px solid var(--stroke);
  border-radius: 8px;
  overflow: hidden;
}

.markdown-content :deep(.md-code-header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--panel);
  border-bottom: 1px solid var(--stroke);
}

.markdown-content :deep(.md-code-lang) {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  text-transform: uppercase;
}

.markdown-content :deep(.md-code-copy) {
  font-size: 12px;
  color: var(--brand);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.markdown-content :deep(.md-code-copy:hover) {
  background: color-mix(in srgb, var(--brand) 10%, transparent);
}

.markdown-content :deep(.md-code-content) {
  padding: 12px 16px;
  margin: 0;
  overflow-x: auto;
}

.markdown-content :deep(.md-code-content code) {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text);
}

/* 行内代码 */
.markdown-content :deep(.md-inline-code) {
  background: var(--panel2);
  border: 1px solid var(--stroke);
  border-radius: 4px;
  padding: 2px 6px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 14px;
  color: var(--brand);
}

/* 无序列表 - 简洁样式 */
.markdown-content :deep(.md-ul) {
  margin: 12px 0;
  padding: 0;
  list-style: none;
}

.markdown-content :deep(.md-li) {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 8px 0;
  padding: 0;
  line-height: 1.7;
}

.markdown-content :deep(.md-li-bullet) {
  color: var(--brand);
  font-size: 8px;
  line-height: 1.7;
  flex-shrink: 0;
  margin-top: 10px;
}

.markdown-content :deep(.md-li-content) {
  flex: 1;
  line-height: 1.7;
}

/* 有序列表 - 简洁样式 */
.markdown-content :deep(.md-ol) {
  margin: 12px 0;
  padding: 0;
  list-style: none;
  counter-reset: item;
}

.markdown-content :deep(.md-oli) {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin: 10px 0;
  padding: 0;
  line-height: 1.7;
}

.markdown-content :deep(.md-oli-num) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: var(--brand);
  border-radius: 50%;
  color: white;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  margin-top: 2px;
}

.markdown-content :deep(.md-oli-content) {
  flex: 1;
  line-height: 1.7;
}

/* 任务列表 */
.markdown-content :deep(.md-task) {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 8px 0;
  padding: 0;
  line-height: 1.7;
}

.markdown-content :deep(.md-task-checked) {
  opacity: 0.7;
}

.markdown-content :deep(.md-checkbox) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 2px solid var(--stroke);
  border-radius: 4px;
  font-size: 11px;
  color: var(--muted);
  flex-shrink: 0;
  margin-top: 4px;
  transition: all 0.2s;
}

.markdown-content :deep(.md-task-checked .md-checkbox) {
  background: var(--ok);
  border-color: var(--ok);
  color: white;
}

/* 学习计划列表 */
.learning-plan-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 学习计划列表 */
.learning-plan-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.plan-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 14px;
  transition: all 0.2s ease;
}

.plan-item:hover {
  border-color: var(--brand);
  transform: translateX(4px);
}

.plan-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  border-radius: 10px;
  color: white;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.plan-text {
  flex: 1;
  font-size: 15px;
  line-height: 1.7;
  color: var(--text);
}

/* 空学习状态 */
.empty-learning {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--muted);
  text-align: center;
}

.empty-learning h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
  margin: 16px 0 8px 0;
}

.empty-learning p {
  font-size: 14px;
  margin: 0;
}

/* 侧边栏动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}

/* 确认对话框 */
.confirm-dialog-overlay {
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
  backdrop-filter: blur(4px);
}

.confirm-dialog {
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: 20px;
  padding: 32px;
  text-align: center;
  max-width: 360px;
  width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.dialog-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.confirm-dialog h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 8px 0;
}

.confirm-dialog p {
  font-size: 14px;
  color: var(--muted);
  margin: 0 0 24px 0;
}

.dialog-buttons {
  display: flex;
  gap: 12px;
}

.dialog-btn {
  flex: 1;
  padding: 12px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dialog-btn.cancel {
  background: var(--panel2);
  border: 1px solid var(--stroke);
  color: var(--text);
}

.dialog-btn.cancel:hover {
  background: var(--stroke);
}

.dialog-btn.confirm {
  background: linear-gradient(135deg, var(--ok), color-mix(in srgb, var(--ok) 80%, black));
  border: none;
  color: white;
}

.dialog-btn.confirm:hover {
  filter: brightness(1.1);
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
  }
  
  .content-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
