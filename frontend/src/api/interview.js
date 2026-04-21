/**
 * 面试相关API - 使用统一的 Axios 请求封装
 * 基于 EchoMind-feature- 的 interview.ts 实现
 */

import { request } from './request.js'

/**
 * 面试API - 完全对应 EchoMind-feature- 的 interviewApi
 */
export const interviewApi = {
  /**
   * 创建面试会话
   * POST /api/interview/sessions
   * @param {Object} req - { resumeId, resumeText, jobId, questionCount }
   * @returns {Promise<InterviewSession>}
   */
  async createSession(req) {
    return request.post('/api/interview/sessions', req, {
      timeout: 180000 // 3分钟超时，AI生成问题需要时间
    })
  },

  /**
   * 获取会话信息
   * GET /api/interview/sessions/{sessionId}
   * @param {string} sessionId
   * @returns {Promise<InterviewSession>}
   */
  async getSession(sessionId) {
    return request.get(`/api/interview/sessions/${sessionId}`)
  },

  /**
   * 获取当前问题
   * GET /api/interview/sessions/{sessionId}/question
   * @param {string} sessionId
   * @returns {Promise<CurrentQuestionResponse>}
   */
  async getCurrentQuestion(sessionId) {
    return request.get(`/api/interview/sessions/${sessionId}/question`)
  },

  /**
   * 提交答案
   * POST /api/interview/sessions/{sessionId}/answers
   * @param {Object} req - { sessionId, questionIndex, answer, addQuestionIndex }
   * @returns {Promise<SubmitAnswerResponse>}
   */
  async submitAnswer(req) {
    return request.post(
      `/api/interview/sessions/${req.sessionId}/answers`,
      {
        questionIndex: req.questionIndex,
        answer: req.answer,
        addQuestionIndex: req.addQuestionIndex
      },
      {
        timeout: 180000 // 3分钟超时
      }
    )
  },

  /**
   * 暂存答案（不进入下一题）
   * PUT /api/interview/sessions/{sessionId}/answers
   * @param {Object} req - { sessionId, questionIndex, answer, addQuestionIndex }
   * @returns {Promise<void>}
   */
  async saveAnswer(req) {
    return request.put(`/api/interview/sessions/${req.sessionId}/answers`, {
      questionIndex: req.questionIndex,
      answer: req.answer,
      addQuestionIndex: req.addQuestionIndex
    })
  },

  /**
   * 获取面试报告
   * GET /api/interview/sessions/{sessionId}/report
   * @param {string} sessionId
   * @returns {Promise<InterviewReport>}
   */
  async getReport(sessionId) {
    return request.get(`/api/interview/sessions/${sessionId}/report`, {
      timeout: 180000 // 3分钟超时，AI评估需要时间
    })
  },

  /**
   * 查找未完成的面试会话
   * GET /api/interview/sessions/unfinished/{resumeId}
   * @param {number} resumeId
   * @returns {Promise<InterviewSession|null>}
   */
  async findUnfinishedSession(resumeId) {
    try {
      return await request.get(`/api/interview/sessions/unfinished/${resumeId}`)
    } catch {
      // 如果没有未完成的会话，返回null
      return null
    }
  },

  /**
   * 获取面试历史列表
   * 通过获取所有简历来汇总面试记录（与 EchoMind 前端保持一致）
   * @returns {Promise<InterviewItem[]>}
   */
  async getInterviewHistory() {
    try {
      const { resumeApi } = await import('./resume.js')
      const resumes = await resumeApi.getResumes()
      const allInterviews = []
      
      // 遍历每个简历，获取其面试记录
      for (const resume of resumes) {
        const detail = await resumeApi.getResumeDetail(resume.id)
        if (detail.interviews && detail.interviews.length > 0) {
          detail.interviews.forEach(interview => {
            allInterviews.push({
              ...interview,
              resumeId: resume.id,
              resumeFilename: resume.filename
            })
          })
        }
      }
      
      // 按创建时间倒序排序
      return allInterviews.sort((a, b) => {
        const timeA = new Date(a.createdAt || a.startTime || 0).getTime()
        const timeB = new Date(b.createdAt || b.startTime || 0).getTime()
        return timeB - timeA
      })
    } catch (err) {
      console.error('获取面试历史失败:', err)
      return []
    }
  },

  /**
   * 获取面试详情
   * GET /api/interview/sessions/{sessionId}/details
   * @param {string} sessionId
   * @returns {Promise<InterviewDetail>}
   */
  async getInterviewDetail(sessionId) {
    return request.get(`/api/interview/sessions/${sessionId}/details`)
  },

  /**
   * 提前结束面试（交卷）
   * POST /api/interview/sessions/{sessionId}/complete
   * @param {string} sessionId
   * @returns {Promise<void>}
   */
  async completeInterview(sessionId) {
    return request.post(`/api/interview/sessions/${sessionId}/complete`)
  },

  /**
   * 删除面试会话
   * DELETE /api/interview/sessions/{sessionId}
   * @param {string} sessionId
   * @returns {Promise<void>}
   */
  async deleteSession(sessionId) {
    return request.delete(`/api/interview/sessions/${sessionId}`)
  },

  /**
   * 导出面试报告PDF
   * GET /api/interview/sessions/{sessionId}/export
   * @param {string} sessionId
   * @returns {Promise<Blob>}
   */
  async exportReport(sessionId) {
    return request.get(`/api/interview/sessions/${sessionId}/export`, {
      responseType: 'blob'
    })
  },

  /**
   * 获取每日一题
   * GET api/interview/sessions/getquestion/ (注意：后端路径没有开头的/)
   * @returns {Promise<TestQuestionEntity>}
   */
  async getDailyQuestion() {
    return request.get('api/interview/sessions/getquestion/')
  },

  /**
   * 获取面试分数统计
   * GET api/interview/sessions/getscores/{resumeId} (注意：后端路径没有开头的/)
   * 注意：后端 ScoreQuestionEntity 中字段名为 "socre"（拼写错误），前端已做兼容处理
   * @param {number} resumeId
   * @returns {Promise<Array<{overallScore: number, scoreQuestionEntity: Array<{category: string, score: number}>}>>}
   */
  async getInterviewScores(resumeId) {
    return request.get(`api/interview/sessions/getscores/${resumeId}`)
  }
}

export default interviewApi
