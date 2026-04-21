<template>
  <div class="layout">
    <main class="main">
      <router-view v-slot="{ Component, route }">
        <transition
          :css="false"
          @enter="enter"
          @leave="leave"
        >
          <component :is="Component" :key="route.path" class="page-container" />
        </transition>
      </router-view>
    </main>
    <MobileNav />
  </div>
</template>

<script setup>
import { onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import gsap from 'gsap'
import { lsGet } from '../lib/storage'
import MobileNav from '../components/ui/MobileNav.vue'
import '../components/layout_extras.css'

const router = useRouter()
const route = useRoute()

const isWindmill = computed(() => route.path === '/app/home')

const goHome = () => {
  router.push('/app/home')
}

// 在应用启动时加载保存的主题
onMounted(() => {
  const prefs = lsGet('ai_prefs', { theme: 'default' })
  const themeId = prefs.theme || 'default'
  
  // 移除所有主题类
  document.body.classList.remove('theme-warm', 'theme-ocean', 'theme-dopamine', 'theme-cloud-white')
  
  // 添加保存的主题类
  if (themeId !== 'default') {
    document.body.classList.add(`theme-${themeId}`)
  }
})

// 保存最后一次点击位置
let lastClickPosition = { x: 90, y: 90 }

// 获取动画起点位置
const getTransitionOrigin = () => {
  const saved = localStorage.getItem('transitionOrigin')
  if (saved) {
    try {
      const pos = JSON.parse(saved)
      lastClickPosition = pos
      localStorage.removeItem('transitionOrigin')
      return pos
    } catch (e) {
      console.error('解析点击位置失败:', e)
    }
  }
  return lastClickPosition
}

// Curtain Open Animation (Entering a sub-page from Windmill)
const enter = (el, done) => {
  const isEnteringHome = route.path === '/app/home'
  
  if (isEnteringHome) {
     // Entering Home: Fade in and scale up slightly
     // Important: set absolute position to ensure it overlaps correctly
     gsap.set(el, { position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 1 })
     gsap.fromTo(el, 
       { opacity: 0, scale: 0.95 }, 
       { opacity: 1, scale: 1, duration: 1.2, ease: 'power2.out', onComplete: done }
     )
  } else {
     // Entering Sub-page: Curtain Effect from click position
     const origin = getTransitionOrigin()
     
     // Set high z-index to cover the windmill
     gsap.set(el, { 
       position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', 
       zIndex: 100
       // 背景色由页面自身的CSS变量控制
     })
     
     gsap.fromTo(el,
       { 
         clipPath: `circle(0% at ${origin.x}% ${origin.y}%)`
       },
       { 
         clipPath: `circle(150% at ${origin.x}% ${origin.y}%)`, 
         duration: 1.2, 
         ease: 'power2.inOut', 
         onComplete: () => {
           gsap.set(el, { clearProps: 'clipPath' }) // Clear clipPath to allow full interaction
           done()
         }
       }
     )
  }
}

const leave = (el, done) => {
  const isGoingToHome = route.path === '/app/home'
  
  if (isGoingToHome) {
     // Leaving Sub-page: Shrink back to click position
     const origin = getTransitionOrigin()
     
     // Ensure it stays on top while shrinking
     gsap.set(el, { zIndex: 100, overflow: 'hidden' })
     
     gsap.to(el, {
       clipPath: `circle(0% at ${origin.x}% ${origin.y}%)`,
       duration: 1.0,
       ease: 'power2.inOut',
       onComplete: done
     })
  } else {
     // Leaving Windmill: Fade out slightly
     gsap.set(el, { zIndex: 1 })
     gsap.to(el, { 
       opacity: 0, 
       scale: 0.95,
       duration: 1.0, 
       ease: 'power2.inOut',
       onComplete: done 
     }) 
  }
}
</script>

<style>
/* Global fix for layout to handle absolute positioning */
.layout {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.main {
  position: relative;
  width: 100%;
  height: 100%;
}

.page-container {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  will-change: clip-path, transform, opacity;
  overflow-y: auto; /* Enable scrolling for sub-pages */
  background: var(--bg0); /* 使用主题背景色 */
  transition: background-color 0.3s ease; /* 主题切换时平滑过渡 */
}

/* Ensure Windmill home doesn't scroll unnecessarily */
.windmill-container {
  overflow: hidden;
}
</style>