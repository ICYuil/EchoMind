import dayjs from 'dayjs'
import { getRecords } from './records'

export function buildAnalytics() {
  const recs = getRecords().slice().reverse() // chronological
  const points = recs.map(r => ({ x: dayjs(r.startedAt).format('MM-DD'), y: r.overall }))
  const last = recs[recs.length-1] || null

  // dimension average
  const dimMap = new Map()
  for (const r of recs) {
    for (const d of (r.dimensions || [])) {
      const cur = dimMap.get(d.k) || { sum:0, n:0 }
      cur.sum += d.v
      cur.n += 1
      dimMap.set(d.k, cur)
    }
  }
  const dimAvg = Array.from(dimMap.entries()).map(([k,v]) => ({ k, v: v.n ? Math.round(v.sum/v.n) : 0 }))
  dimAvg.sort((a,b)=>b.v-a.v)

  // moving trend
  const trend = points.map((p,i)=>{
    const window = points.slice(Math.max(0,i-2), i+1)
    const avg = window.reduce((s,t)=>s+t.y,0)/window.length
    return { x:p.x, y: Math.round(avg) }
  })

  return { recs, points, trend, dimAvg, last }
}
