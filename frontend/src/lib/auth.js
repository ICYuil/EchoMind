/**
 * 用户认证模块 - 对接后端 EchoMind 2.0 JWT 认证
 * 保持原有 API 接口不变，不影响其他功能
 */

import { userApi } from '../api/user.js'
import { lsGet, lsSet, lsDel } from './storage'

const KEY_TOKEN = 'ai_token'
const KEY_USER_INFO = 'ai_user_info'

/**
 * 获取当前登录用户
 * @returns {Object|null} 用户信息
 */
export function currentUser() {
  return lsGet(KEY_USER_INFO, null)
}

/**
 * 检查是否已登录
 * @returns {boolean}
 */
export function isAuthed() {
  return !!lsGet(KEY_TOKEN, null)
}

/**
 * 获取 Token
 * @returns {string|null}
 */
export function getToken() {
  return lsGet(KEY_TOKEN, null)
}

/**
 * 获取当前用户ID
 * @returns {number|null}
 */
export function getUserId() {
  const user = lsGet(KEY_USER_INFO, null)
  return user ? user.userId : null
}

/**
 * 用户注册
 * @param {Object} data - { email, username, password }
 * @returns {Promise<Object>} 注册成功的用户信息
 */
export async function register({ email, username, password }) {
  // 前端验证
  email = (email || '').trim().toLowerCase()
  username = (username || '').trim()
  if (!email || !email.includes('@')) throw new Error('请输入有效邮箱')
  if (!username) throw new Error('请输入用户名')
  if (!password || password.length < 6) throw new Error('密码至少 6 位')

  // 调用后端注册接口
  await userApi.register({
    email,
    username,
    password
  })

  // 注册成功后自动登录
  return login({ email, password })
}

/**
 * 用户登录
 * @param {Object} data - { email, password }
 * @returns {Promise<Object>} 登录成功的用户信息
 */
export async function login({ email, password }) {
  // 前端验证
  email = (email || '').trim().toLowerCase()
  if (!email) throw new Error('请输入邮箱')
  if (!password) throw new Error('请输入密码')

  // 调用后端登录接口
  const data = await userApi.login({
    email,
    password
  })

  // 保存 Token 和用户信息
  lsSet(KEY_TOKEN, data.token)
  lsSet(KEY_USER_INFO, {
    userId: data.userId,
    username: data.username,
    email: data.email
  })

  return data
}

/**
 * 用户登出
 */
export function logout() {
  lsDel(KEY_TOKEN)
  lsDel(KEY_USER_INFO)
}

/**
 * 更新用户信息
 * @param {Object} patch - 要更新的字段
 * @returns {Promise<Object>} 更新后的用户信息
 */
export async function updateProfile(patch) {
  const user = currentUser()
  if (!user) throw new Error('未登录')

  // 更新本地存储
  const updated = { ...user, ...patch }
  lsSet(KEY_USER_INFO, updated)
  return updated
}

/**
 * 修改密码
 * @param {Object} data - { oldPassword, newPassword }
 * @returns {Promise<void>}
 */
export async function changePassword({ oldPassword, newPassword }) {
  const user = currentUser()
  if (!user) throw new Error('未登录')
  if (!oldPassword) throw new Error('请输入旧密码')
  if (!newPassword || newPassword.length < 6) throw new Error('新密码至少 6 位')

  // TODO: 后端需要实现修改密码接口
  throw new Error('修改密码功能暂未实现')
}

/**
 * 删除账号
 * @param {Object} data - { password }
 * @returns {Promise<void>}
 */
export async function deleteAccount({ password }) {
  const user = currentUser()
  if (!user) throw new Error('未登录')
  if (!password) throw new Error('请输入密码')

  // TODO: 后端需要实现删除账号接口
  throw new Error('删除账号功能暂未实现')
}

// 为了向后兼容，导出旧版函数名（如果有其他文件使用）
export { logout as signOut }
