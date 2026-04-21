import dayjs from 'dayjs'
import { lsGet, lsSet } from './storage'
import { currentUser } from './auth'

const KEY = 'ai_records'

function uid() {
  return Math.random().toString(16).slice(2) + Date.now().toString(16)
}

export function getRecords() {
  const me = currentUser()
  if (!me) return []
  const all = lsGet(KEY, {})
  return (all[me.email] || []).slice().sort((a,b)=> (b.startedAt||0)-(a.startedAt||0))
}

export function addRecord(rec) {
  const me = currentUser()
  if (!me) throw new Error('未登录')
  const all = lsGet(KEY, {})
  const list = (all[me.email] || []).slice()
  const r = {
    id: uid(),
    type: rec.type || 'frontend',
    title: rec.title || `${labelType(rec.type)} · 模拟面试`,
    overall: rec.overall ?? 75,
    dimensions: rec.dimensions || [
      { k:'沟通表达', v: 78 },
      { k:'技术深度', v: 72 },
      { k:'结构化思维', v: 70 },
      { k:'项目经验', v: 76 },
      { k:'加分项', v: 74 },
    ],
    transcript: rec.transcript || [
      { role:'interviewer', text:'请做一个自我介绍。' },
      { role:'candidate', text:'我叫… 我最近主要做…' },
    ],
    startedAt: rec.startedAt || Date.now(),
    endedAt: rec.endedAt || Date.now(),
    notes: rec.notes || '（示例）建议回答时更结构化，使用 STAR 方法。'
  }
  list.unshift(r)
  all[me.email] = list
  lsSet(KEY, all)
  return r
}

export function seedRecordsIfEmpty() {
  const me = currentUser()
  if (!me) return
  const all = lsGet(KEY, {})
  const list = (all[me.email] || [])
  if (list.length) return

  const now = dayjs()
  const types = ['frontend','backend','algo','pm']
  const mk = (i) => {
    const t = types[i % types.length]
    const base = 62 + i*3
    return {
      id: uid(),
      type: t,
      title: `${labelType(t)} · 第 ${i+1} 次面试`,
      overall: Math.min(92, base + (i%3)*4),
      dimensions: [
        { k:'沟通表达', v: clamp(base + 8 - i, 50, 95) },
        { k:'技术深度', v: clamp(base + 3 + i, 50, 95) },
        { k:'结构化思维', v: clamp(base + 1 + (i%4)*2, 50, 95) },
        { k:'项目经验', v: clamp(base + 6 - (i%5), 50, 95) },
        { k:'加分项', v: clamp(base - 2 + (i%3)*3, 50, 95) },
      ],
      transcript: [
        { role:'interviewer', text:'用 60 秒介绍你最近做的一个项目。' },
        { role:'candidate', text:'我最近做了… 我负责… 最终指标提升…' },
        { role:'interviewer', text:'如果让你重做一次，你会怎么优化？' },
        { role:'candidate', text:'我会从… 入手，重点解决…' },
      ],
      startedAt: now.subtract(14-i*2,'day').valueOf(),
      endedAt: now.subtract(14-i*2,'day').add(25+i,'minute').valueOf(),
      notes: i%2===0 ? '表达清晰，但缺少量化指标。' : '技术点扎实，建议补充业务背景与权衡。'
    }
  }
  const seeded = Array.from({length: 7}, (_,i)=>mk(i))
  all[me.email] = seeded
  lsSet(KEY, all)
}

function clamp(x, a, b){ return Math.max(a, Math.min(b, x)) }

export function labelType(t){
  const map = { frontend:'前端', backend:'后端', algo:'算法', pm:'产品' }
  return map[t] || t
}
