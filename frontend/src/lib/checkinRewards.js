/**
 * 打卡奖励系统 - 基于后端打卡数据计算奖励
 * 
 * 设计目标：
 * 1) 基于后端现有的打卡接口 (getCheckinRecords/doCheckin)
 * 2) 计算累计打卡天数和连续打卡天数
 * 3) 根据里程碑解锁奖励
 * 4) 奖励数据存储在 localStorage（纯前端）
 */

import dayjs from 'dayjs'
import { lsGet, lsSet } from './storage'
import { getCheckinRecords } from '../api/calendar'

// ======= 奖励配置 =======
export const CHECKIN_REWARDS = [
  {
    id: 'badge_3',
    milestone: 3,
    type: 'badge',
    name: '初出茅庐',
    icon: '🌱',
    desc: '连续打卡 3 天解锁，开始养成学习习惯。',
  },
  {
    id: 'badge_7',
    milestone: 7,
    type: 'badge',
    name: '坚持不懈',
    icon: '🔥',
    desc: '连续打卡 7 天解锁，一周的坚持值得鼓励！',
  },
  {
    id: 'badge_14',
    milestone: 14,
    type: 'badge',
    name: '习惯养成',
    icon: '💪',
    desc: '连续打卡 14 天解锁，学习习惯正在养成。',
  },
  {
    id: 'badge_30',
    milestone: 30,
    type: 'badge',
    name: '月度达人',
    icon: '📅',
    desc: '连续打卡 30 天解锁，一个月的全勤记录！',
  },
  {
    id: 'badge_60',
    milestone: 60,
    type: 'badge',
    name: '双月坚持',
    icon: '🏆',
    desc: '连续打卡 60 天解锁，两个月的持续努力。',
  },
  {
    id: 'badge_100',
    milestone: 100,
    type: 'badge',
    name: '百日成就',
    icon: '👑',
    desc: '连续打卡 100 天解锁，百日筑基，学习已成习惯！',
  },
]

// 存储 key
const REWARDS_KEY = 'ai_checkin_rewards'

// 默认奖励状态
function defaultRewardsState() {
  return {
    unlocked: [], // 已解锁奖励 id 列表
    equipped: null, // 当前装备的徽章 id
    lastCheckDate: null, // 上次检查日期
    totalDays: 0, // 累计打卡天数（缓存）
    maxStreak: 0, // 最大连续天数（缓存）
  }
}

/**
 * 获取奖励状态
 */
export function getRewardsState() {
  return lsGet(REWARDS_KEY, defaultRewardsState())
}

/**
 * 保存奖励状态
 */
function saveRewardsState(state) {
  lsSet(REWARDS_KEY, state)
  return state
}

/**
 * 计算连续打卡天数（支持跨月）
 * @param {Array<{year: number, month: number, day: number}>} records - 所有打卡记录
 * @returns {number} 当前连续打卡天数
 */
export function calculateTotalStreak(records) {
  if (!records || records.length === 0) return 0

  // 按日期排序（从新到旧）
  const sorted = [...records].sort((a, b) => {
    const dateA = dayjs(`${a.year}-${a.month}-${a.day}`)
    const dateB = dayjs(`${b.year}-${b.month}-${b.day}`)
    return dateB.diff(dateA)
  })

  let streak = 0
  let checkDate = dayjs()
  const todayStr = checkDate.format('YYYY-MM-DD')

  for (const record of sorted) {
    const recordDate = dayjs(`${record.year}-${record.month}-${record.day}`)
    const recordStr = recordDate.format('YYYY-MM-DD')

    // 跳过今天（今天还没打卡不算中断）
    if (recordStr === todayStr) {
      streak++
      checkDate = recordDate.subtract(1, 'day')
      continue
    }

    // 检查是否是连续的
    if (recordStr === checkDate.format('YYYY-MM-DD')) {
      streak++
      checkDate = checkDate.subtract(1, 'day')
    } else if (recordDate.isBefore(checkDate)) {
      // 日期不连续，中断
      break
    }
  }

  return streak
}

