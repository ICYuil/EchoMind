<template>
  <div class="page-with-bg">
    <Topbar
      title="打卡中心"
      subtitle="每天打卡一次，形成学习习惯；连续打卡可解锁头像挂件与勋章（纯前端/本地存储演示）。"
    >
      <!-- 顶部右侧：快捷按钮（保持页面轻量） -->
      <button class="btn primary btn-glow" :disabled="!canToday" @click="onCheckin">
        {{ canToday ? '今日打卡' : '已打卡' }}
      </button>
    </Topbar>

    <div class="container page-content">
      <!-- Bento 风格：左侧状态 + 右侧日历（更“别出心裁”，但不影响原有功能） -->
      <div class="bento">
        <!-- A) 今日状态/连续天数 -->
        <div class="card bento-a" style="overflow:hidden;">
          <div class="card-head">
            <div>
              <div class="card-title">今日状态</div>
              <div class="card-sub">{{ todayStr }} · {{ canToday ? '还没打卡' : '已完成' }}</div>
            </div>
            <span class="badge">连续 {{ state.streak }} 天</span>
          </div>

          <div style="padding: 14px;">
            <div class="row gap14 wrap" style="align-items: stretch;">
              <!-- 连续天数/累计天数：用 kv 小卡片让信息更直观 -->
              <div class="kv" style="flex:1; min-width: 200px;">
                <div>
                  <div class="k">连续打卡</div>
                  <div class="v">{{ state.streak }} 天</div>
                </div>
                <div class="muted2" style="font-size:12px;">streak</div>
              </div>
              <div class="kv" style="flex:1; min-width: 200px;">
                <div>
                  <div class="k">累计打卡</div>
                  <div class="v">{{ state.total }} 天</div>
                </div>
                <div class="muted2" style="font-size:12px;">total</div>
              </div>
            </div>

            <!-- 连续打卡进度条：给新手一个“怎么做进度条”的示例 -->
            <div style="margin-top:14px;">
              <div class="row space center">
                <div class="muted2" style="font-size:12px;">最近目标：连续 {{ nextMilestone?.milestone || '--' }} 天</div>
                <div class="muted2" style="font-size:12px;">{{ progressLabel }}</div>
              </div>
              <div class="progress" style="margin-top:8px;">
                <div class="bar" :style="{ width: progressPct + '%' }"></div>
              </div>
              <div class="muted" style="font-size:12px; margin-top:8px; line-height:1.5;">
                说明：若中断（比如今天没打，明天再打）则 streak 会重新从 1 开始。
              </div>
            </div>

            <div class="hr" style="margin:14px 0;"></div>

            <!-- B) 奖励面板：已解锁/未解锁 -->
            <div class="row space center">
              <div>
                <div class="card-title">奖励</div>
                <div class="card-sub">连续天数达到里程碑即可解锁；你可以“装备”到头像上。</div>
              </div>
              <span class="badge">已解锁 {{ unlocked.length }} / {{ rewards.length }}</span>
            </div>

            <div class="reward-grid" style="margin-top:12px;">
              <div
                v-for="r in rewards"
                :key="r.id"
                class="reward"
                :class="isUnlocked(r.id) ? 'ok' : 'lock'"
              >
                <div class="row space center" style="gap:10px;">
                  <div class="row gap10 center" style="min-width:0;">
                    <div class="reward-ico">{{ r.icon }}</div>
                    <div style="min-width:0;">
                      <div style="font-weight:950; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">{{ r.name }}</div>
                      <div class="muted2" style="font-size:12px; margin-top:4px;">连续 {{ r.milestone }} 天</div>
                    </div>
                  </div>

                  <!-- “装备/卸下”按钮：演示一个简单的状态切换（已解锁才能装备） -->
                  <div class="row gap8 center">
                    <button
                      v-if="r.type==='frame'"
                      class="btn"
                      :disabled="!isUnlocked(r.id)"
                      @click="toggleEquip('frame', r.id)"
                    >
                      {{ state.equipped?.frame === r.id ? '卸下' : '装备' }}
                    </button>
                    <button
                      v-if="r.type==='badge'"
                      class="btn"
                      :disabled="!isUnlocked(r.id)"
                      @click="toggleEquip('badge', r.id)"
                    >
                      {{ state.equipped?.badge === r.id ? '卸下' : '装备' }}
                    </button>
                  </div>
                </div>

                <div class="muted" style="font-size:12px; margin-top:8px; line-height:1.5;">
                  {{ r.desc }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- B) 日历：展示本月打卡情况（更直观，也方便你学习日历渲染） -->
        <div class="card bento-b" style="overflow:hidden;">
          <div class="card-head">
            <div>
              <div class="card-title">打卡日历</div>
              <div class="card-sub">本月打卡天会高亮；点击“今日打卡”会立刻更新。</div>
            </div>
            <div class="row gap10 center">
              <button class="btn" @click="prevMonth">上月</button>
              <button class="btn" @click="nextMonth">下月</button>
            </div>
          </div>

          <div style="padding: 14px;">
            <div class="row space center" style="margin-bottom:10px;">
              <div style="font-weight:950;">{{ monthTitle }}</div>
              <div class="muted2" style="font-size:12px;">高亮 = 已打卡</div>
            </div>

            <div class="cal-week">
              <div v-for="w in weekNames" :key="w" class="cal-w">{{ w }}</div>
            </div>
            <div class="cal-grid">
              <div v-for="(d,idx) in calDays" :key="idx" class="cal-cell" :class="cellClass(d)">
                <div class="cal-day">{{ d.day }}</div>
              </div>
            </div>

            <div class="hr" style="margin:14px 0;"></div>

            <!-- 小贴士：告诉新手哪里改成后端接口 -->
            <div class="card soft" style="padding:12px 14px;">
              <div class="muted2" style="font-size:12px;">后端接入建议</div>
              <div class="muted" style="font-size:12px; margin-top:8px; line-height:1.5;">
                你们接后端时，可以做：<span class="mono">GET /checkin/state</span> 获取 streak/total；
                <span class="mono">POST /checkin</span> 完成打卡；
                <span class="mono">POST /checkin/equip</span> 装备奖励。
                目前这些都在 <span class="mono">src/lib/checkin.js</span> 里用 localStorage 模拟。
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 这里展示了一个“页面级状态管理”的最小示例：
 * - stateRef 用来承载 localStorage 的数据（可理解为页面的“数据源”）
 * - 每次打卡/装备后，重新读取并覆盖 stateRef，从而触发 Vue 的响应式刷新
 */

