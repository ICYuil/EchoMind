/**
 * 知识库API - 对接后端 EchoMind 2.0 接口
 * 使用 request.js 统一处理 JWT 认证
 */

import { request } from './request.js';
import { getToken } from '../lib/auth.js';

// 向量化状态
export const VectorStatus = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED'
};

/**
 * 知识库API
 */
export const knowledgeBaseApi = {
  /**
   * 上传知识库文件
   * POST /api/knowledgebase/upload
   * @param {File} file
   * @param {string} name
   * @param {string} category
   * @returns {Promise<Object>} - { id, name, category, fileSize, vectorStatus }
   */
  async uploadKnowledgeBase(file, name, category) {
    const formData = new FormData();
    formData.append('file', file);
    if (name) {
      formData.append('name', name);
    }
    if (category) {
      formData.append('category', category);
    }

    return request.upload('/api/knowledgebase/upload', formData);
  },

  /**
   * 下载知识库文件
   * GET /api/knowledgebase/{id}/download
   * @param {number} id
   * @returns {Promise<Blob>}
   */
  async downloadKnowledgeBase(id) {
    const response = await fetch(
      `${import.meta.env.PROD ? '' : 'http://localhost:8080'}/api/knowledgebase/${id}/download`,
      {
        headers: {
          'Authorization': `Bearer ${getToken() || ''}`
        }
      }
    );
    if (!response.ok) {
      throw new Error('下载失败');
    }
    return response.blob();
  },

  /**
   * 获取所有知识库列表
   * GET /api/knowledgebase/list
   * @param {string} sortBy - 'time' | 'size' | 'access' | 'question'
   * @param {string} vectorStatus - 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
   * @returns {Promise<Array>} - KnowledgeBaseListItemDTO[]
   */
  async getAllKnowledgeBases(sortBy, vectorStatus) {
    const params = new URLSearchParams();
    if (sortBy) {
      params.append('sortBy', sortBy);
    }
    if (vectorStatus) {
      params.append('vectorStatus', vectorStatus);
    }
    const queryString = params.toString();
    return request.get(`/api/knowledgebase/list${queryString ? `?${queryString}` : ''}`);
  },

  /**
   * 获取知识库详情
   * GET /api/knowledgebase/{id}
   * @param {number} id
   * @returns {Promise<Object>} - KnowledgeBaseListItemDTO
   */
  async getKnowledgeBase(id) {
    return request.get(`/api/knowledgebase/${id}`);
  },

  /**
   * 删除知识库
   * DELETE /api/knowledgebase/{id}
   * @param {number} id
   * @returns {Promise<void>}
   */
  async deleteKnowledgeBase(id) {
    return request.delete(`/api/knowledgebase/${id}`);
  },

  // ========== 分类管理 ==========

  /**
   * 获取所有分类
   * GET /api/knowledgebase/categories
   * @returns {Promise<string[]>}
   */
  async getAllCategories() {
    return request.get('/api/knowledgebase/categories');
  },

  /**
   * 根据分类获取知识库
   * GET /api/knowledgebase/category/{category}
   * @param {string} category
   * @returns {Promise<Array>} - KnowledgeBaseListItemDTO[]
   */
  async getByCategory(category) {
    return request.get(`/api/knowledgebase/category/${encodeURIComponent(category)}`);
  },

  /**
   * 获取未分类的知识库
   * GET /api/knowledgebase/uncategorized
   * @returns {Promise<Array>} - KnowledgeBaseListItemDTO[]
   */
  async getUncategorized() {
    return request.get('/api/knowledgebase/uncategorized');
  },

  /**
   * 更新知识库分类
   * PUT /api/knowledgebase/{id}/category
   * @param {number} id
   * @param {string|null} category
   * @returns {Promise<void>}
   */
  async updateCategory(id, category) {
    return request.put(`/api/knowledgebase/${id}/category`, { category });
  },

  // ========== 搜索 ==========

  /**
   * 搜索知识库
   * GET /api/knowledgebase/search?keyword=xxx
   * @param {string} keyword
   * @returns {Promise<Array>} - KnowledgeBaseListItemDTO[]
   */
  async search(keyword) {
    // 优先调用后端搜索接口
    try {
      if (keyword && keyword.trim() !== '') {
        return await request.get(`/api/knowledgebase/search?keyword=${encodeURIComponent(keyword)}`);
      }
      // 空关键字返回全部
      return await this.getAllKnowledgeBases();
    } catch (error) {
      // 后端接口失败时，使用前端过滤作为 fallback
      console.warn('后端搜索接口失败，使用前端过滤:', error.message);
      const allItems = await this.getAllKnowledgeBases();
      if (!keyword || keyword.trim() === '') {
        return allItems;
      }
      const lowerKeyword = keyword.toLowerCase();
      return allItems.filter(item =>
        (item.name && item.name.toLowerCase().includes(lowerKeyword)) ||
        (item.category && item.category.toLowerCase().includes(lowerKeyword))
      );
    }
  },

  // ========== 统计 ==========

  /**
   * 获取知识库统计信息
   * GET /api/knowledgebase/stats
   * @returns {Promise<Object>} - KnowledgeBaseStatsDTO
   */
  async getStatistics() {
    // 优先调用后端统计接口
    try {
      const stats = await request.get('/api/knowledgebase/stats');
      return {
        totalCount: stats.totalCount || 0,
        totalQuestionCount: stats.totalQuestionCount || 0,
        totalAccessCount: stats.totalAccessCount || 0,
        completedCount: stats.completedCount || 0,
        processingCount: stats.processingCount || 0
      };
    } catch (error) {
      // 后端接口失败时，使用前端计算作为 fallback
      console.warn('后端统计接口失败，使用前端计算:', error.message);
      try {
        const allItems = await this.getAllKnowledgeBases();
        return {
          totalCount: allItems.length,
          totalQuestionCount: allItems.reduce((sum, item) => sum + (item.questionCount || 0), 0),
          totalAccessCount: allItems.reduce((sum, item) => sum + (item.accessCount || 0), 0),
          completedCount: allItems.filter(item => item.vectorStatus === 'COMPLETED').length,
          processingCount: allItems.filter(item => item.vectorStatus === 'PROCESSING' || item.vectorStatus === 'PENDING').length
        };
      } catch {
        return {
          totalCount: 0,
          totalQuestionCount: 0,
          totalAccessCount: 0,
          completedCount: 0,
          processingCount: 0
        };
      }
    }
  },

  // ========== 向量化管理 ==========

  /**
   * 重新向量化知识库（手动重试）
   * POST /api/knowledgebase/{id}/revectorize
   * @param {number} id
   * @returns {Promise<void>}
   */
  async revectorize(id) {
    return request.post(`/api/knowledgebase/${id}/revectorize`);
  },

  // ========== 问答查询 ==========

  /**
   * 基于知识库回答问题（非流式）
   * POST /api/knowledgebase/query
   * @param {Object} req - { knowledgeBaseIds: number[], question: string }
   * @returns {Promise<Object>} - { answer, sources }
   */
  async queryKnowledgeBase(req) {
    return request.post('/api/knowledgebase/query', req);
  },

  /**
   * 基于知识库回答问题（流式SSE）
   * POST /api/knowledgebase/query/stream
   * @param {Object} req - { knowledgeBaseIds: number[], question: string }
   * @param {Function} onMessage - 收到消息回调
   * @param {Function} onComplete - 完成回调
   * @param {Function} onError - 错误回调
   * @returns {Promise<void>}
   */
  async queryKnowledgeBaseStream(req, onMessage, onComplete, onError) {
    try {
      const baseURL = import.meta.env.PROD ? '' : 'http://localhost:8080';
      const token = getToken() || '';

      const response = await fetch(`${baseURL}/api/knowledgebase/query/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(req)
      });

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

        // SSE 事件以 \n\n 分隔，但也需要处理单行的情况
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
      onError(error);
    }
  }
};

export default knowledgeBaseApi;
