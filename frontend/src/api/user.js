/**
 * 用户相关API - 对接后端 EchoMind 2.0 用户认证接口
 */

import { request } from './request.js'

/**
 * 用户API
 */
export const userApi = {
  /**
   * 用户注册
   * POST /api/user/register
   * @param {Object} data - { username, email, password }
   * @returns {Promise<void>}
   */
  async register(data) {
    return request.post('/api/user/register', {
      username: data.username,
      email: data.email,
      password: data.password
    })
  },

  /**
   * 用户登录
   * POST /api/user/login
   * @param {Object} data - { email, password }
   * @returns {Promise<LoginResponse>} - { token, userId, username, email }
   */
  async login(data) {
    return request.post('/api/user/login', {
      email: data.email,
      password: data.password
    })
  },

  /**
   * 获取当前用户信息
   * GET /api/user/info
   * @returns {Promise<UserEntity>} - { userId, username, email, createdAt }
   */
  async getUserInfo() {
    return request.get('/api/user/info')
  }
}

export default userApi
