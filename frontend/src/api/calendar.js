/**
 * 日历和打卡 API 模块
 * 对接后端 EchoMind 的 Calendar 和 CardRecord 接口
 */

import request from './request.js'
import { getUserId } from '../lib/auth.js'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

/**
 * 获取当前用户ID（确保是数字）
 * @returns {number|null}
 */
function getValidUserId() {
  const userId = getUserId()
  console.log('[getValidUserId] raw userId:', userId, 'type:', typeof userId)
  if (!userId) {
    console.warn('用户未登录，无法获取用户ID')
    return null
  }
  const numUserId = Number(userId)
  console.log('[getValidUserId] converted userId:', numUserId)
  return numUserId
}

/**
 * 获取某月的所有日历任务（day=0 获取整月）
 * @param {number} year - 年份
 * @param {number} month - 月份 (1-12)
 * @returns {Promise<Array<{day: number, assignment: string}>>}
 */
export async function getCalendar(year, month) {
  const userId = getValidUserId()
  if (!userId) {
    console.warn('获取日历失败：用户未登录')
    return []
  }
  try {
    const requestData = { userId, year, month, day: 0, assignment: '' }
    console.log('[getCalendar] 发送数据:', requestData)
    const data = await request.post(`${API_BASE}/api/interview/sessions/getcalendar`, requestData)
    console.log('[getCalendar] 返回数据:', data)
    if (data && Array.isArray(data)) {
      return data
    }
    return []
  } catch (err) {
    console.error('获取日历失败:', err)
    return []
  }
}

/**
 * 获取某天的日历任务（day>0 获取单天）
 * @param {number} year - 年份
 * @param {number} month - 月份 (1-12)
 * @param {number} day - 日期 (1-31)
 * @returns {Promise<{day: number, assignment: string}|null>}
 */
export async function getCalendarDay(year, month, day) {
  const userId = getValidUserId()
  if (!userId) {
    console.warn('获取日历失败：用户未登录')
    return null
  }
  try {
    const data = await request.post(`${API_BASE}/api/interview/sessions/getcalendar`, { 
      userId, 
      year, 
      month, 
      day, 
      assignment: '' 
    })
    if (data && Array.isArray(data) && data.length > 0) {
      return data[0]
    }
    return null
  } catch (err) {
    console.error('获取日历失败:', err)
    return null
  }
}

/**
 * 设置某天的日历任务
 * @param {Object} data
 * @param {number} data.year - 年份
 * @param {number} data.month - 月份 (1-12)
 * @param {number} data.day - 日期 (1-31)
 * @param {string} data.assignment - 任务内容
 * @returns {Promise<void>}
 */
export async function setCalendar(data) {
  const userId = getValidUserId()
  if (!userId) {
    throw new Error('用户未登录')
  }
  const requestData = { ...data, userId }
  console.log('[setCalendar] 发送数据:', requestData)
  const result = await request.post(`${API_BASE}/api/interview/sessions/setcalendar`, requestData)
  console.log('[setCalendar] 返回结果:', result)
  return result
}

/**
 * 获取某月的打卡记录
 * @param {number} year - 年份
 * @param {number} month - 月份 (1-12)
 * @returns {Promise<number[]>} 打卡日期数组 [1, 3, 5, ...]
 */
export async function getCheckinRecords(year, month) {
  const userId = getValidUserId()
  if (!userId) {
    console.warn('获取打卡记录失败：用户未登录')
    return []
  }
  try {
    const data = await request.post(`${API_BASE}/api/interview/sessions/getcards`, { 
      userId, 
      year, 
      month, 
      day: 0 
    })
    return data || []
  } catch (err) {
    console.error('获取打卡记录失败:', err)
    return []
  }
}

/**
 * 执行打卡
 * @param {number} year - 年份
 * @param {number} month - 月份 (1-12)
 * @param {number} day - 日期 (1-31)
 * @returns {Promise<void>}
 */
export async function doCheckin(year, month, day) {
  const userId = getValidUserId()
  if (!userId) {
    throw new Error('用户未登录')
  }
  await request.post(`${API_BASE}/api/interview/sessions/setcard`, { userId, year, month, day })
}

/**
 * 计算连续打卡天数
 * @param {number[]} records - 本月打卡记录
 * @param {number} today - 今天日期
 * @returns {number} 连续打卡天数
 */
export function calculateStreak(records, today) {
  if (!records || records.length === 0) return 0

  const sortedDays = [...new Set(records)].sort((a, b) => b - a)

  let streak = 0
  let checkDay = today

  for (const day of sortedDays) {
    if (day === checkDay || day === checkDay - 1) {
      streak++
      checkDay = day
    } else if (day < checkDay - 1) {
      break
    }
  }

  return streak
}

/**
 * 获取近一周的打卡记录
 * @param {number[]} records - 本月打卡记录
 * @param {number} today - 今天日期
 * @returns {Array<{day: number, checked: boolean, isToday: boolean}>}
 */
export function getRecentWeekRecords(records, today) {
  const days = []
  for (let i = 6; i >= 0; i--) {
    const day = today - i
    if (day > 0) {
      days.push({
        day,
        checked: records.includes(day),
        isToday: day === today
      })
    }
  }
  return days
}

/**
 * 获取近一周的任务
 * @param {Array} tasks - 日历任务列表
 * @param {number} today - 今天日期
 * @returns {Array}
 */
export function getRecentWeekTasks(tasks, today) {
  const weekDays = []
  for (let i = 6; i >= 0; i--) {
    const day = today - i
    if (day > 0) {
      weekDays.push(day)
    }
  }

  return tasks.filter(task => weekDays.includes(task.day))
}
