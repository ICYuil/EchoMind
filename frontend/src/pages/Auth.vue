<template>
  <div class="auth-page page-with-bg">
    <!-- 粒子网络背景 -->
    <ParticleNetwork />
    
    <div class="auth-card" :class="{ 'register-mode': isRegister }">
      <!-- 左侧 -->
      <div class="auth-left">
        <!-- 登录表单 -->
        <div class="form-container">
          <div class="form-header">
            <div class="logo-mark">
              <svg viewBox="0 0 24 24" width="24" height="24">
                <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2"/>
                <circle cx="12" cy="12" r="4" fill="currentColor"/>
              </svg>
            </div>
            <h2>欢迎回来</h2>
            <p class="subtitle">登录你的账户继续面试之旅</p>
          </div>
          
          <!-- 错误提示 - 动态显示在表单顶部 -->
          <transition name="error-slide">
            <div v-if="loginError" class="error-message error-message-top">
              <span class="error-icon">⚠️</span>
              {{ loginError }}
            </div>
          </transition>
          
          <div class="form-body">
            <div class="input-wrap">
              <label>邮箱</label>
              <input 
                type="email" 
                v-model.trim="loginForm.email" 
                placeholder="请输入邮箱"
                class="input"
                :class="{ 'input-error': loginError }"
                @input="clearLoginError"
              />
            </div>
            
            <div class="input-wrap">
              <label>密码</label>
              <input 
                type="password" 
                v-model="loginForm.password" 
                placeholder="请输入密码"
                class="input"
                :class="{ 'input-error': loginError }"
                @input="clearLoginError"
              />
            </div>
            
            <div class="form-options">
              <label class="checkbox">
                <input type="checkbox" />
                <span>记住我</span>
              </label>
              <a href="#" class="link">忘记密码？</a>
            </div>
            
            <button class="btn-primary btn-glow" @click="onLogin">登 录</button>
          </div>
        </div>
        
        <!-- 左侧切换提示（注册模式） -->
        <div class="switch-panel">
          <div class="switch-content">
            <h3>已有账号？</h3>
            <p>欢迎回来！立即登录开始你的面试准备</p>
            <button class="btn-secondary btn-glow" @click="toggleAuth">
              去登录
            </button>
          </div>
        </div>
      </div>
      
      <!-- 右侧 -->
      <div class="auth-right">
        <!-- 右侧切换提示（登录模式） -->
        <div class="switch-panel">
          <div class="switch-content">
            <h3>新用户？</h3>
            <p>加入我们，开启AI智能面试新体验</p>
            <button class="btn-secondary btn-glow" @click="toggleAuth">
              去注册
            </button>
          </div>
        </div>
        
        <!-- 注册表单 -->
        <div class="form-container">
          <div class="form-header">
            <div class="logo-mark">
              <svg viewBox="0 0 24 24" width="24" height="24">
                <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2"/>
                <circle cx="12" cy="12" r="4" fill="currentColor"/>
              </svg>
            </div>
            <h2>创建账号</h2>
            <p class="subtitle">填写信息开始你的面试之旅</p>
          </div>
          
          <!-- 错误提示 - 动态显示在表单顶部 -->
          <transition name="error-slide">
            <div v-if="registerError" class="error-message error-message-top">
              <span class="error-icon">⚠️</span>
              {{ registerError }}
            </div>
          </transition>
          
          <div class="form-body">
            <div class="input-wrap">
              <label>用户名</label>
              <input 
                type="text" 
                v-model.trim="registerForm.username" 
                placeholder="请输入用户名"
                class="input"
                :class="{ 'input-error': registerError }"
                @input="clearRegisterError"
              />
            </div>
            
            <div class="input-wrap">
              <label>邮箱</label>
              <input 
                type="email" 
                v-model.trim="registerForm.email" 
                placeholder="请输入邮箱"
                class="input"
                :class="{ 'input-error': registerError }"
                @input="clearRegisterError"
              />
            </div>
            
            <div class="input-wrap">
              <label>密码</label>
              <input 
                type="password" 
                v-model="registerForm.password" 
                placeholder="请输入密码"
                class="input"
                :class="{ 'input-error': registerError }"
                @input="clearRegisterError"
              />
            </div>
            
            <div class="input-wrap">
              <label>确认密码</label>
              <input 
                type="password" 
                v-model="registerForm.confirmPassword" 
                placeholder="请再次输入密码"
                class="input"
                :class="{ 'input-error': registerError }"
                @input="clearRegisterError"
              />
            </div>
            
            <button class="btn-primary btn-glow" @click="onRegister">注 册</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 背景装饰 -->
    <!-- 动态背景层 -->
    <div class="bg-decoration">
      <!-- 基础渐变背景 -->
      <div class="bg-gradient"></div>
      <!-- 动态光晕 -->
      <div class="glow-1"></div>
      <div class="glow-2"></div>
      <div class="glow-3"></div>
      <div class="glow-4"></div>
      <!-- 粒子网格 -->
      <div class="particle-grid"></div>
      <!-- 浮动几何图形 -->
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
      </div>
      <!-- 扫描线效果 -->
      <div class="scan-lines"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, register } from '../lib/auth'
