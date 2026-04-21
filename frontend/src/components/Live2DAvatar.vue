<template>
  <div ref="wrap" class="avatar-wrap"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, defineExpose } from 'vue'
import { Application } from 'pixi.js'
import { pinyin } from 'pinyin-pro'
import { getToken } from '../lib/auth.js'

/**
 * ✅ 目标（稳定版）：
 * 1) 模型常驻 update（眨眼/物理/动作不会断）
 * 2) 口型：ParamMouthOpenY（开合）+ ParamA/I/U/E/O（元音形状）
 * 3) 顺序固定：先 update motion/physics → 再覆盖嘴型参数 → 再 core.update 提交
 */

const MODEL_JSON_PATH = '/models/kei/kei_vowels_pro/runtime/kei_vowels_pro.model3.json'
// 后端已配置 CORS，直接使用完整 URL
const API_BASE = 'http://localhost:8080'

const emit = defineEmits(['subtitle', 'speaking'])
const wrap = ref(null)
let app = null
let model = null

// audio
let currentAudio = null
let rafId = null

// offline envelope
let audioCtx = null
let envelope = null
let envelopeFps = 60
let vowelCues = null

// realtime analyser fallback
let analyser = null
let analyserSrc = null
let analyserData = null

// ticker
let masterTicker = null

// ---------------- Cubism Core 兜底加载 ----------------
function loadScriptOnce(src) {
  return new Promise((resolve, reject) => {
    const exists = [...document.scripts].some(s => s.src && s.src.includes(src))
    if (exists) return resolve()
    const s = document.createElement('script')
    s.src = src
    s.async = false
    s.onload = () => resolve()
    s.onerror = () => reject(new Error(`Failed to load script: ${src}`))
    document.head.appendChild(s)
  })
}
async function ensureCubismCore() {
  if (globalThis.Live2DCubismCore) return
  await loadScriptOnce('/live2d/live2dcubismcore.min.js')
  await new Promise(r => setTimeout(r, 0))
  if (!globalThis.Live2DCubismCore) throw new Error('Live2DCubismCore missing.')
}

// ---------------- 参数读写（稳健版）----------------
const _paramIndexCache = new Map()
const _idHandleCache = new Map()

// 可能的嘴型参数名（不同模型可能使用不同名称）
const MOUTH_PARAM_NAMES = [
  'ParamMouthOpenY',
  'PARAM_MOUTH_OPEN_Y',
  'MouthOpenY',
  'parammouthopeny'
]

function getCoreModel() {
  return (
    model?.internalModel?.coreModel ||
    model?.internalModel?._coreModel ||
    null
  )
}

function getIdHandle(idStr) {
  if (_idHandleCache.has(idStr)) return _idHandleCache.get(idStr)
  let handle = null
  try {
    const fw = globalThis?.Live2DCubismFramework?.CubismFramework
    const mgr = fw?.getIdManager?.()
    handle = mgr?.getId?.(idStr) ?? null
  } catch {}
  _idHandleCache.set(idStr, handle)
  return handle
}

// 根据 kei_vowels_pro.cdi3.json 文件硬编码的参数索引
// 这些索引是基于 Parameters 数组的顺序（从0开始）
const PARAM_INDEX_MAP = {
  'ParamAngleX': 0,
  'ParamAngleY': 1,
  'ParamAngleZ': 2,
  'ParamCheek': 3,
  'ParamBodyAngleX': 4,
  'ParamBodyAngleY': 5,
  'ParamBodyAngleZ': 6,
  'ParamBreath': 7,
  'ParamBrowLY': 8,
  'ParamBrowRY': 9,
  'ParamBrowLForm': 10,
  'ParamBrowRForm': 11,
  'ParamEyeLOpen': 12,
  'ParamEyeLSmile': 13,
  'ParamEyeROpen': 14,
  'ParamEyeRSmile': 15,
  'ParamEyeBallX': 16,
  'ParamEyeBallY': 17,
  'ParamMouthOpenY': 18,  // 嘴型开合
  'ParamA': 19,           // 元音 A
  'ParamI': 20,           // 元音 I
  'ParamU': 21,           // 元音 U
  'ParamE': 22,           // 元音 E
  'ParamO': 23,           // 元音 O
  'ParamHairFront': 24,
  'ParamHairSide': 25,
  'ParamHairSide2': 26,
  'ParamHairBack': 27,
  'ParamHairFrontFuwa': 28,
  'ParamHairSideFuwa': 29,
  'ParamHairBackFuwa': 30
}

