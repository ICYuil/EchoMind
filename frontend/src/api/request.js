/**
 * 统一的 Axios 请求封装
 * 基于 EchoMind-feature- 的 request.ts 实现
 * 已对接后端 2.0 JWT 认证
 */

import axios from 'axios'
import { getToken } from '../lib/auth.js'

// 后端基础 URL
const baseURL = import.meta.env.PROD ? '' : 'http://localhost:8080'

// 创建 axios 实例
const instance = axios.create({
  baseURL,
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 请求拦截器 - 自动添加 JWT Token
 */
instance.interceptors.request.use(
  (config) => {
    // 使用 auth.js 中的 getToken 获取 Token（正确处理 JSON 解析）
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器
 * 后端约定：所有响应都是 HTTP 200 + Result
 * - code === 200 → 成功，返回 data
 * - code !== 200 → 失败，直接显示 message
 */
instance.interceptors.response.use(
  (response) => {
    const result = response.data

    // 检查是否是 Result 格式
    if (result && typeof result === 'object' && 'code' in result) {
      if (result.code === 200) {
        // 成功：返回 data
        return result.data
      }
      // 失败：直接抛出 message
      return Promise.reject(new Error(result.message || '请求失败'))
    }

    // 非 Result 格式，直接返回
    return result
  },
  (error) => {
    // 处理 401 未授权 - 清除登录状态并跳转到登录页
    if (error.response?.status === 401) {
      localStorage.removeItem('ai_token')
      localStorage.removeItem('ai_user_info')
      // 如果不是在登录页面，则跳转
      if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/register')) {
        window.location.href = '/login'
      }
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }

    // 有响应的情况：后端返回了结果（即使是错误）
    if (error.response) {
      const { data } = error.response
      // 尝试解析 Result 格式
      if (data && typeof data === 'object' && 'code' in data && 'message' in data) {
        return Promise.reject(new Error(data.message || '请求失败'))
      }
      // 响应格式不对
      return Promise.reject(new Error('请求失败，请重试'))
    }

    // 没有响应的情况：真正的网络错误或连接被重置
    const config = error.config
    const isUpload = config && (
      config.url?.includes('/upload') ||
      config.headers?.['Content-Type']?.toString().includes('multipart')
    )

    if (isUpload) {
      return Promise.reject(new Error('上传失败，可能是网络超时或连接中断，请重试'))
    }

    // 其他网络错误
    return Promise.reject(new Error('网络连接失败，请检查网络'))
  }
)

/**
 * 请求工具对象
 */
export const request = {
  /**
   * GET 请求
   * @param {string} url - 请求地址
   * @param {Object} config - 额外配置
   * @returns {Promise<T>}
   */
  get(url, config = {}) {
    return instance.get(url, config)
  },

  /**
   * POST 请求
   * @param {string} url - 请求地址
   * @param {Object} data - 请求数据
   * @param {Object} config - 额外配置
   * @returns {Promise<T>}
   */
  post(url, data = {}, config = {}) {
    return instance.post(url, data, config)
  },

  /**
   * PUT 请求
   * @param {string} url - 请求地址
   * @param {Object} data - 请求数据
   * @param {Object} config - 额外配置
   * @returns {Promise<T>}
   */
  put(url, data = {}, config = {}) {
    return instance.put(url, data, config)
  },

  /**
   * DELETE 请求
   * @param {string} url - 请求地址
   * @param {Object} config - 额外配置
   * @returns {Promise<T>}
   */
  delete(url, config = {}) {
    return instance.delete(url, config)
  },

  /**
   * 文件上传
   * @param {string} url - 请求地址
   * @param {FormData} formData - 表单数据
   * @param {Object} config - 额外配置
   * @returns {Promise<T>}
   */
  upload(url, formData, config = {}) {
    return instance.post(url, formData, {
      timeout: 120000,
      headers: { 'Content-Type': 'multipart/form-data' },
      ...config
    })
  }
}

/**
 * 获取错误信息
 * @param {Error} error - 错误对象
 * @returns {string}
 */
export function getErrorMessage(error) {
  if (error instanceof Error) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return '未知错误'
}

export default request
