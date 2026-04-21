/**
 * 打卡（签到）功能：纯前端、本地存储版（UI Shell）
 *
 * 设计目标：
 * 1) 不依赖后端 —— 用 localStorage 保存每个用户的打卡状态。
 * 2) 规则清晰 —— 每天最多打卡一次；连续天数（streak）用于解锁奖励。
 * 3) 易扩展 —— 你们以后接后端时，只需要把这里的读写 localStorage 换成 API。
 *
 * 数据结构（每个用户一份）：
 * {
 *   lastDate: 'YYYY-MM-DD' | null,    // 上次打卡日期
 *   streak: number,                   // 连续打卡天数
 *   total: number,                    // 累计打卡天数
 *   days: string[],                   // 最近的打卡日期列表（YYYY-MM-DD），用于日历展示
 *   unlocked: string[],               // 已解锁奖励 id 列表
 *   equipped: { frame: string|null, badge: string|null } // 已装备的外观（头像框/勋章）
 * }
 */

import dayjs from 'dayjs'
import { lsGet, lsSet } from './storage'

// ======= 1) 奖励配置（你可以按需要继续加） =======
// 这里用“连续打卡 streak 达到 N 天”作为解锁条件。
// type 用于区分外观类型：frame（头像挂件/头像框）、badge（勋章/徽章）
export const REWARDS = [
  {
    id: 'frame_bronze',
    milestone: 3,
    type: 'frame',
    name: '青铜头像框',
    icon: '🟤',
    desc: '连续打卡 3 天解锁，可作为头像边框装饰。',
  },
  {
    id: 'frame_orbit',
    milestone: 10,
    type: 'frame',
    name: '星环头像挂件',
    icon: '🪐',
    desc: '连续打卡 10 天解锁，头像右上角会出现星环挂件。',
  },
  {
    id: 'badge_silver',
    milestone: 30,
    type: 'badge',
    name: '30 天银色勋章',
    icon: '🥈',
    desc: '连续打卡 30 天解锁，代表你已经形成稳定学习习惯。',
  },
  {
    id: 'badge_gold',
    milestone: 100,
    type: 'badge',
    name: '100 天金色勋章',
    icon: '🥇',
    desc: '连续打卡 100 天解锁，属于“长期坚持”的成就。',
  },
]

function keyFor(email) {
  // 用 email 作为用户唯一标识，避免不同账号打卡数据串号
  return `ai_checkin_${String(email || '').toLowerCase()}`
}

function defaultState() {
  return {
    lastDate: null,
    streak: 0,
    total: 0,
    days: [],
    unlocked: [],
    equipped: { frame: null, badge: null },
  }
}

export function getCheckinState(email) {
  // 读取用户打卡数据，如果不存在则返回默认值
  return lsGet(keyFor(email), defaultState())
}

function save(email, state) {
  lsSet(keyFor(email), state)
  // 这里发一个自定义事件：用于同一页面内的组件“同步刷新”。
  // 例如：你在“打卡中心”点了打卡，侧边栏的连续天数也能立刻更新。
  try {
    window.dispatchEvent(new CustomEvent('ai-checkin-updated', { detail: { email } }))
  } catch {}
  return state
}

export function canCheckIn(email, now = dayjs()) {
  /**
   * 判断“今天是否还能打卡”
   * - 若 lastDate === todayStr => 今天已打过卡
   */
  const s = getCheckinState(email)
  const todayStr = now.format('YYYY-MM-DD')
  return s.lastDate !== todayStr
}

export function doCheckIn(email, now = dayjs()) {
  /**
   * 执行打卡：
   * 1) 当天只能打一次
   * 2) 如果上次打卡是“昨天”，则 streak + 1；否则 streak 重新变为 1
   * 3) total + 1
   * 4) days 追加今天（用于日历展示），并去重
   * 5) 根据 streak 解锁奖励
   */
  const s = getCheckinState(email)
  const todayStr = now.format('YYYY-MM-DD')
  if (s.lastDate === todayStr) return s

  const yestStr = now.subtract(1, 'day').format('YYYY-MM-DD')
  const nextStreak = s.lastDate === yestStr ? (Number(s.streak || 0) + 1) : 1

  const next = {
    ...defaultState(),
    ...s,
    lastDate: todayStr,
    streak: nextStreak,
    total: Number(s.total || 0) + 1,
  }

  // 维护 days：最多保留最近 120 天用于展示（足够覆盖 100 天奖励）
  const set = new Set([...(next.days || []), todayStr])
  next.days = Array.from(set).sort().slice(-120)

  // 解锁奖励：满足 milestone 且未解锁 => 加入 unlocked
  const unlocked = new Set(next.unlocked || [])
  for (const r of REWARDS) {
    if (next.streak >= r.milestone) unlocked.add(r.id)
  }
  next.unlocked = Array.from(unlocked)

  return save(email, next)
}

export function getUnlockedRewards(email) {
  const s = getCheckinState(email)
  const unlocked = new Set(s.unlocked || [])
  return REWARDS.filter(r => unlocked.has(r.id))
}

export function getLockedRewards(email) {
  const s = getCheckinState(email)
  const unlocked = new Set(s.unlocked || [])
  return REWARDS.filter(r => !unlocked.has(r.id))
}

export function equipReward(email, patch) {
  /**
   * 装备外观：
   * patch = { frame: 'xxx' | null, badge: 'yyy' | null }
   *
   * 注意：这里只做最基本校验：只能装备“已解锁”的奖励。
   */
  const s = getCheckinState(email)
  const unlocked = new Set(s.unlocked || [])

  const next = { ...s, equipped: { ...(s.equipped || { frame:null, badge:null }) } }

  if ('frame' in (patch || {})) {
    const id = patch.frame
    next.equipped.frame = id === null ? null : (unlocked.has(id) ? id : next.equipped.frame)
  }
  if ('badge' in (patch || {})) {
    const id = patch.badge
    next.equipped.badge = id === null ? null : (unlocked.has(id) ? id : next.equipped.badge)
  }

  return save(email, next)
}

export function getEquippedCosmetics(email) {
  /**
   * 给 AvatarChip / UI 用：返回已经装备的“头像框 + 勋章”。
   * 结构：{ frame: rewardObj|null, badge: rewardObj|null }
   */
  const s = getCheckinState(email)
  const frame = REWARDS.find(r => r.id === s?.equipped?.frame) || null
  const badge = REWARDS.find(r => r.id === s?.equipped?.badge) || null
  return { frame, badge }
}