function getParamIndex(idStr) {
  // 优先使用硬编码的索引
  if (PARAM_INDEX_MAP.hasOwnProperty(idStr)) {
    return PARAM_INDEX_MAP[idStr]
  }

  const core = getCoreModel()
  if (!core) return -1
  if (_paramIndexCache.has(idStr)) return _paramIndexCache.get(idStr)

  let idx = -1
  const handle = getIdHandle(idStr)

  if (typeof core.getParameterIndex === 'function') {
    if (handle) {
      try { idx = core.getParameterIndex(handle) } catch {}
    }
    if (idx < 0) {
      try { idx = core.getParameterIndex(idStr) } catch {}
    }
  }

  if (
    idx < 0 &&
    typeof core.getParameterCount === 'function' &&
    typeof core.getParameterId === 'function'
  ) {
    try {
      const n = core.getParameterCount()
      for (let i = 0; i < n; i++) {
        const pid = core.getParameterId(i)
        const s = (typeof pid === 'string') ? pid : (pid?.getString?.() || String(pid))
        if (s === idStr) { idx = i; break }
      }
    } catch {}
  }

  _paramIndexCache.set(idStr, idx)
  return idx
}

function safeSetParam(idStr, v) {
  // 如果是嘴型参数，尝试所有可能的参数名
  const paramNames = (idStr === 'ParamMouthOpenY') ? MOUTH_PARAM_NAMES : [idStr]

  // 方法1: 直接修改 _parameterValues 数组
  const core = model?.internalModel?.coreModel
  if (core && core._parameterValues) {
    for (const name of paramNames) {
      const idx = getParamIndex(name)
      if (idx >= 0 && idx < core._parameterValues.length) {
        core._parameterValues[idx] = v
        return
      }
    }
  }

  // 方法2-4: 其他设置方式（省略调试日志）
  if (model) {
    try {
      for (const name of paramNames) {
        if (typeof model.setParamFloat === 'function') {
          model.setParamFloat(name, v)
          return
        }
      }
    } catch (e) {}
  }

  if (model?.internalModel) {
    try {
      for (const name of paramNames) {
        if (typeof model.internalModel.setParamFloat === 'function') {
          model.internalModel.setParamFloat(name, v)
          return
        }
        if (typeof model.internalModel.setParameterValue === 'function') {
          model.internalModel.setParameterValue(name, v)
          return
        }
      }
    } catch (e) {}
  }

  const core2 = getCoreModel()
  if (core2) {
    for (const name of paramNames) {
      const handle = getIdHandle(name)
      if (typeof core2.setParameterValueById === 'function') {
        if (handle) {
          try { core2.setParameterValueById(handle, v); return } catch(e) {}
        }
        try { core2.setParameterValueById(name, v); return } catch(e) {}
      }
      const idx = getParamIndex(name)
      if (idx >= 0 && typeof core2.setParameterValueByIndex === 'function') {
        try { core2.setParameterValueByIndex(idx, v); return } catch(e) {}
      }
    }
  }
}

function clearVowels() {
  safeSetParam('ParamA', 0)
  safeSetParam('ParamI', 0)
  safeSetParam('ParamU', 0)
  safeSetParam('ParamE', 0)
  safeSetParam('ParamO', 0)
}

function setVowel(v) {
  clearVowels()
  const intensity = 1.5 // 使用放大系数
  if (v === 'A') safeSetParam('ParamA', intensity)
  if (v === 'I') safeSetParam('ParamI', intensity)
  if (v === 'U') safeSetParam('ParamU', intensity)
  if (v === 'E') safeSetParam('ParamE', intensity)
  if (v === 'O') safeSetParam('ParamO', intensity)
}

