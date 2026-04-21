/**
 * 简历相关 API - 使用统一的 Axios 请求封装
 * 基于 EchoMind-feature- 的接口封装
 */

import { request, getErrorMessage } from './request.js'

/**
 * 简历API - 完全对应 EchoMind-feature- 的 historyApi 中的简历相关接口
 */
export const resumeApi = {
  /**
   * 获取所有简历列表
   * 对应后端 ResumeListItemDTO: { id, filename, fileSize, uploadedAt, accessCount, latestScore, lastAnalyzedAt, interviewCount }
   * @returns {Promise<Array<{id: number, filename: string, fileSize: number, uploadedAt: string, accessCount: number, latestScore: number, lastAnalyzedAt: string, interviewCount: number}>>}
   */
  async getResumes() {
    return request.get('/api/resumes')
  },

  /**
   * 上传简历并获取分析结果
   * @param {File} file - 简历文件
   * @returns {Promise<UploadResponse>}
   */
  async uploadAndAnalyze(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.upload('/api/resumes/upload', formData)
  },

  /**
   * 获取简历详情
   * 对应后端 ResumeDetailDTO: { id, filename, fileSize, contentType, storageUrl, uploadedAt, accessCount, resumeText, analyzeStatus, analyzeError, analyses, interviews }
   * @param {number} resumeId - 简历ID
   * @returns {Promise<{id: number, filename: string, fileSize: number, contentType: string, storageUrl: string, uploadedAt: string, accessCount: number, resumeText: string, analyzeStatus: string, analyzeError: string, analyses: Array, interviews: Array}>}
   */
  async getResumeDetail(resumeId) {
    return request.get(`/api/resumes/${resumeId}/detail`)
  },

  /**
   * 重新分析简历
   * @param {number} resumeId - 简历ID
   * @returns {Promise<void>}
   */
  async reanalyze(resumeId) {
    return request.post(`/api/resumes/${resumeId}/reanalyze`)
  },

  /**
   * 导出简历分析报告为 PDF
   * GET /api/resumes/{id}/export
   * @param {number} resumeId - 简历ID
   * @returns {Promise<Blob>}
   */
  async exportAnalysisPdf(resumeId) {
    return request.get(`/api/resumes/${resumeId}/export`, {
      responseType: 'blob'
    })
  },

  /**
   * 获取简历统计信息
   * 注意：后端 2.0 版本暂不提供此接口，返回模拟数据
   * @returns {Promise<{totalCount: number, totalInterviewCount: number, totalAccessCount: number}>}
   */
  async getStatistics() {
    // 后端 2.0 版本暂无此接口，返回基于现有数据的模拟统计
    try {
      const resumes = await this.getResumes()
      const totalCount = resumes.length
      let totalInterviewCount = 0
      let totalAccessCount = 0

      for (const resume of resumes) {
        totalAccessCount += resume.accessCount || 0
        // 获取简历详情来统计面试次数
        try {
          const detail = await this.getResumeDetail(resume.id)
          if (detail.interviews) {
            totalInterviewCount += detail.interviews.length
          }
        } catch {
          // 忽略单个简历详情获取失败
        }
      }

      return {
        totalCount,
        totalInterviewCount,
        totalAccessCount
      }
    } catch {
      // 如果获取失败，返回零值
      return {
        totalCount: 0,
        totalInterviewCount: 0,
        totalAccessCount: 0
      }
    }
  },

  /**
   * 删除简历
   * DELETE /api/resumes/{id}
   * @param {number} id - 简历ID
   * @returns {Promise<void>}
   */
  async deleteResume(id) {
    return request.delete(`/api/resumes/${id}`)
  },

  /**
   * 健康检查
   * @returns {Promise<{status: string, service: string}>}
   */
  async healthCheck() {
    return request.get('/api/resumes/health')
  }
}

export { getErrorMessage }
export default resumeApi