/**
 * 获取用户的所有打卡记录（跨月）
 * 目前只获取最近3个月的数据用于计算连续天数
 */
export async function fetchAllCheckinRecords() {
  const records = []
  const now = dayjs()

  // 获取最近3个月的打卡记录
  for (let i = 0; i < 3; i++) {
    const date = now.subtract(i, 'month')
    const year = date.year()
    const month = date.month() + 1

    try {
      const monthRecords = await getCheckinRecords(year, month)
      if (monthRecords && monthRecords.length > 0) {
        monthRecords.forEach(day => {
          records.push({ year, month, day })
        })
      }
    } catch (err) {
      console.error(`获取 ${year}-${month} 打卡记录失败:`, err)
    }
  }

  return records
}

/**
 * 检查并解锁奖励
 * @param {boolean} force - 强制刷新，忽略缓存
 * @returns {Object} { newUnlocks: [], currentStreak: number }
 */
export async function checkAndUnlockRewards(force = false) {
  const state = getRewardsState()
  const today = dayjs().format('YYYY-MM-DD')

  // 如果今天已经检查过且不是强制刷新，直接返回缓存
  if (!force && state.lastCheckDate === today) {
    return {
      newUnlocks: [],
      currentStreak: state.maxStreak,
      totalDays: state.totalDays,
      unlocked: state.unlocked,
    }
  }

  // 获取所有打卡记录
  const records = await fetchAllCheckinRecords()

  // 计算累计打卡天数
  const totalDays = records.length

  // 计算当前连续打卡天数
  const currentStreak = calculateTotalStreak(records)

  // 检查新解锁的奖励
  const newUnlocks = []
  const unlocked = new Set(state.unlocked || [])

  for (const reward of CHECKIN_REWARDS) {
    if (currentStreak >= reward.milestone && !unlocked.has(reward.id)) {
      unlocked.add(reward.id)
      newUnlocks.push(reward)
    }
  }

  // 保存状态
  const newState = {
    ...state,
    unlocked: Array.from(unlocked),
    lastCheckDate: today,
    totalDays,
    maxStreak: Math.max(state.maxStreak || 0, currentStreak),
  }
  saveRewardsState(newState)

  return {
    newUnlocks,
    currentStreak,
    totalDays,
    unlocked: Array.from(unlocked),
  }
}

/**
 * 获取已解锁的奖励
 */
export function getUnlockedRewards() {
  const state = getRewardsState()
  const unlocked = new Set(state.unlocked || [])
  return CHECKIN_REWARDS.filter(r => unlocked.has(r.id))
}

/**
 * 获取未解锁的奖励
 */
export function getLockedRewards() {
  const state = getRewardsState()
  const unlocked = new Set(state.unlocked || [])
  return CHECKIN_REWARDS.filter(r => !unlocked.has(r.id))
}

/**
 * 装备徽章
 * @param {string|null} badgeId - 徽章 id 或 null 卸下
 */
export function equipBadge(badgeId) {
  const state = getRewardsState()
  const unlocked = new Set(state.unlocked || [])

  // 只能装备已解锁的徽章
  if (badgeId && !unlocked.has(badgeId)) {
    return false
  }

  state.equipped = badgeId
  saveRewardsState(state)
  return true
}

/**
 * 获取当前装备的徽章
 */
export function getEquippedBadge() {
  const state = getRewardsState()
  if (!state.equipped) return null
  return CHECKIN_REWARDS.find(r => r.id === state.equipped) || null
}

/**
 * 获取下一个里程碑奖励
 * @param {number} currentStreak - 当前连续天数
 */
export function getNextMilestone(currentStreak) {
  for (const reward of CHECKIN_REWARDS) {
    if (reward.milestone > currentStreak) {
      return reward
    }
  }
  return null
}