// ---------------- audio tools ----------------
function teardownAnalyser() {
  try { analyserSrc?.disconnect() } catch {}
  try { analyser?.disconnect() } catch {}
  analyser = null
  analyserSrc = null
  analyserData = null
}

function setupAnalyser(audioEl) {
  if (!audioCtx) return false
  teardownAnalyser()
  try {
    analyser = audioCtx.createAnalyser()
    analyser.fftSize = 2048
    analyserData = new Uint8Array(analyser.fftSize)
    analyserSrc = audioCtx.createMediaElementSource(audioEl)
    analyserSrc.connect(analyser)
    analyser.connect(audioCtx.destination)
    return true
  } catch (e) {
    teardownAnalyser()
    return false
  }
}

function analyserRms() {
  if (!analyser || !analyserData) return 0
  analyser.getByteTimeDomainData(analyserData)
  let sum = 0
  for (let i = 0; i < analyserData.length; i++) {
    const v = (analyserData[i] - 128) / 128
    sum += v * v
  }
  const rms = Math.sqrt(sum / analyserData.length)
  return Math.max(0, Math.min(1, rms * 3.5))
}

async function unlockAudio() {
  const AC = window.AudioContext || window.webkitAudioContext
  if (!audioCtx) audioCtx = new AC()
  if (audioCtx.state === 'suspended') {
    try { await audioCtx.resume() } catch {}
  }
  return true
}

// ---------------- envelope + vowels ----------------
// 嘴型放大系数：如果嘴张得不够大，调大这个值
const MOUTH_OPEN_SCALE = 3.0

function buildEnvelopeFromBuffer(audioBuffer, fps = 60) {
  const ch = audioBuffer.getChannelData(0)
  const sr = audioBuffer.sampleRate
  const hop = Math.max(1, Math.floor(sr / fps))
  const win = hop * 2
  const n = Math.ceil(ch.length / hop)
  const env = new Float32Array(n)
  for (let i = 0; i < n; i++) {
    const start = i * hop
    const end = Math.min(ch.length, start + win)
    let sum = 0
    for (let j = start; j < end; j++) {
      const v = ch[j]
      sum += v * v
    }
    const rms = Math.sqrt(sum / Math.max(1, end - start))
    env[i] = Math.max(0, Math.min(1, rms * 6.0))
  }
  
  return env
}

function envAt(t) {
  if (!envelope) return 0
  const idx = Math.max(0, Math.min(envelope.length - 1, Math.floor(t * envelopeFps)))
  return envelope[idx] || 0
}

function vowelFromSyllable(s) {
  const x = (s || '').toLowerCase()
  // 简单规则：按拼音/英文里出现的元音决定口型
  if (x.includes('a')) return 'A'
  if (x.includes('o')) return 'O'
  if (x.includes('e')) return 'E'
  if (x.includes('i')) return 'I'
  if (x.includes('u') || x.includes('v') || x.includes('ü')) return 'U'
  return 'A'
}

function buildVowelCuesFromText(text, duration) {
  const clean = (text || '').trim()
  if (!clean || !duration || duration <= 0) return null

  // 中文：用 pinyin-pro 拆成拼音数组；英文会原样混在数组里也能工作
  let syllables = []
  try {
    syllables = pinyin(clean, { toneType: 'none', type: 'array' })
      .map(s => String(s || '').trim())
      .filter(Boolean)
  } catch {
    syllables = []
  }

  // 兜底：拼音失败就按字符粗分
  if (!syllables.length) {
    syllables = [...clean].filter(ch => !/\s/.test(ch))
  }

  const vowels = syllables.map(vowelFromSyllable)
  const n = Math.max(1, vowels.length)
  const step = duration / n

  return vowels.map((v, i) => ({
    start: i * step,
    end: (i + 1) * step,
    v,
  }))
}