import { lsGet } from '../lib/storage'
import ParticleNetwork from '../components/ParticleNetwork.vue'

const router = useRouter()
const route = useRoute()

// 主题恢复 - 与主页面保持一致
const THEME_KEY = 'app_theme_id'

const initTheme = () => {
  const savedThemeId = lsGet(THEME_KEY, 1)
  document.body.setAttribute('data-theme', savedThemeId.toString())
}

// 状态管理
const isRegister = ref(false)

// 根据路由自动切换模式
onMounted(() => {
  // 恢复用户选择的主题
  initTheme()
  
  isRegister.value = route.path === '/register'
  
  // 添加按钮鼠标跟随效果
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

const loginForm = ref({
  email: '',
  password: ''
})

const registerForm = ref({
  email: '',
  username: '',
  password: '',
  confirmPassword: ''
})

const loginError = ref('')
const registerError = ref('')

// 清除登录错误
function clearLoginError() {
  if (loginError.value) {
    loginError.value = ''
  }
}

// 清除注册错误
function clearRegisterError() {
  if (registerError.value) {
    registerError.value = ''
  }
}

// 切换登录/注册
function toggleAuth() {
  isRegister.value = !isRegister.value
  // 更新路由
  if (isRegister.value) {
    router.replace('/register')
  } else {
    router.replace('/login')
  }
}

// 登录处理 - 异步调用后端接口
async function onLogin() {
  try {
    loginError.value = ''
    await login({
      email: loginForm.value.email,
      password: loginForm.value.password
    })
    router.replace(route.query.next || '/app/home')
  } catch (e) {
    loginError.value = e?.message || '登录失败'
  }
}

// 注册处理 - 异步调用后端接口
async function onRegister() {
  try {
    registerError.value = ''
    await register({
      email: registerForm.value.email,
      username: registerForm.value.username,
      password: registerForm.value.password
    })
    router.replace('/app/home')
  } catch (e) {
    registerError.value = e?.message || '注册失败'
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* 背景装饰 - 简化版，配合粒子网络 */
.bg-decoration {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

/* 基础渐变背景 - 使用全局主题变量 */
.bg-gradient {
  position: absolute;
  inset: 0;
  background: var(--bg-gradient, linear-gradient(135deg, #0A0C14 0%, #1A1E2C 100%));
}

/* 柔和光晕 - 仅保留一层 */
.glow-1 {
  position: absolute;
  top: -20%;
  left: -20%;
  width: 70%;
  height: 70%;
  background: radial-gradient(ellipse at center, rgba(100, 108, 255, 0.08) 0%, transparent 70%);
  animation: float 20s ease-in-out infinite;
  filter: blur(60px);
}

/* 动画定义 */
@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(20px, -20px) rotate(1deg); }
}

/* 主卡片 */
.auth-card {
  width: 900px;
  height: 560px;
  background: var(--panel);
  border: 1px solid var(--stroke);
  border-radius: var(--r20);
  box-shadow: var(--shadow);
  position: relative;
  z-index: 1;
  overflow: hidden;
  display: flex;
  backdrop-filter: blur(20px);
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.auth-left,
.auth-right {
  width: 50%;
  height: 100%;
  padding: 50px 45px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.auth-left {
  background: transparent;
}

.auth-right {
  background: var(--panel2);
  border-left: 1px solid var(--stroke);
}

/* 表单容器 */
.form-container,
.switch-panel {
  width: 100%;
  max-width: 340px;
  position: relative;
  z-index: 1;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 表单头部 */
.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-mark {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(100, 108, 255, 0.2) 0%, rgba(100, 108, 255, 0.05) 100%);
  border: 1px solid var(--stroke2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: var(--brand);
  box-shadow: 0 8px 32px rgba(100, 108, 255, 0.2);
}

.form-header h2 {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 8px;
  color: var(--text);
  letter-spacing: 0.5px;
}

.subtitle {
  font-size: 14px;
  color: var(--muted);
  margin: 0;
}

/* 表单内容 */
.form-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-wrap label {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}

.input {
  width: 100%;
  padding: 14px 16px;
  background: var(--panel2);
  border: 1px solid var(--stroke);
  border-radius: 12px;
  color: var(--text);
  font-size: 14px;
  transition: all 0.2s ease;
  outline: none;
}

.input::placeholder {
  color: var(--muted2);
}

.input:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(100, 108, 255, 0.15);
}

/* 表单选项 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  cursor: pointer;
}

.checkbox input {
  width: 16px;
  height: 16px;
  accent-color: var(--brand);
}

.link {
  color: var(--brand);
  text-decoration: none;
  transition: opacity 0.2s;
}

.link:hover {
  opacity: 0.8;
}

/* 按钮 */
.btn-primary {
  width: 100%;
  padding: 14px 24px;
  background: linear-gradient(135deg, var(--brand) 0%, rgba(100, 108, 255, 0.8) 100%);
  border: none;
  border-radius: 12px;
  color: white;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 8px;
  letter-spacing: 1px;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(100, 108, 255, 0.4);
}

.btn-primary:active {
  transform: translateY(0);
}

.btn-secondary {
  padding: 12px 32px;
  background: transparent;
  border: 1px solid var(--stroke2);
  border-radius: 12px;
  color: var(--text);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: var(--brand);
  color: var(--brand);
}

/* 切换面板 */
.switch-content {
  text-align: center;
}

.switch-panel h3 {
  font-size: 24px;
  font-weight: 800;
  margin: 0 0 12px;
  color: var(--text);
}

.switch-panel p {
  font-size: 14px;
  margin: 0 0 24px;
  line-height: 1.6;
  color: var(--muted);
}

/* 错误信息 */
.error-message {
  padding: 12px 16px;
  background: rgba(255, 90, 90, 0.1);
  border: 1px solid rgba(255, 90, 90, 0.3);
  border-radius: 10px;
  color: var(--bad);
  font-size: 13px;
  text-align: center;
}

/* 顶部错误提示 - 动态显示 */
.error-message-top {
  margin: 0 32px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 500;
  animation: errorShake 0.5s ease;
}

.error-icon {
  font-size: 16px;
  flex-shrink: 0;
}

/* 错误提示动画 */
.error-slide-enter-active,
.error-slide-leave-active {
  transition: all 0.3s ease;
}

.error-slide-enter-from,
.error-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 输入框错误状态 */
.input-error {
  border-color: var(--bad) !important;
  background: rgba(255, 90, 90, 0.05) !important;
}

.input-error:focus {
  box-shadow: 0 0 0 3px rgba(255, 90, 90, 0.2) !important;
}

/* 错误抖动动画 */
@keyframes errorShake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-5px); }
  40%, 80% { transform: translateX(5px); }
}

/* 切换动画 */

/* 登录表单 */
.auth-left .form-container {
  opacity: 1;
  transform: translateX(0);
}

.auth-card.register-mode .auth-left .form-container {
  opacity: 0;
  transform: translateX(-50px);
  position: absolute;
  pointer-events: none;
}

/* 左侧切换面板 */
.auth-left .switch-panel {
  opacity: 0;
  transform: translateX(50px);
  position: absolute;
  pointer-events: none;
}

.auth-card.register-mode .auth-left .switch-panel {
  opacity: 1;
  transform: translateX(0);
  position: relative;
  pointer-events: auto;
}

/* 右侧切换面板 */
.auth-right .switch-panel {
  opacity: 1;
  transform: translateX(0);
}

.auth-card.register-mode .auth-right .switch-panel {
  opacity: 0;
  transform: translateX(50px);
  position: absolute;
  pointer-events: none;
}

/* 注册表单 */
.auth-right .form-container {
  opacity: 0;
  transform: translateX(-50px);
  position: absolute;
  pointer-events: none;
}

.auth-card.register-mode .auth-right .form-container {
  opacity: 1;
  transform: translateX(0);
  position: relative;
  pointer-events: auto;
}

/* 响应式 */
@media (max-width: 960px) {
  .auth-card {
    width: 100%;
    max-width: 420px;
    height: auto;
    min-height: 600px;
    flex-direction: column;
  }
  
  .auth-left,
  .auth-right {
    width: 100%;
    padding: 40px 30px;
  }
  
  .auth-right {
    border-left: none;
    border-top: 1px solid var(--stroke);
  }
  
  /* 移动端：上下切换 */
  .auth-left .form-container,
  .auth-left .switch-panel,
  .auth-right .form-container,
  .auth-right .switch-panel {
    position: relative;
    opacity: 1;
    transform: none;
    pointer-events: auto;
  }
  
  .auth-card.register-mode .auth-left .form-container,
  .auth-card.register-mode .auth-right .switch-panel {
    display: none;
  }
  
  .auth-left .switch-panel,
  .auth-right .form-container {
    display: none;
  }
  
  .auth-card.register-mode .auth-left .switch-panel,
  .auth-card.register-mode .auth-right .form-container {
    display: flex;
  }
}
</style>
