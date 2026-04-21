<template>
  <!--
    MobileNav（移动端底部导航）
    目的：原项目在 <980px 时会隐藏 Sidebar，但没有替代导航。
    这样手机/小窗口体验会“没法跳页面”。
    这个组件只在小屏幕显示，功能上等价于侧边栏导航。
  -->
  <nav class="mnav">
    <div class="mnav-tracker" ref="navTracker"></div>
    <RouterLink to="/app/home" :class="{ active: isActive('/app/home') }" title="工作台" ref="navLinks">
      <LayoutDashboard :size="18" />
      <span>工作台</span>
    </RouterLink>
    <RouterLink to="/app/interview" :class="{ active: isActive('/app/interview') }" title="面试" ref="navLinks">
      <Mic :size="18" />
      <span>面试</span>
    </RouterLink>
    <RouterLink to="/app/records" :class="{ active: isActive('/app/records') }" title="记录" ref="navLinks">
      <History :size="18" />
      <span>记录</span>
    </RouterLink>
    <RouterLink to="/app/analytics" :class="{ active: isActive('/app/analytics') }" title="统计" ref="navLinks">
      <ChartLine :size="18" />
      <span>统计</span>
    </RouterLink>
    <RouterLink to="/app/checkin" :class="{ active: isActive('/app/checkin') }" title="打卡" ref="navLinks">
      <Calendar :size="18" />
      <span>打卡</span>
    </RouterLink>
    <RouterLink to="/app/profile" :class="{ active: isActive('/app/profile') }" title="我的" ref="navLinks">
      <User :size="18" />
      <span>我的</span>
    </RouterLink>
  </nav>
</template>

<script setup>
import { useRoute, RouterLink } from 'vue-router'
import { watch, onMounted, ref } from 'vue'
import { LayoutDashboard, Mic, History, ChartLine, Calendar, User } from 'lucide-vue-next'

const route = useRoute()
const navTracker = ref(null)
const navLinks = ref([])

function isActive(path) {
  return route.path === path
}

// 收集导航链接元素
function collectNavLinks() {
  // 手动收集所有导航链接
  const links = document.querySelectorAll('.mnav a')
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
    
    navTracker.value.style.left = `${rect.left - navRect.left}px`
    navTracker.value.style.width = `${rect.width}px`
  }
}

// 监听路由变化
watch(() => route.path, () => {
  updateNavTracker()
})

onMounted(() => {
  // 初始化导航追踪器位置
  setTimeout(updateNavTracker, 100)
})
</script>

<style scoped>
/* 移动端导航追踪器样式 */
.mnav {
  position: relative;
  display: flex;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  height: 56px;
  z-index: 100;
}

.mnav-tracker {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 3px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 3px 3px 0 0;
  transition: all 0.3s ease;
  z-index: 1;
  box-shadow: 0 -2px 8px rgba(102, 126, 234, 0.4);
}

.mnav a {
  position: relative;
  z-index: 2;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 0;
  color: var(--text);
  text-decoration: none;
  transition: all 0.3s ease;
}

.mnav a:hover {
  color: #667eea;
}

.mnav a.active {
  color: #667eea;
  font-weight: 900;
}

.mnav a span {
  font-size: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .mnav {
    height: 50px;
  }
  
  .mnav a {
    gap: 2px;
  }
  
  .mnav a span {
    font-size: 9px;
  }
}
</style>