function vowelAt(t) {
  if (!vowelCues) return null
  for (const c of vowelCues) {
    if (t >= c.start && t < c.end) return c.v
  }
  return vowelCues.length ? vowelCues[vowelCues.length - 1].v : null
}

// ---------------- 手动眨眼状态 ----------------
let blinkEnabled = false
let blinkState = 'open' // 'open', 'closing', 'closed', 'opening'
let blinkTimer = 0
let nextBlinkTime = 0

// 更新眨眼状态（在每一帧调用）
function updateBlink(now) {
  if (!blinkEnabled) return
  if (model?.internalModel?.eyeBlink) return // 如果引擎有眨眼，不手动处理
  
  if (now >= nextBlinkTime && blinkState === 'open') {
    blinkState = 'closing'
    blinkTimer = now
  }
  
  if (blinkState === 'closing') {
    const progress = (now - blinkTimer) / 100 // 100ms闭眼
    if (progress >= 1) {
      safeSetParam('ParamEyeLOpen', 0)
      safeSetParam('ParamEyeROpen', 0)
      blinkState = 'closed'
      blinkTimer = now
    } else {
      const val = 1 - progress
      safeSetParam('ParamEyeLOpen', val)
      safeSetParam('ParamEyeROpen', val)
    }
  } else if (blinkState === 'closed') {
    if (now - blinkTimer > 50) { // 闭眼50ms
      blinkState = 'opening'
      blinkTimer = now
    }
  } else if (blinkState === 'opening') {
    const progress = (now - blinkTimer) / 100 // 100ms睁眼
    if (progress >= 1) {
      safeSetParam('ParamEyeLOpen', 1)
      safeSetParam('ParamEyeROpen', 1)
      blinkState = 'open'
      nextBlinkTime = now + 3000 + Math.random() * 2000 // 3-5秒后再眨眼
    } else {
      safeSetParam('ParamEyeLOpen', progress)
      safeSetParam('ParamEyeROpen', progress)
    }
  }
}

// ---------------- ✅ 核心：每帧更新顺序（确保嘴不会被覆盖） ----------------
function tickModel(tick) {
  if (!model) return

  const now = performance.now()

  // 1) 先让引擎跑 motion/physics（可能会改嘴）
  try {
    const dt = tick?.deltaTime ?? 0
    if (typeof model.update === 'function') {
      model.update(dt)
    } else if (typeof model?.internalModel?.update === 'function') {
      model.internalModel.update(dt)
    }
  } catch {}

  // 2) 再覆盖嘴型（说话时开合+AIUEO；不说话强制闭嘴）
  if (currentAudio && !currentAudio.paused && !currentAudio.ended) {
    const t = currentAudio.currentTime || 0
    let open = 0
    
    if (envelope) {
      // 使用预计算的 envelope
      open = envAt(t)
    } else if (analyser && analyserData) {
      // 使用实时分析，但设置一个最小值确保嘴能张开
      const rms = analyserRms()
      open = Math.max(0.3, rms)  // 最小张开 0.3
    }
    
    // 应用放大系数
    open = Math.min(1, open * MOUTH_OPEN_SCALE)
    
    safeSetParam('ParamMouthOpenY', open)

    const v = vowelAt(t)
    if (v) setVowel(v)
    else clearVowels()
  } else {
    safeSetParam('ParamMouthOpenY', 0)
    clearVowels()
  }
  
  // 3) 应用眨眼（在引擎更新之后，但在最终提交之前）
  updateBlink(now)

  // 4) 提交到 drawable（确保所有参数生效）
  try {
    getCoreModel()?.update?.()
  } catch {}
}

// ---------------- public methods ----------------
let preparedAudio = null
let preparedText = ''
let preparedUrl = null

