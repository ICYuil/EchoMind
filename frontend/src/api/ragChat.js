/**
 * RAG聊天API - 对接后端 EchoMind 2.0 接口
 * 使用 request.js 统一处理 JWT 认证
 */

import { request, getErrorMessage } from './request.js';
import { getToken } from '../lib/auth.js';

/**
 * RAG聊天API
 */
export const ragChatApi = {
  /**
   * 创建新会话
   * POST /api/rag-chat/sessions
   * @param {number[]} knowledgeBaseIds
   * @param {string} title
   * @returns {Promise<Object>} - SessionDTO { id, title, knowledgeBaseIds, createdAt }
   */
  async createSession(knowledgeBaseIds, title) {
    return request.post('/api/rag-chat/sessions', { knowledgeBaseIds, title });
  },

  /**
   * 获取会话列表
   * GET /api/rag-chat/sessions
   * @returns {Promise<Array>} - SessionListItemDTO[]
   */
  async listSessions() {
    return request.get('/api/rag-chat/sessions');
  },

  /**
   * 获取会话详情
   * GET /api/rag-chat/sessions/{sessionId}
   * @param {number} sessionId
   * @returns {Promise<Object>} - SessionDetailDTO
   */
  async getSessionDetail(sessionId) {
    return request.get(`/api/rag-chat/sessions/${sessionId}`);
  },

  /**
   * 更新会话标题
   * PUT /api/rag-chat/sessions/{sessionId}/title
   * @param {number} sessionId
   * @param {string} title
   * @returns {Promise<void>}
   */
  async updateSessionTitle(sessionId, title) {
    return request.put(`/api/rag-chat/sessions/${sessionId}/title`, { title });
  },

  /**
   * 更新会话知识库
   * PUT /api/rag-chat/sessions/{sessionId}/knowledge-bases
   * @param {number} sessionId
   * @param {number[]} knowledgeBaseIds
   * @returns {Promise<void>}
   */
  async updateKnowledgeBases(sessionId, knowledgeBaseIds) {
    return request.put(`/api/rag-chat/sessions/${sessionId}/knowledge-bases`, { knowledgeBaseIds });
  },

  /**
   * 切换会话置顶状态
   * PUT /api/rag-chat/sessions/{sessionId}/pin
   * @param {number} sessionId
   * @returns {Promise<void>}
   */
  async togglePin(sessionId) {
    return request.put(`/api/rag-chat/sessions/${sessionId}/pin`);
  },

  /**
   * 删除会话
   * DELETE /api/rag-chat/sessions/{sessionId}
   * @param {number} sessionId
   * @returns {Promise<void>}
   */
  async deleteSession(sessionId) {
    return request.delete(`/api/rag-chat/sessions/${sessionId}`);
  },

  /**
   * 发送消息（流式SSE）
   * POST /api/rag-chat/sessions/{sessionId}/messages/stream
   * @param {number} sessionId
   * @param {string} question
   * @param {Function} onMessage - 收到消息回调
   * @param {Function} onComplete - 完成回调
   * @param {Function} onError - 错误回调
   * @returns {Promise<void>}
   */
  async sendMessageStream(sessionId, question, onMessage, onComplete, onError) {
    try {
      const baseURL = import.meta.env.PROD ? '' : 'http://localhost:8080';
      const token = getToken() || '';

      const response = await fetch(
        `${baseURL}/api/rag-chat/sessions/${sessionId}/messages/stream`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({ question })
        }
      );

      if (!response.ok) {
        let errorMessage = `请求失败 (${response.status})`;
        try {
          const errorData = await response.json();
          if (errorData && errorData.message) {
            errorMessage = errorData.message;
          }
        } catch {
          // 忽略解析错误
        }
        throw new Error(errorMessage);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('无法获取响应流');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      // 从 SSE 事件中提取内容
      // 后端返回格式: ServerSentEvent<String>，data 字段包含转义的换行符
      const extractEventContent = (event) => {
        if (!event.trim()) return null;

        const lines = event.split('\n');
        const contentParts = [];

        for (const line of lines) {
          if (line.startsWith('data:')) {
            contentParts.push(line.substring(5));
          }
        }

        if (contentParts.length === 0) return null;

        // 后端转义了换行符，需要反转义
        return contentParts.join('')
          .replace(/\\n/g, '\n')
          .replace(/\\r/g, '\r');
      };

      while (true) {
        const { done, value } = await reader.read();

        if (done) {
          if (buffer) {
            const content = extractEventContent(buffer);
            if (content !== null) {
              onMessage(content);
            }
          }
          onComplete();
          break;
        }

        buffer += decoder.decode(value, { stream: true });

        // SSE 事件以 \n\n 分隔
        let newlineIndex = buffer.indexOf('\n\n');
        if (newlineIndex === -1) {
          // 如果没有找到 \n\n，尝试处理单行 data: 格式
          const singleLineIndex = buffer.indexOf('\n');
          if (singleLineIndex !== -1 && buffer.substring(0, singleLineIndex).startsWith('data:')) {
            const line = buffer.substring(0, singleLineIndex);
            const content = extractEventContent(line);
            if (content) {
              onMessage(content);
            }
            buffer = buffer.substring(singleLineIndex + 1);
          }
          continue;
        }

        // 处理完整的事件块
        while (newlineIndex !== -1) {
          const eventBlock = buffer.substring(0, newlineIndex);
          buffer = buffer.substring(newlineIndex + 2);

          const content = extractEventContent(eventBlock);
          if (content !== null) {
            onMessage(content);
          }

          newlineIndex = buffer.indexOf('\n\n');
        }
      }
    } catch (error) {
      onError(new Error(getErrorMessage(error)));
    }
  }
};

export default ragChatApi;
