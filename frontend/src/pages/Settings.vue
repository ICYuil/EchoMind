<template>
  <div class="page-with-bg">
    <Topbar
      title="设置"
      subtitle="主题/偏好/快捷键/关于等（示例）。"
    />

    <div class="container page-content">
      <div class="grid2">
        <div class="card" style="padding:12px 14px;">

          <div style="font-weight:950;">主题配色</div>
          <div class="muted2" style="font-size:12px; margin-top:4px;">选择你喜欢的界面风格</div>

          <div class="theme-grid" style="margin-top:12px;">
            <div 
              v-for="theme in themes" 
              :key="theme.id"
              class="theme-card"
              :class="{ active: pref.theme === theme.id }"
              @click="selectTheme(theme.id)"
            >
              <div class="theme-preview" :style="{ background: theme.preview }"></div>
              <div class="theme-name">{{ theme.name }}</div>
              <div class="theme-desc">{{ theme.desc }}</div>
            </div>
          </div>
        </div>

        <div class="card" style="padding:12px 14px;">
          <div style="font-weight:950;">偏好设置（示例）</div>
          <div class="muted2" style="font-size:12px; margin-top:4px;">仅保存在本地</div>

          <label class="row gap10 center" style="margin-top:12px;">
            <input type="checkbox" v-model="pref.autoSeed" />
            <span>自动生成示例面试记录</span>
          </label>

          <label class="row gap10 center" style="margin-top:10px;">
            <input type="checkbox" v-model="pref.showTips" />
            <span>显示引导提示</span>
          </label>

          <div class="row gap10 wrap" style="margin-top:12px;">
            <button class="btn primary btn-glow" @click="save">保存</button>
            <button class="btn btn-glow" @click="reset">恢复默认</button>
          </div>

          <div class="muted2" style="margin-top:10px;" v-if="msg">{{ msg }}</div>
        </div>

        <div class="card" style="padding:12px 14px;">
          <div style="font-weight:950;">关于</div>
          <div class="muted2" style="font-size:12px; margin-top:4px;">AI 面试官 UI 壳子</div>

          <div class="muted" style="margin-top:10px;">
            页面包含：登录/注册、工作台、面试、记录、统计、题库、个人中心、设置。
            后端接入时，把 localStorage 替换成真实 API 即可。
          </div>

          <div class="card soft" style="margin-top:14px; padding:12px 14px;">
            <div class="muted2 mono">localStorage key: ai_prefs</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Topbar from '../components/ui/Topbar.vue'
import { lsGet, lsSet } from '../lib/storage'

const KEY = 'ai_prefs'
const pref = ref(lsGet(KEY, { autoSeed: true, showTips: true, theme: 'default' }))
const msg = ref('')

// 5种主题配置
const themes = [
  {
    id: 'default',
    name: '科技冷静',
    desc: '经典蓝紫科技风',
    preview: 'linear-gradient(135deg, #0A0C14 0%, #1A1E2C 100%)'
  },
  {
    id: 'warm',
    name: '温暖高端',
    desc: '奢华金棕暖色调',
    preview: 'linear-gradient(135deg, #110F0B 0%, #1E1B16 100%)'
  },
  {
    id: 'ocean',
    name: '海洋灵感',
    desc: '清新蓝绿海洋风',
    preview: 'linear-gradient(135deg, #0A1A2F 0%, #1E3A5F 100%)'
  },
  {
    id: 'dopamine',
    name: '多巴胺撞色',
    desc: '活力橙+电光紫',
    preview: 'linear-gradient(135deg, #FF8200 0%, #7846AA 100%)'
  },
  {
    id: 'cloud-white',
    name: '岩雾白',
    desc: '淡紫灰护眼',
    preview: 'linear-gradient(135deg, #ECECF0 0%, #D8D4E0 100%)'
  }
]

// 应用主题
function applyTheme(themeId) {
  // 移除所有主题类
  document.body.classList.remove('theme-warm', 'theme-ocean', 'theme-dopamine', 'theme-cloud-white')
  
  // 添加新主题类
  if (themeId !== 'default') {
    document.body.classList.add(`theme-${themeId}`)
  }
}

// 选择主题
function selectTheme(themeId) {
  pref.value.theme = themeId
  applyTheme(themeId)
  save()
}

function save(){
  lsSet(KEY, pref.value)
  msg.value = '已保存'
  setTimeout(()=>msg.value='', 1800)
}

function reset(){
  pref.value = { autoSeed: true, showTips: true, theme: 'default' }
  applyTheme('default')
  save()
}

onMounted(() => {
  // 应用保存的主题
  applyTheme(pref.value.theme || 'default')
  
  document.querySelectorAll('.btn-glow').forEach(btn => {
    btn.addEventListener('mousemove', (e) => {
      const rect = btn.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top
      btn.style.setProperty('--mouse-x', `${x}px`)
      btn.style.setProperty('--mouse-y', `${y}px`)
    })
  })
})
</script>

<style scoped>
.theme-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

@media (max-width: 640px) {
  .theme-grid {
    grid-template-columns: 1fr;
  }
}

.theme-card {
  padding: 12px;
  border-radius: 16px;
  border: 2px solid var(--stroke);
  background: var(--panel);
  cursor: pointer;
  transition: all 0.3s ease;
}

.theme-card:hover {
  border-color: var(--brand);
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.theme-card.active {
  border-color: var(--brand);
  box-shadow: 0 0 20px rgba(100, 108, 255, 0.3);
}

.theme-preview {
  height: 60px;
  border-radius: 12px;
  margin-bottom: 10px;
}

.theme-name {
  font-weight: 700;
  font-size: 14px;
  color: var(--text);
}

.theme-desc {
  font-size: 12px;
  color: var(--muted2);
  margin-top: 4px;
}
</style>