// 预加载 TTS 音频（不播放）
async function preloadTTS(text, voice = 'zh-CN-XiaoxiaoNeural', rate = '+0%') {
  if (!model) return null
  const t = String(text || '').trim()
  if (!t) return null

  // 停止之前的
  try {
    if (currentAudio) {
      currentAudio.pause()
      currentAudio.currentTime = 0
    }
  } catch {}
  currentAudio = null
  teardownAnalyser()
  envelope = null
  vowelCues = null
  if (rafId) cancelAnimationFrame(rafId)
  rafId = null

  // 清理之前的预加载
  if (preparedUrl) {
    URL.revokeObjectURL(preparedUrl)
    preparedUrl = null
  }
  preparedAudio = null
  preparedText = ''

  // 1) request TTS
  const resp = await fetch(`${API_BASE}/api/interview/sessions/tts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken() || ''}`
    },
    body: JSON.stringify({ text: t, voice, rate }),
  })
  if (!resp.ok) {
    let detail = ''
    try { detail = JSON.stringify(await resp.json()) } catch {}
    throw new Error(`TTS failed: ${resp.status} ${detail}`)
  }
  const ab = await resp.arrayBuffer()
  if (!ab || ab.byteLength === 0) {
    throw new Error('TTS returned empty audio')
  }

  const url = URL.createObjectURL(new Blob([ab], { type: 'audio/mpeg' }))
  preparedUrl = url
  preparedText = t

  // 2) offline analyse envelope + vowel cues
  try {
    await unlockAudio()
    const buf = await audioCtx.decodeAudioData(ab.slice(0))
    envelopeFps = 60
    envelope = buildEnvelopeFromBuffer(buf, envelopeFps)
    vowelCues = buildVowelCuesFromText(t, buf.duration)
  } catch (e) {
    envelope = null
    vowelCues = null
  }

  // 创建 audio 对象但不播放
  const audio = new Audio(url)
  audio.volume = 1.0
  preparedAudio = audio

  // 如果离线 envelope 没算出来，用 realtime analyser 兜底
  if (!envelope) {
    try { await unlockAudio() } catch {}
    setupAnalyser(audio)
  }

  return { duration: audio.duration || (t.length / 4.5) }
}

// 播放预加载的音频
async function playPrepared() {
  if (!preparedAudio || !preparedUrl) {
    throw new Error('没有预加载的音频')
  }

  const audio = preparedAudio
  const url = preparedUrl
  const t = preparedText
  currentAudio = audio

  emit('speaking', true)
  emit('subtitle', '')

  // subtitle: 逐字显露
  const chars = [...t]
  let lastIdx = -1
  const subTick = () => {
    if (!currentAudio || currentAudio.paused || currentAudio.ended) return
    const dur = (currentAudio.duration && isFinite(currentAudio.duration) && currentAudio.duration > 0)
      ? currentAudio.duration
      : (chars.length / 4.5)
    const p = dur > 0 ? Math.min(1, Math.max(0, currentAudio.currentTime / dur)) : 0
    const idx = Math.min(chars.length, Math.floor(p * chars.length))
    if (idx !== lastIdx) {
      lastIdx = idx
      emit('subtitle', chars.slice(0, idx).join(''))
    }
    rafId = requestAnimationFrame(subTick)
  }

  audio.addEventListener('ended', () => {
    if (rafId) cancelAnimationFrame(rafId)
    rafId = null

    teardownAnalyser()
    currentAudio = null
    envelope = null
    vowelCues = null

    // 结束后马上闭嘴
    safeSetParam('ParamMouthOpenY', 0)
    clearVowels()

    emit('subtitle', '')
    emit('speaking', false)

    URL.revokeObjectURL(url)
    preparedAudio = null
    preparedUrl = null
    preparedText = ''
  }, { once: true })

  try {
    await audio.play()
  } catch (e) {
    emit('speaking', false)
    URL.revokeObjectURL(url)
    preparedAudio = null
    preparedUrl = null
    preparedText = ''
    throw e
  }

  rafId = requestAnimationFrame(subTick)
}

// 兼容旧接口：直接 speak
async function speak(text, voice = 'zh-CN-XiaoxiaoNeural', rate = '+0%') {
  await preloadTTS(text, voice, rate)
  await playPrepared()
}

