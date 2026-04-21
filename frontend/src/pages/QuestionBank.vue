<template>
  <div class="page-with-bg">
    <Topbar
      title="题库练习"
      subtitle="按方向浏览题目（UI 示例）。可扩展为收藏/错题本/练习计时。"
    />

    <div class="container page-content">
      <div class="grid2">
        <div class="card" style="padding:12px 14px;">
          <div style="font-weight:950;">方向</div>
          <div class="muted2" style="font-size:12px; margin-top:4px;">选择一个方向查看题目</div>

          <div class="row gap10 wrap" style="margin-top:12px;">
            <button class="btn btn-glow" :class="{ primary: type==='frontend' }" @click="type='frontend'">前端</button>
            <button class="btn btn-glow" :class="{ primary: type==='backend' }" @click="type='backend'">后端</button>
            <button class="btn btn-glow" :class="{ primary: type==='algo' }" @click="type='algo'">算法</button>
            <button class="btn btn-glow" :class="{ primary: type==='pm' }" @click="type='pm'">产品</button>
          </div>

          <div class="card soft" style="margin-top:14px; padding:12px 14px;">
            <div class="muted2" style="font-size:12px;">建议</div>
            <div class="muted" style="margin-top:8px; font-size:12px;">
              把“题库练习”联动到“开始面试”的随机抽题，让练习更系统。
            </div>
          </div>
        </div>

        <div class="card" style="padding:12px 14px;">
          <div style="font-weight:950;">练习区</div>
          <div class="muted2" style="font-size:12px; margin-top:4px;">点击题目开始练习（仅 UI）</div>

          <div class="item" style="margin-top:12px;">
            <div style="font-weight:950;">{{ current || '请选择一题开始练习' }}</div>
            <textarea class="textarea" style="margin-top:10px;" rows="6" v-model="draft" placeholder="写下你的回答要点…" />
            <div class="row gap10 wrap" style="margin-top:10px;">
              <button class="btn primary btn-glow" @click="markDone" :disabled="!current">标记已练</button>
              <button class="btn btn-glow" @click="draft=''" :disabled="!draft">清空</button>
            </div>
            <div class="muted2" style="margin-top:10px;" v-if="doneCount>0">
              已练题目数：<span class="mono">{{ doneCount }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="card" style="margin-top:14px; overflow:hidden;">
        <div style="padding:12px 14px; border-bottom:1px solid var(--stroke);">
          <div style="font-weight:950;">题目列表 · {{ label(type) }}</div>
          <div class="muted2" style="font-size:12px; margin-top:4px;">来自 mockQuestions.js</div>
        </div>
        <div style="padding:12px 14px 14px;">
          <div class="list">
            <div class="item" v-for="(q, i) in list" :key="i" style="cursor:pointer;" @click="pick(q)">
              <div class="row space center">
                <div style="font-weight:900; line-height:1.35;">{{ i+1 }}. {{ q }}</div>
                <div class="muted2" style="font-size:12px;" v-if="doneSet.has(q)">已练</div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import Topbar from '../components/ui/Topbar.vue'
import { QUESTION_BANK } from '../lib/mockQuestions'
import { lsGet, lsSet } from '../lib/storage'

const KEY_DONE = 'ai_bank_done'
const type = ref('frontend')
const current = ref('')
const draft = ref('')

const doneSet = ref(new Set(lsGet(KEY_DONE, [])))
const doneCount = computed(() => doneSet.value.size)

const list = computed(() => QUESTION_BANK[type.value] || [])

function label(t){
  const map = { frontend:'前端', backend:'后端', algo:'算法', pm:'产品' }
  return map[t] || t
}

function pick(q){
  current.value = q
  draft.value = ''
}

function markDone(){
  if (!current.value) return
  doneSet.value.add(current.value)
  lsSet(KEY_DONE, Array.from(doneSet.value))
}

onMounted(() => {
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
