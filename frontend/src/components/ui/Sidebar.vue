<template>
  <!--
    Sidebar（侧边栏）
    - 负责：全局导航 + 当前用户信息 + 退出登录
    - 新增：可折叠（collapsed）+ 打卡入口 + 连续天数展示
  -->
  <aside class="sidebar" :class="collapsed ? 'collapsed' : ''">
    <div class="sb-top">
      <LogoMark />
      <div style="min-width:0;">
        <div class="sb-title">AI 面试官</div>
        <div class="sb-sub">UI Shell · 前端演示</div>
      </div>

      <!-- 折叠按钮：小屏幕/专注模式更实用 -->
      <button class="sb-toggle" :title="collapsed ? '展开侧边栏' : '折叠侧边栏'" @click="toggle">
        <ChevronRight v-if="collapsed" :size="18" />
        <ChevronLeft v-else :size="18" />
      </button>
    </div>

    <!-- 连续打卡信息：让用户一进来就看到“今天要不要打卡” -->
    <div class="sb-streak card soft">
      <div class="row space center">
        <div style="min-width:0;">
          <div style="font-weight:950;">连续 {{ streak }} 天</div>
          <div class="muted2" style="font-size:12px; margin-top:4px;">
            {{ canToday ? '今天还没打卡' : '今日已打卡' }}
          </div>
        </div>
        <RouterLink
          class="btn primary"
          style="padding:8px 10px; border-radius: 12px;"
          to="/app/checkin"
          :title="canToday ? '去打卡' : '查看奖励'"
        >
          {{ canToday ? '去打卡' : '奖励' }}
        </RouterLink>
      </div>
    </div>

    <!--
      导航分组：比“一长串按钮”更清晰，也更像真实产品。
      注意：为了“保持原有功能不变”，我们没有删任何入口，只是重新排版。
    -->
    <nav class="nav">
      <div class="nav-tracker" ref="navTracker"></div>
      <div class="nav-sec">核心</div>
      <RouterLink :title="collapsed ? '工作台' : ''" to="/app/home" :class="{ active: isActive('/app/home') }" ref="navLinks">
        <LayoutDashboard :size="18" />
        <span class="nav-text">工作台</span>
      </RouterLink>
      <RouterLink :title="collapsed ? '开始面试' : ''" to="/app/interview" :class="{ active: isActive('/app/interview') }" ref="navLinks">
        <Mic :size="18" />
        <span class="nav-text">开始面试</span>
      </RouterLink>

      <div class="nav-sec" style="margin-top:10px;">成长</div>
      <RouterLink :title="collapsed ? '面试记录' : ''" to="/app/records" :class="{ active: isActive('/app/records') }" ref="navLinks">
        <History :size="18" />
        <span class="nav-text">面试记录</span>
      </RouterLink>
      <RouterLink :title="collapsed ? '面试统计' : ''" to="/app/analytics" :class="{ active: isActive('/app/analytics') }" ref="navLinks">
        <ChartLine :size="18" />
        <span class="nav-text">面试统计</span>
      </RouterLink>
      <RouterLink :title="collapsed ? '题库练习' : ''" to="/app/bank" :class="{ active: isActive('/app/bank') }" ref="navLinks">
        <BookOpen :size="18" />
        <span class="nav-text">题库练习</span>
      </RouterLink>
      <RouterLink :title="collapsed ? '打卡中心' : ''" to="/app/checkin" :class="{ active: isActive('/app/checkin') }" ref="navLinks">
        <Calendar :size="18" />
        <span class="nav-text">打卡中心</span>
      </RouterLink>

      <div class="nav-sec" style="margin-top:10px;">知识库</div>
      <RouterLink :title="collapsed ? '知识库' : ''" to="/app/knowledge" :class="{ active: isActive('/app/knowledge') }" ref="navLinks">
        <Database :size="18" />
        <span class="nav-text">知识库</span>
      </RouterLink>
      <RouterLink :title="collapsed ? '知识问答' : ''" to="/app/knowledge-chat" :class="{ active: isActive('/app/knowledge-chat') }" ref="navLinks">
        <MessageSquare :size="18" />
        <span class="nav-text">知识问答</span>
      </RouterLink>

      <div class="nav-sec" style="margin-top:10px;">账户</div>
      <RouterLink :title="collapsed ? '个人中心' : ''" to="/app/profile" :class="{ active: isActive('/app/profile') }" ref="navLinks">
        <User :size="18" />
        <span class="nav-text">个人中心</span>
      </RouterLink>
      <RouterLink :title="collapsed ? '设置' : ''" to="/app/settings" :class="{ active: isActive('/app/settings') }" ref="navLinks">
        <Settings :size="18" />
        <span class="nav-text">设置</span>
      </RouterLink>
    </nav>

    <div class="sb-footer">
      <!--
        AvatarChip：一个“小组件”
        - label/sub/avatar 来自用户信息
        - cosmetics 来自打卡系统（如果用户装备了头像框/勋章，就能展示出来）
      -->
      <AvatarChip
        :label="user?.username"
        :sub="user?.email"
        :avatar="user?.avatar"
        :cosmetics="cosmetics"
        :compact="collapsed"
      />
      <div class="row gap10" style="margin-top:10px;">
        <button class="btn" style="flex:1;" @click="onLogout">
          退出
        </button>
      </div>
      <div class="muted2" style="margin-top:10px; font-size:12px;">
        本地存储演示 · 无后端
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { LayoutDashboard, Mic, History, ChartLine, BookOpen, User, Settings, Calendar, ChevronLeft, ChevronRight, Database, MessageSquare } from 'lucide-vue-next'
import LogoMark from './LogoMark.vue'
import AvatarChip from './AvatarChip.vue'
import { currentUser, logout } from '../../lib/auth'
import { lsGet, lsSet } from '../../lib/storage'
import { canCheckIn, getCheckinState, getEquippedCosmetics } from '../../lib/checkin'