defineExpose({ speak, preloadTTS, playPrepared, unlockAudio })

// ---------------- lifecycle ----------------
onMounted(async () => {
  if (!wrap.value) return
  await ensureCubismCore()

  const mod = await import('untitled-pixi-live2d-engine/cubism')
  const Live2DModel = mod.Live2DModel

  app = new Application()
  await app.init({ resizeTo: wrap.value, backgroundAlpha: 0, antialias: true })
  wrap.value.appendChild(app.canvas)

  model = await Live2DModel.from(MODEL_JSON_PATH)
  app.stage.addChild(model)

  // ✅ 关闭引擎自带 lipSync（避免它将来介入）
  try {
    model.internalModel.lipSync = false
  } catch (e) {}
  
  // ✅ 关闭鼠标追踪 - 通过 automator 属性
  try {
    if (model.automator) {
      model.automator.autoFocus = false
      console.log('[Live2D] 鼠标追踪已禁用')
    } else {
      console.warn('[Live2D] automator 不存在')
    }
  } catch (e) {
    console.warn('[Live2D] 禁用鼠标追踪失败:', e)
  }
  
  // ✅ 配置眨眼功能
  try {
    if (model.internalModel?.eyeBlink) {
      // 如果参数为空，手动设置
      const paramIds = model.internalModel.eyeBlink._parameterIds
      if ((!paramIds || paramIds._size === 0) && model.internalModel.idManager) {
        const eyeBlinkParams = model.internalModel.settings?.getEyeBlinkParameters?.()
        if (eyeBlinkParams?.length > 0) {
          for (const paramName of eyeBlinkParams) {
            const id = model.internalModel.idManager.getId(paramName)
            if (id) model.internalModel.eyeBlink._parameterIds.pushBack(id)
          }
        }
      }
      // 配置眨眼间隔
      model.internalModel.eyeBlink.setBlinkingInterval?.(4.0)
      blinkEnabled = true
    } else {
      blinkEnabled = true // 使用手动眨眼
    }
  } catch (e) {
    blinkEnabled = true
  }
  
  // ✅ 物理效果已启用
  
  // 初始化眨眼计时器
  nextBlinkTime = performance.now() + 3000



  const fit = () => {
    if (!model || !wrap.value) return
    const w = wrap.value.clientWidth || 1
    const h = wrap.value.clientHeight || 1
    const bw = model.width || 1
    const bh = model.height || 1
    const s = Math.min(w / bw, h / bh) * 0.95
    model.scale.set(s)
    model.x = w * 0.5
    model.y = h * 0.98
    model.anchor?.set?.(0.5, 1.0)
  }
  fit()
  window.addEventListener('resize', fit)

  // 初始闭嘴
  safeSetParam('ParamMouthOpenY', 0)
  clearVowels()
  try { getCoreModel()?.update?.() } catch {}

  // ✅ 常驻 ticker：永远跑更新 + 永远覆盖嘴型
  masterTicker = (tick) => tickModel(tick)
  app.ticker.add(masterTicker)
  
  // 通知外部组件加载完成
  emit('loaded')
})

onBeforeUnmount(() => {
  // 1) 停止正在播放的音频
  try { currentAudio?.pause() } catch {}
  currentAudio = null

  // 2) 停止字幕的 requestAnimationFrame 循环
  if (rafId) cancelAnimationFrame(rafId)
  rafId = null

  // 3) 清理 AudioContext analyser 相关连接
  teardownAnalyser()

  // 4) 销毁 Pixi 应用（会释放 canvas、纹理等）
  if (app) {
    try { app.destroy(true, true) } catch {}
    app = null
  }

  // 5) 释放 model 引用
  model = null

  // 6) 关闭 AudioContext
  if (audioCtx) {
    try { audioCtx.close() } catch {}
    audioCtx = null
  }
})
</script>

<style scoped>
.avatar-wrap{
  width:100%;
  height:100%;
  border-radius: 18px;
  overflow:hidden;
}
</style>