import { computed, ref, watch, onMounted } from 'vue'
import dayjs from 'dayjs'
import Topbar from '../components/ui/Topbar.vue'
import { currentUser } from '../lib/auth'
import {
  REWARDS,
  getCheckinState,
  canCheckIn,
  doCheckIn,
  equipReward,
  getUnlockedRewards,
} from '../lib/checkin'

const me = computed(() => currentUser())
const email = computed(() => me.value?.email || '')

// stateRef：页面的“单一真相来源”（single source of truth）
const stateRef = ref(getCheckinState(email.value))

// 如果用户切换账号（理论上很少发生，但做个保险），重新拉取该账号的打卡数据
watch(email, () => refresh())

function refresh() {
  // refresh 的意义：当 localStorage 被更新后，我们重新读取一次并更新到页面
  stateRef.value = getCheckinState(email.value)
}

// 今日
const todayStr = dayjs().format('YYYY-MM-DD')
const canToday = computed(() => canCheckIn(email.value))
const state = computed(() => stateRef.value)

// 奖励列表
const rewards = REWARDS
const unlocked = computed(() => getUnlockedRewards(email.value))

function isUnlocked(id) {
  return (state.value.unlocked || []).includes(id)
}

function onCheckin() {
  // 1) 执行打卡
  doCheckIn(email.value)
  // 2) 刷新页面状态
  refresh()
}

function toggleEquip(type, id) {
  // 点击“装备/卸下”
  // - 如果当前已经装备了这个 id，则卸下（设为 null）
  // - 否则装备它
  if (type === 'frame') {
    equipReward(email.value, { frame: state.value?.equipped?.frame === id ? null : id })
  }
  if (type === 'badge') {
    equipReward(email.value, { badge: state.value?.equipped?.badge === id ? null : id })
  }
  refresh()
}

// ======= 里程碑进度条（找下一个还没解锁的奖励） =======
const nextMilestone = computed(() => {
  const s = Number(state.value.streak || 0)
  return rewards.find(r => s < r.milestone) || null
})

const progressPct = computed(() => {
  const s = Number(state.value.streak || 0)
  const m = nextMilestone.value?.milestone
  if (!m) return 100
  return Math.max(0, Math.min(100, (s / m) * 100))
})

const progressLabel = computed(() => {
  const m = nextMilestone.value?.milestone
  if (!m) return '所有奖励已解锁 ✅'
  return `${state.value.streak} / ${m}`
})

// ======= 日历渲染（本月/上月/下月切换） =======
// monthCursor 是“当前要展示的月份”，用 dayjs 对象表示。
const monthCursor = ref(dayjs().startOf('month'))
const monthTitle = computed(() => monthCursor.value.format('YYYY 年 MM 月'))

const weekNames = ['一', '二', '三', '四', '五', '六', '日']

/**
 * 计算日历需要的格子：
 * - 一般月历会显示 6 行 x 7 列（42 个格子），包含上月/下月溢出部分。
 * - 这样布局稳定，不会出现某些月只有 4 行导致页面跳动。
 */
const calDays = computed(() => {
  const start = monthCursor.value.startOf('month')
  // dayjs().day()：0=周日...6=周六，这里我们希望周一为第一列
  // 所以把 day() 做一个转换：周一=>0, 周日=>6
  const dow = (start.day() + 6) % 7
  const gridStart = start.subtract(dow, 'day')
  return Array.from({ length: 42 }).map((_, i) => {
    const d = gridStart.add(i, 'day')
    return {
      dateStr: d.format('YYYY-MM-DD'),
      day: d.date(),
      inMonth: d.month() === monthCursor.value.month(),
      isToday: d.format('YYYY-MM-DD') === todayStr,
    }
  })
})

function cellClass(d) {
  const checked = (state.value.days || []).includes(d.dateStr)
  return [
    d.inMonth ? 'in' : 'out',
    checked ? 'checked' : '',
    d.isToday ? 'today' : '',
  ].join(' ')
}

function prevMonth() {
  monthCursor.value = monthCursor.value.subtract(1, 'month').startOf('month')
}

function nextMonth() {
  monthCursor.value = monthCursor.value.add(1, 'month').startOf('month')
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