const route = useRoute()
const router = useRouter()
const user = computed(() => currentUser())

// ======= Sidebar 折叠状态（存本地，刷新也能记住） =======
const collapsed = ref(lsGet('ai_sidebar_collapsed', false))
function toggle() {
  collapsed.value = !collapsed.value
  lsSet('ai_sidebar_collapsed', collapsed.value)
  // 折叠状态改变时更新追踪器位置
  setTimeout(updateNavTracker, 300)
}

// ======= 导航追踪 =======
const navTracker = ref(null)
const navLinks = ref([])

// 收集导航链接元素
function collectNavLinks() {
  // 手动收集所有导航链接
  const links = document.querySelectorAll('.nav a')
  navLinks.value = Array.from(links)
}

function updateNavTracker() {
  if (!navTracker.value) return
  
  collectNavLinks()
  
  const activeLink = navLinks.value.find(link => {
    return link?.getAttribute('href') === route.path
  })
  
  if (activeLink) {
    const rect = activeLink.getBoundingClientRect()
    const navRect = activeLink.parentElement.getBoundingClientRect()
    
    navTracker.value.style.top = `${rect.top - navRect.top}px`
    navTracker.value.style.height = `${rect.height}px`
    navTracker.value.style.width = `${collapsed.value ? '4px' : 'calc(100% - 16px)'}`
  }
}

// 监听路由变化
watch(() => route.path, () => {
  updateNavTracker()
})

// ======= 打卡状态（用于侧边栏的小提示） =======
const streak = ref(0)
const canToday = ref(true)
const cosmetics = ref({ frame: null, badge: null })

function refreshCheckin() {
  const email = user.value?.email
  if (!email) return
  const s = getCheckinState(email)
  streak.value = Number(s?.streak || 0)
  canToday.value = canCheckIn(email)
  cosmetics.value = getEquippedCosmetics(email)
}

function isActive(path){
  return route.path === path
}

function onLogout(){
  logout()
  router.replace('/login')
}

// 监听“打卡数据更新”事件（同一页面内组件同步用）
function onCheckinUpdated(ev){
  const email = ev?.detail?.email
  if (email && email === user.value?.email) refreshCheckin()
}

onMounted(() => {
  refreshCheckin()
  window.addEventListener('ai-checkin-updated', onCheckinUpdated)
  // 初始化导航追踪器位置
  setTimeout(updateNavTracker, 100)
})

onBeforeUnmount(() => {
  window.removeEventListener('ai-checkin-updated', onCheckinUpdated)
})
</script>

<style scoped>
/* 导航追踪器样式 */
.nav {
  position: relative;
  padding: 16px 8px;
}

.nav-tracker {
  position: absolute;
  left: 8px;
  top: 0;
  height: 40px;
  width: calc(100% - 16px);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  transition: all 0.3s ease;
  z-index: 1;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.nav a {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  transition: all 0.3s ease;
  color: var(--text);
  text-decoration: none;
}

.nav a:hover {
  background: rgba(102, 126, 234, 0.1);
  transform: translateX(4px);
}

.nav a.active {
  color: white;
  font-weight: 900;
}

.nav-sec {
  font-size: 12px;
  font-weight: 900;
  color: var(--muted);
  margin-bottom: 8px;
  padding: 0 12px;
}

.nav-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar.collapsed .nav-tracker {
  width: 4px;
  left: 8px;
}

.sidebar.collapsed .nav a {
  justify-content: center;
  padding: 10px;
}

.sidebar.collapsed .nav-text {
  display: none;
}

.sidebar.collapsed .nav-sec {
  display: none;
}
</style>
