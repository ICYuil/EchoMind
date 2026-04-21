<template>
  <div class="resume-analysis-page">
    <!-- 面试准备加载状态 -->
    <div v-if="preparingInterview" class="interview-loading-screen">
      <div class="loading-content">
        <div class="loading-ring">
          <div class="ring-ring"></div>
          <div class="ring-ring"></div>
          <div class="ring-ring"></div>
        </div>
        <div class="loading-text">{{ loadingText }}</div>
        <div class="loading-meta">
          <span class="meta-item">{{ getJobLabel(interviewConfig.jobId) }}</span>
          <span class="meta-dot">·</span>
          <span class="meta-item">{{ interviewConfig.questionCount }}题</span>
        </div>
        <div class="loading-progress">
          <div class="progress-track">
            <div class="progress-fill" :style="`width:${loadingProgress}%`"></div>
          </div>
          <span class="progress-text">{{ loadingProgress }}%</span>
        </div>
      </div>
      <button class="loading-cancel" @click="cancelPreparation">
        <svg viewBox="0 0 24 24" width="16" height="16">
          <line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/>
          <line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/>
        </svg>
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-else-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p class="loading-text">加载中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="!resume" class="error-container">
      <p class="error-text">加载失败，请返回重试</p>
      <button class="btn-back" @click="goBack">返回列表</button>
    </div>

    <!-- 主内容 -->
    <template v-else>
      <!-- 顶部导航栏 -->
      <div class="analysis-header">
        <div class="header-left">
          <button class="btn-icon" @click="goBack">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
          </button>
          <div class="header-info">
            <h2 class="header-title">{{ resume.filename }}</h2>
            <p class="header-meta">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
              上传于 {{ formatDate(resume.uploadedAt) }}
            </p>
          </div>
        </div>

        <button 
          class="btn-start-interview"
          @click="showInterviewSettings = true"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            <line x1="12" y1="19" x2="12" y2="23"/>
            <line x1="8" y1="23" x2="16" y2="23"/>
          </svg>
          开始模拟面试
        </button>
      </div>

      <!-- 标签页切换 -->
      <div class="tabs-container">
        <button 
          class="tab-btn"
          :class="{ active: activeTab === 'analysis' }"
          @click="activeTab = 'analysis'"
        >
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 11 12 14 22 4"/>
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
          </svg>
          简历分析
        </button>
        <button 
          class="tab-btn"
          :class="{ active: activeTab === 'interview' }"
          @click="activeTab = 'interview'"
        >
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15C21 15.5304 20.7893 16.0391 20.4142 16.4142C20.0391 16.7893 19.5304 17 19 17H7L3 21V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H19C19.5304 3 20.0391 3.21071 20.4142 3.58579C20.7893 3.96086 21 4.46957 21 5V15Z"/>
          </svg>
          面试记录
          <span v-if="resume.interviews?.length > 0" class="tab-badge">
            {{ resume.interviews.length }}
          </span>
        </button>
      </div>

      <!-- 内容区域 -->
      <div class="content-area">
        <!-- 简历分析标签页 -->
        <div v-if="activeTab === 'analysis'" class="tab-content">
          <!-- 分析中状态 -->
          <div v-if="isProcessing" class="processing-card">
            <div class="processing-icon">
              <div class="spinner-large"></div>
            </div>
            <h3 class="processing-title">AI 正在分析中...</h3>
            <p class="processing-desc">请稍候，AI 正在对您的简历进行深度分析</p>
            <p class="processing-hint">页面将自动刷新显示分析结果</p>
          </div>

          <!-- 分析失败状态 -->
          <div v-else-if="analyzeFailed" class="error-card">
            <div class="error-icon">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
            </div>
            <h3 class="error-title">分析失败</h3>
            <p class="error-desc">AI 服务暂时不可用，请稍后重试</p>
            <div v-if="resume.analyzeError || latestAnalysis?.summary" class="error-detail">
              {{ resume.analyzeError || latestAnalysis.summary }}
            </div>
            <button 
              class="btn-retry"
              @click="handleReanalyze"
              :disabled="reanalyzing"
            >
              <span v-if="reanalyzing" class="spinner-small"></span>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10"/>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
              </svg>
              {{ reanalyzing ? '重新分析中...' : '重新分析' }}
            </button>
          </div>

          <!-- 分析结果 -->
          <template v-else-if="latestAnalysis">
            <!-- 核心评价和雷达图 -->
            <div class="analysis-grid">
              <!-- 核心评价 -->
              <div class="analysis-card core-evaluation">
                <div class="card-header">
                  <div class="header-label">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                      <polyline points="17 6 23 6 23 12"/>
                    </svg>
                    核心评价
                  </div>
                  <button 
                    class="btn-export"
                    @click="exportAnalysisPdf"
                    :disabled="exporting === 'analysis'"
                  >
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                      <polyline points="7 10 12 15 17 10"/>
                      <line x1="12" y1="15" x2="12" y2="3"/>
                    </svg>
                    {{ exporting === 'analysis' ? '导出中...' : '导出分析报告' }}
                  </button>
                </div>

                <div class="evaluation-content">
                  <p class="evaluation-summary">{{ latestAnalysis.summary || '候选人具备扎实的技术基础，有大型项目架构经验。' }}</p>

                  <div class="score-grid">
                    <div class="score-box">
                      <span class="score-label">总分</span>
                      <div class="score-value">
                        <span class="score-number">{{ latestAnalysis.overallScore || 0 }}</span>
                        <span class="score-total">/ 100</span>
                      </div>
                    </div>
                    <div class="score-box">
                      <span class="score-label">分析时间</span>
                      <span class="score-time">{{ formatDateTime(latestAnalysis.analyzedAt) }}</span>
                    </div>
                  </div>

                  <!-- 优势标签 -->
                  <div v-if="latestAnalysis.strengths?.length > 0" class="strengths-section">
                    <span class="section-label">优势亮点</span>
                    <div class="strengths-tags">
                      <span 
                        v-for="(strength, i) in latestAnalysis.strengths" 
                        :key="i"
                        class="strength-tag"
                      >
                        {{ strength }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 多维度评分雷达图 -->
              <div class="analysis-card radar-section">
                <div class="card-header">
                  <div class="header-label">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/>
                      <circle cx="12" cy="12" r="6"/>
                      <circle cx="12" cy="12" r="2"/>
                    </svg>
                    多维度评分
                  </div>
                </div>

                <!-- 雷达图 -->
                <div class="radar-chart-container">
                  <RadarChart v-if="radarData.length > 0" :data="radarData" />
                  <div v-else class="radar-loading">暂无评分数据 (radarData: {{ radarData }})</div>
                </div>

                <!-- 维度得分详情 -->
                <div class="dimension-scores">
                  <div class="debug-info" style="color: red; font-size: 12px; margin-bottom: 10px;">
                    latestAnalysis: {{ latestAnalysis ? '有值' : '无值' }}<br>
                    contentScore: {{ latestAnalysis?.contentScore }}<br>
                    structureScore: {{ latestAnalysis?.structureScore }}
                  </div>
                  <ScoreProgressBar
                    label="内容完整性"
                    :score="latestAnalysis?.contentScore || 0"
                    :maxScore="25"
                    color="emerald"
                  />
                  <ScoreProgressBar
                    label="结构清晰度"
                    :score="latestAnalysis?.structureScore || 0"
                    :maxScore="20"
                    color="cyan"
                  />
                  <ScoreProgressBar
                    label="技能匹配度"
                    :score="latestAnalysis?.skillMatchScore || 0"
                    :maxScore="25"
                    color="blue"
                  />
                  <ScoreProgressBar
                    label="表达专业性"
                    :score="latestAnalysis?.expressionScore || 0"
                    :maxScore="15"
                    color="orange"
                  />
                  <ScoreProgressBar
                    label="项目经验"
                    :score="latestAnalysis?.projectScore || 0"
                    :maxScore="15"
                    color="purple"
                  />
                </div>
              </div>
            </div>

            <!-- 改进建议 -->
            <div v-if="latestAnalysis.suggestions?.length > 0" class="analysis-card suggestions-section">
              <div class="card-header">
                <div class="header-label">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                    <polyline points="22 4 12 14.01 9 11.01"/>
                  </svg>
                  改进建议
                  <span class="suggestion-count">({{ latestAnalysis.suggestions.length }} 条)</span>
                </div>
              </div>

              <div class="suggestions-list">
                <!-- 高优先级 -->
                <div v-if="suggestionsByPriority.high.length > 0" class="suggestion-group">
                  <div class="group-header high">
                    <span class="priority-badge high">高</span>
                    <span class="group-title">高优先级</span>
                  </div>
                  <div 
                    v-for="(suggestion, i) in suggestionsByPriority.high" 
                    :key="'high-'+i"
                    class="suggestion-item"
                  >
                    <span class="category-tag" :class="suggestion.category">{{ suggestion.category }}</span>
                    <div class="suggestion-content">
                      <div class="suggestion-issue">{{ suggestion.issue }}</div>
                      <div class="suggestion-recommendation" v-if="suggestion.recommendation">
                        <span class="recommendation-label">建议：</span>{{ suggestion.recommendation }}
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 中优先级 -->
                <div v-if="suggestionsByPriority.medium.length > 0" class="suggestion-group">
                  <div class="group-header medium">
                    <span class="priority-badge medium">中</span>
                    <span class="group-title">中优先级</span>
                  </div>
                  <div 
                    v-for="(suggestion, i) in suggestionsByPriority.medium" 
                    :key="'medium-'+i"
                    class="suggestion-item"
                  >
                    <span class="category-tag" :class="suggestion.category">{{ suggestion.category }}</span>
                    <div class="suggestion-content">
                      <div class="suggestion-issue">{{ suggestion.issue }}</div>
                      <div class="suggestion-recommendation" v-if="suggestion.recommendation">
                        <span class="recommendation-label">建议：</span>{{ suggestion.recommendation }}
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 低优先级 -->
                <div v-if="suggestionsByPriority.low.length > 0" class="suggestion-group">
                  <div class="group-header low">
                    <span class="priority-badge low">低</span>
                    <span class="group-title">低优先级</span>
                  </div>
                  <div 
                    v-for="(suggestion, i) in suggestionsByPriority.low" 
                    :key="'low-'+i"
                    class="suggestion-item"
                  >
                    <span class="category-tag" :class="suggestion.category">{{ suggestion.category }}</span>
                    <div class="suggestion-content">
                      <div class="suggestion-issue">{{ suggestion.issue }}</div>
                      <div class="suggestion-recommendation" v-if="suggestion.recommendation">
                        <span class="recommendation-label">建议：</span>{{ suggestion.recommendation }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- 无分析结果 -->
          <div v-else class="empty-card">
            <p>暂无分析结果</p>
          </div>
        </div>

        <!-- 面试记录标签页 -->
        <div v-else class="tab-content">
          <div v-if="resume.interviews?.length === 0" class="empty-interviews">
            <p>暂无面试记录</p>
            <button class="btn-start-interview-empty" @click="showInterviewSettings = true">
              开始第一次模拟面试
            </button>
          </div>
          <div v-else class="interviews-list">
            <div 
              v-for="interview in resume.interviews" 
              :key="interview.sessionId"
              class="interview-item"
              @click="viewInterviewDetail(interview.sessionId)"
            >
              <div class="interview-info">
                <span class="interview-id">面试 #{{ interview.sessionId.slice(-6) }}</span>
                <span class="interview-date">{{ formatDate(interview.createdAt) }}</span>
              </div>
              <div class="interview-meta">
                <span class="question-count">{{ interview.totalQuestions }} 题</span>
                <span 
                  class="interview-status"
                  :class="interview.status.toLowerCase()"
                >
                  {{ getStatusText(interview.status) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 面试设置弹窗 -->
    <Teleport to="body">
      <div v-if="showInterviewSettings" class="modal-overlay" @click.self="showInterviewSettings = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>面试设置</h3>
            <button class="btn-close" @click="showInterviewSettings = false">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <div class="modal-body">
            <!-- 面试职位 -->
            <div class="setting-item">
              <label class="setting-label">面试职位</label>
              <select class="setting-select" v-model="interviewConfig.jobId">
                <option value="0">前端开发</option>
                <option value="1">后端开发</option>
                <option value="2">测试工程师</option>
              </select>
              <p class="setting-hint">选择目标职位，系统会生成相关的技术问题</p>
            </div>

            <!-- 题目数量 -->
            <div class="setting-item">
              <label class="setting-label">题目数量</label>
              <div class="question-count-options">
                <button 
                  v-for="count in questionCountOptions"
                  :key="count"
                  class="count-btn"
                  :class="{ active: interviewConfig.questionCount === count }"
                  @click="interviewConfig.questionCount = count"
                >
                  {{ count }}题
                </button>
              </div>
              <p class="setting-hint">选择面试题目数量（4-20题）</p>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-cancel" @click="showInterviewSettings = false">取消</button>
            <button class="btn-confirm" @click="startInterview">开始面试</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 面试详情弹窗 -->
    <Teleport to="body">
      <div v-if="showDetailModal" class="detail-modal-overlay" @click.self="closeDetailModal">
        <div class="detail-modal-content">
          <!-- 加载状态 -->
          <div v-if="detailLoading" class="detail-loading">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>

          <!-- 错误状态 -->
          <div v-else-if="detailError" class="detail-error">
            <p>{{ detailError }}</p>
            <button class="btn secondary" @click="closeDetailModal">关闭</button>
          </div>

          <!-- 详情内容 -->
          <template v-else-if="currentInterview">
            <div class="detail-modal-header">
              <h3>面试详情</h3>
              <div class="header-actions">
                <button 
                  class="btn-icon" 
                  @click="exportDetailPDF"
                  :disabled="detailExporting"
                  title="导出PDF"
                >
                  <svg v-if="!detailExporting" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7 10 12 15 17 10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                  <span v-else class="spinner-small"></span>
                </button>
                <button class="btn-icon" @click="closeDetailModal" title="关闭">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
            </div>

            <div class="detail-modal-body">
              <!-- 评分卡片 -->
              <div v-if="currentInterview.overallScore !== undefined" class="detail-score-card">
                <div class="detail-score-circle">
                  <svg class="detail-score-ring" viewBox="0 0 140 140">
                    <circle cx="70" cy="70" r="62" fill="none" stroke="#f1f5f9" stroke-width="10"/>
                    <circle 
                      cx="70" cy="70" r="62" 
                      fill="none" 
                      :stroke="getScoreColor(currentInterview.overallScore)"
                      stroke-width="10"
                      stroke-linecap="round"
                      :stroke-dasharray="2 * Math.PI * 62"
                      :stroke-dashoffset="2 * Math.PI * 62 - (currentInterview.overallScore / 100) * 2 * Math.PI * 62"
                      class="score-progress"
                    />
                  </svg>
                  <div class="detail-score-value">
                    <span class="detail-score-number">{{ currentInterview.overallScore ?? '-' }}</span>
                    <span class="detail-score-label">总分</span>
                  </div>
                </div>
                <p class="detail-score-feedback">{{ currentInterview.overallFeedback || '表现良好，展示了扎实的技术基础。' }}</p>
              </div>

              <!-- 基本信息 -->
              <div class="detail-info-card">
                <div class="detail-info-row">
                  <span class="info-label">面试ID</span>
                  <span class="info-value">#{{ currentInterview.sessionId?.slice(-8) }}</span>
                </div>
                <div class="detail-info-row">
                  <span class="info-label">面试时间</span>
                  <span class="info-value">{{ formatDateTime(currentInterview.createdAt) }}</span>
                </div>
                <div class="detail-info-row">
                  <span class="info-label">状态</span>
                  <span class="info-value status-badge" :class="currentInterview.status?.toLowerCase()">
                    {{ getStatusText(currentInterview.status) }}
                  </span>
                </div>
              </div>

              <!-- 表现优势 -->
              <div v-if="currentInterview.strengths && currentInterview.strengths.length > 0" class="detail-section">
                <h4 class="section-title success">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                    <polyline points="22,4 12,14.01 9,11.01"/>
                  </svg>
                  表现优势
                </h4>
                <ul class="detail-list">
                  <li v-for="(strength, i) in currentInterview.strengths" :key="i">
                    <span class="list-dot success"></span>
                    <span>{{ strength }}</span>
                  </li>
                </ul>
              </div>

              <!-- 改进建议 -->
              <div v-if="currentInterview.improvements && currentInterview.improvements.length > 0" class="detail-section">
                <h4 class="section-title warning">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                  </svg>
                  改进建议
                </h4>
                <ul class="detail-list">
                  <li v-for="(improvement, i) in currentInterview.improvements" :key="i">
                    <span class="list-dot warning"></span>
                    <span>{{ improvement }}</span>
                  </li>
                </ul>
              </div>

              <!-- 问答详情 -->
              <div v-if="currentInterview.answers?.length > 0" class="detail-section">
                <h4>问答记录详情 ({{ currentInterview.answers.length }}题)</h4>
                <div class="qa-list">
                  <div 
                    v-for="(answer, index) in currentInterview.answers" 
                    :key="index"
                    class="qa-item"
                    :class="{ expanded: expandedQuestions.has(index) }"
                  >
                    <div class="qa-header" @click="toggleQuestion(index)">
                      <div class="qa-title">
                        <span class="qa-number">{{ answer.questionIndex + 1 }}</span>
                        <span v-if="answer.category" class="qa-category">{{ answer.category }}</span>
                        <span class="qa-question">{{ answer.question }}</span>
                      </div>
                      <div class="qa-meta">
                        <span v-if="answer.score !== undefined" class="qa-score" :class="getScoreClass(answer.score)">
                          得分: {{ answer.score }}
                        </span>
                        <svg class="expand-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="6 9 12 15 18 9"/>
                        </svg>
                      </div>
                    </div>
                    <div v-if="expandedQuestions.has(index)" class="qa-content">
                      <div class="qa-answer-section">
                        <span class="section-label">
                          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 15C21 15.5304 20.7893 16.0391 20.4142 16.4142C20.0391 16.7893 19.5304 17 19 17H7L3 21V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H19C19.5304 3 20.0391 3.21071 20.4142 3.58579C20.7893 3.96086 21 4.46957 21 5V15Z"/>
                          </svg>
                          你的回答
                        </span>
                        <p class="section-content" :class="{ 'no-answer': !answer.userAnswer || answer.userAnswer === '不知道' }">
                          "{{ answer.userAnswer || '(未回答)' }}"
                        </p>
                      </div>
                      <div v-if="answer.feedback" class="qa-evaluation-section">
                        <span class="section-label">
                          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M3 3V21H21"/>
                            <path d="M18 9L12 15L9 12L3 18"/>
                          </svg>
                          AI 深度评价
                        </span>
                        <p class="section-content">{{ answer.feedback }}</p>
                      </div>
                      <div v-if="answer.referenceAnswer" class="qa-suggestion-section">
                        <span class="section-label">
                          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="3" y="3" width="18" height="18" rx="2"/>
                            <path d="M9 12H15"/>
                            <path d="M12 9V15"/>
                          </svg>
                          参考答案
                        </span>
                        <pre class="reference-text">{{ answer.referenceAnswer }}</pre>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resumeApi } from '../api/resume.js'
import { interviewApi } from '../api/interview.js'
import RadarChart from '../components/RadarChart.vue'
import ScoreProgressBar from '../components/ScoreProgressBar.vue'

const route = useRoute()
const router = useRouter()
const resumeId = computed(() => route.params.resumeId)

// 状态
const loading = ref(true)
const resume = ref(null)
const activeTab = ref('analysis')
const exporting = ref(null)
const reanalyzing = ref(false)
const showInterviewSettings = ref(false)

// 面试详情弹窗状态
const showDetailModal = ref(false)
const detailLoading = ref(false)
const detailError = ref(null)
const currentInterview = ref(null)
const detailExporting = ref(false)
const expandedQuestions = ref(new Set())

// 面试准备状态
const preparingInterview = ref(false)
const loadingText = ref('正在准备面试...')
const loadingProgress = ref(0)
let loadingTimer = null
let progressTimer = null

// 面试配置
const interviewConfig = ref({
  jobId: '0',
  questionCount: 8
})

const questionCountOptions = [4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]

// 计算属性
const latestAnalysis = computed(() => {
  return resume.value?.analyses?.[0]
})

const isProcessing = computed(() => {
  if (!resume.value) return false
  return resume.value.analyzeStatus === 'PENDING' ||
         resume.value.analyzeStatus === 'PROCESSING' ||
         (resume.value.analyzeStatus === undefined && !latestAnalysis.value)
})

const analyzeFailed = computed(() => {
  if (!latestAnalysis.value) return false
  const hasErrorKeywords = latestAnalysis.value.summary && (
    latestAnalysis.value.summary.includes('I/O error') ||
    latestAnalysis.value.summary.includes('分析过程中出现错误') ||
    latestAnalysis.value.summary.includes('简历分析失败') ||
    latestAnalysis.value.summary.includes('Remote host terminated') ||
    latestAnalysis.value.summary.includes('handshake')
  )
  return resume.value?.analyzeStatus === 'FAILED' ||
         (latestAnalysis.value.overallScore < 10 && latestAnalysis.value.summary) ||
         hasErrorKeywords
})

const radarData = computed(() => {
  if (!latestAnalysis.value) {
    return []
  }

  const data = [
    { subject: '内容完整性', score: latestAnalysis.value.contentScore || 0, fullMark: 25 },
    { subject: '结构清晰度', score: latestAnalysis.value.structureScore || 0, fullMark: 20 },
    { subject: '技能匹配度', score: latestAnalysis.value.skillMatchScore || 0, fullMark: 25 },
    { subject: '表达专业性', score: latestAnalysis.value.expressionScore || 0, fullMark: 15 },
    { subject: '项目经验', score: latestAnalysis.value.projectScore || 0, fullMark: 15 }
  ]
  return data
})

const suggestionsByPriority = computed(() => {
  if (!latestAnalysis.value?.suggestions) {
    return { high: [], medium: [], low: [] }
  }
  
  const suggestions = latestAnalysis.value.suggestions
  return {
    high: suggestions.filter(s => s.priority === '高'),
    medium: suggestions.filter(s => s.priority === '中'),
    low: suggestions.filter(s => s.priority === '低')
  }
})

// 方法
const loadResumeDetail = async () => {
  try {
    const data = await resumeApi.getResumeDetail(resumeId.value)
    resume.value = data
  } catch (err) {
    console.error('加载简历详情失败', err)
  } finally {
    loading.value = false
  }
}

// 静默加载（用于轮询）
const loadResumeDetailSilent = async () => {
  try {
    const data = await resumeApi.getResumeDetail(resumeId.value)
    resume.value = data
  } catch (err) {
    console.error('加载简历详情失败', err)
  }
}

// 轮询
let pollTimer = null
const startPolling = () => {
  if (pollTimer) return
  pollTimer = setInterval(() => {
    if (isProcessing.value) {
      loadResumeDetailSilent()
    }
  }, 5000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 重新分析
const handleReanalyze = async () => {
  try {
    reanalyzing.value = true
    await resumeApi.reanalyze(resumeId.value)
    await loadResumeDetailSilent()
  } catch (err) {
    console.error('重新分析失败', err)
  } finally {
    reanalyzing.value = false
  }
}

// 导出PDF
const exportAnalysisPdf = async () => {
  try {
    exporting.value = 'analysis'
    const blob = await resumeApi.exportAnalysisPdf(resumeId.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `简历分析报告_${resume.value?.filename || resumeId.value}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch (err) {
    alert('导出失败，请重试')
  } finally {
    exporting.value = null
  }
}

// 开始面试 - 先显示加载界面，再进入面试
const startInterview = () => {
  showInterviewSettings.value = false
  preparingInterview.value = true
  loadingProgress.value = 0
  loadingText.value = '正在准备面试...'
  
  // 模拟加载进度
  progressTimer = setInterval(() => {
    if (loadingProgress.value < 90) {
      loadingProgress.value += Math.random() * 15
      if (loadingProgress.value > 90) loadingProgress.value = 90
    }
    
    // 更新加载文本
    if (loadingProgress.value < 30) {
      loadingText.value = '正在分析简历...'
    } else if (loadingProgress.value < 60) {
      loadingText.value = '正在生成面试题目...'
    } else if (loadingProgress.value < 85) {
      loadingText.value = '正在初始化面试环境...'
    } else {
      loadingText.value = '即将开始面试...'
    }
  }, 300)
  
  // 2秒后跳转到面试页面
  loadingTimer = setTimeout(() => {
    loadingProgress.value = 100
    clearInterval(progressTimer)
    
    // 将简历文本和配置传递到面试页面
    const query = {
      resumeId: resumeId.value,
      resumeText: resume.value?.resumeText || '',
      jobId: interviewConfig.value.jobId,
      questionCount: interviewConfig.value.questionCount
    }
    
    router.push({
      path: '/interview-room',
      query
    })
  }, 2000)
}

// 取消准备
const cancelPreparation = () => {
  preparingInterview.value = false
  if (loadingTimer) {
    clearTimeout(loadingTimer)
    loadingTimer = null
  }
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

// 获取职位标签
const getJobLabel = (jobId) => {
  const jobMap = {
    '0': '前端开发',
    '1': '后端开发',
    '2': '测试工程师'
  }
  return jobMap[jobId] || '前端开发'
}

// 查看面试详情
const viewInterviewDetail = async (sessionId) => {
  showDetailModal.value = true
  detailLoading.value = true
  detailError.value = null
  currentInterview.value = null
  expandedQuestions.value = new Set()
  
  try {
    const detail = await interviewApi.getInterviewDetail(sessionId)
    currentInterview.value = detail
  } catch (err) {
    console.error('加载面试详情失败', err)
    detailError.value = err.message || '加载失败'
  } finally {
    detailLoading.value = false
  }
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  currentInterview.value = null
  detailError.value = null
  expandedQuestions.value = new Set()
}

// 切换问题展开/收起
const toggleQuestion = (index) => {
  const newSet = new Set(expandedQuestions.value)
  if (newSet.has(index)) {
    newSet.delete(index)
  } else {
    newSet.add(index)
  }
  expandedQuestions.value = newSet
}

// 从弹窗导出PDF
const exportDetailPDF = async () => {
  if (!currentInterview.value) return
  
  detailExporting.value = true
  try {
    const blob = await interviewApi.exportReport(currentInterview.value.sessionId)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `面试报告_${currentInterview.value.sessionId.slice(-8)}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch (err) {
    alert('导出失败，请重试')
  } finally {
    detailExporting.value = false
  }
}

// 返回
const goBack = () => {
  router.push('/app/records')
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'INTERRUPTED': '已中断'
  }
  return statusMap[status] || status
}

// 获取评分颜色类
const getScoreClass = (score) => {
  if (score >= 80) return 'high'
  if (score >= 60) return 'medium'
  return 'low'
}

// 获取评分颜色值
const getScoreColor = (score) => {
  if (score >= 80) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

// 生命周期
onMounted(() => {
  loadResumeDetail()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.resume-analysis-page {
  width: 100%;
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

/* 面试准备加载界面 */
.interview-loading-screen {
  position: fixed;
  inset: 0;
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #1e1b4b 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

.loading-ring {
  position: relative;
  width: 120px;
  height: 120px;
}

.ring-ring {
  position: absolute;
  inset: 0;
  border: 3px solid transparent;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 1.5s linear infinite;
}

.ring-ring:nth-child(1) {
  width: 100%;
  height: 100%;
  animation-duration: 1.5s;
}

.ring-ring:nth-child(2) {
  width: 80%;
  height: 80%;
  top: 10%;
  left: 10%;
  border-top-color: #8b5cf6;
  animation-duration: 1.2s;
  animation-direction: reverse;
}

.ring-ring:nth-child(3) {
  width: 60%;
  height: 60%;
  top: 20%;
  left: 20%;
  border-top-color: #a855f7;
  animation-duration: 0.9s;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 24px;
  font-weight: 600;
  color: white;
  text-align: center;
}

.loading-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.meta-dot {
  opacity: 0.5;
}

.loading-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 300px;
}

.progress-track {
  flex: 1;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  font-weight: 600;
  color: white;
  min-width: 40px;
  text-align: right;
}

.loading-cancel {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.2s;
}

.loading-cancel:hover {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e2e8f0;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  margin-top: 16px;
  color: #64748b;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 错误状态 */
.error-container {
  text-align: center;
  padding: 60px 20px;
}

.error-text {
  color: #dc2626;
  margin-bottom: 16px;
}

.btn-back {
  padding: 10px 24px;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

/* 头部 */
.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-icon {
  width: 40px;
  height: 40px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: #f8fafc;
  color: #334155;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.btn-start-interview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
}

.btn-start-interview:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

/* 标签页 */
.tabs-container {
  display: flex;
  gap: 4px;
  background: white;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 24px;
  width: fit-content;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.tab-btn:hover {
  color: #334155;
}

.tab-btn.active {
  background: #eef2ff;
  color: #6366f1;
}

.tab-badge {
  padding: 2px 8px;
  background: #e0e7ff;
  color: #6366f1;
  border-radius: 10px;
  font-size: 12px;
}

/* 内容区域 */
.content-area {
  min-height: 400px;
}

/* 分析中状态 */
.processing-card {
  background: white;
  border-radius: 16px;
  padding: 60px;
  text-align: center;
}

.processing-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 24px;
  background: #dbeafe;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner-large {
  width: 32px;
  height: 32px;
  border: 3px solid #93c5fd;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.processing-title {
  font-size: 20px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.processing-desc {
  color: #64748b;
  margin-bottom: 8px;
}

.processing-hint {
  font-size: 13px;
  color: #94a3b8;
}

/* 错误状态 */
.error-card {
  background: white;
  border-radius: 16px;
  padding: 60px;
  text-align: center;
}

.error-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 24px;
  background: #fee2e2;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ef4444;
}

.error-title {
  font-size: 20px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.error-desc {
  color: #64748b;
  margin-bottom: 16px;
}

.error-detail {
  padding: 12px 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 13px;
  margin-bottom: 20px;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

.btn-retry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 分析网格 */
.analysis-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

@media (max-width: 900px) {
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}

.analysis-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #64748b;
}

.btn-export {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-export:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

/* 核心评价 */
.evaluation-content {
  background: linear-gradient(135deg, #ecfdf5 0%, #f0fdf4 100%);
  border-radius: 12px;
  padding: 20px;
}

.evaluation-summary {
  font-size: 16px;
  line-height: 1.7;
  color: #1e293b;
  margin-bottom: 20px;
}

.score-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}

.score-box {
  background: white;
  border-radius: 10px;
  padding: 16px;
}

.score-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #10b981;
  margin-bottom: 8px;
}

.score-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-number {
  font-size: 36px;
  font-weight: 800;
  color: #1e293b;
}

.score-total {
  font-size: 14px;
  color: #64748b;
}

.score-time {
  font-size: 13px;
  color: #334155;
}

/* 优势标签 */
.strengths-section {
  background: white;
  border-radius: 10px;
  padding: 16px;
}

.section-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #10b981;
  margin-bottom: 12px;
}

.strengths-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.strength-tag {
  padding: 6px 12px;
  background: #d1fae5;
  color: #047857;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
}

/* 雷达图区域 */
.radar-chart-container {
  height: 280px;
  margin-bottom: 20px;
}

.dimension-scores {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* 改进建议 */
.suggestions-section {
  margin-top: 20px;
}

.suggestion-count {
  font-size: 13px;
  color: #94a3b8;
  font-weight: normal;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.suggestion-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.priority-badge {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.priority-badge.high {
  background: #ef4444;
  color: white;
}

.priority-badge.medium {
  background: #f59e0b;
  color: white;
}

.priority-badge.low {
  background: #3b82f6;
  color: white;
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 10px;
}

.category-tag {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.category-tag.项目 {
  background: #e9d5ff;
  color: #7c3aed;
}

.category-tag.技能 {
  background: #c7d2fe;
  color: #4f46e5;
}

.category-tag.内容 {
  background: #bbf7d0;
  color: #16a34a;
}

.category-tag.格式 {
  background: #fbcfe8;
  color: #db2777;
}

.category-tag.结构 {
  background: #a5f3fc;
  color: #0891b2;
}

.category-tag.表达 {
  background: #fed7aa;
  color: #ea580c;
}

.suggestion-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.suggestion-issue {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  font-weight: 500;
}

.suggestion-recommendation {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  padding: 8px 12px;
  background: #f1f5f9;
  border-radius: 6px;
  border-left: 3px solid #3b82f6;
}

.recommendation-label {
  color: #3b82f6;
  font-weight: 600;
}

.suggestion-text {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
}

/* 面试记录 */
.empty-interviews {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
}

.empty-interviews p {
  color: #64748b;
  margin-bottom: 16px;
}

.btn-start-interview-empty {
  padding: 12px 24px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  cursor: pointer;
}

.interviews-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.interview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.interview-item:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.interview-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.interview-id {
  font-weight: 600;
  color: #1e293b;
}

.interview-date {
  font-size: 13px;
  color: #64748b;
}

.interview-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-count {
  font-size: 13px;
  color: #64748b;
}

.interview-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.interview-status.in_progress {
  background: #dbeafe;
  color: #2563eb;
}

.interview-status.completed {
  background: #d1fae5;
  color: #059669;
}

.interview-status.interrupted {
  background: #fee2e2;
  color: #dc2626;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow: hidden;
  animation: modal-in 0.3s ease;
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.btn-close {
  width: 32px;
  height: 32px;
  background: #f1f5f9;
  border: none;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  cursor: pointer;
}

.modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  overflow-y: auto;
  max-height: 60vh;
}

.setting-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.setting-label {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.setting-options {
  display: flex;
  gap: 8px;
}

.option-btn {
  flex: 1;
  padding: 10px 16px;
  background: #f1f5f9;
  border: 2px solid transparent;
  border-radius: 10px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.option-btn:hover {
  background: #e2e8f0;
}

.option-btn.active {
  background: #eef2ff;
  border-color: #6366f1;
  color: #6366f1;
  font-weight: 600;
}

.setting-select {
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  color: #334155;
  cursor: pointer;
}

.question-count-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.count-btn {
  padding: 8px 14px;
  background: #f1f5f9;
  border: 2px solid transparent;
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.count-btn:hover {
  background: #e2e8f0;
}

.count-btn.active {
  background: #eef2ff;
  border-color: #6366f1;
  color: #6366f1;
  font-weight: 600;
}

.setting-hint {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
}

.btn-cancel {
  padding: 10px 20px;
  background: #f1f5f9;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
}

.btn-confirm {
  padding: 10px 24px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

/* 空状态 */
.empty-card {
  text-align: center;
  padding: 60px;
  background: white;
  border-radius: 16px;
  color: #64748b;
}

/* 面试详情弹窗 */
.detail-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}

.detail-modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 700px;
  max-height: 85vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.detail-modal-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn-icon {
  width: 36px;
  height: 36px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.btn-icon:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.detail-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

.detail-error {
  text-align: center;
  padding: 40px;
}

.detail-info-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.detail-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #e2e8f0;
}

.detail-info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 14px;
  color: #64748b;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.in_progress {
  background: #dbeafe;
  color: #2563eb;
}

.status-badge.completed {
  background: #d1fae5;
  color: #059669;
}

.status-badge.interrupted {
  background: #fee2e2;
  color: #dc2626;
}

.score-value {
  font-weight: 700;
}

.score-value.high {
  color: #059669;
}

.score-value.medium {
  color: #d97706;
}

.score-value.low {
  color: #dc2626;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 16px;
}

.comment-text {
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  margin: 0;
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.qa-item {
  background: #f8fafc;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.qa-item.expanded {
  border-color: #6366f1;
}

.qa-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.qa-header:hover {
  background: #f1f5f9;
}

.qa-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.qa-number {
  width: 28px;
  height: 28px;
  background: #6366f1;
  color: white;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.qa-question {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  flex: 1;
}

.qa-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.qa-score {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.qa-score.high {
  background: #d1fae5;
  color: #059669;
}

.qa-score.medium {
  background: #fef3c7;
  color: #d97706;
}

.qa-score.low {
  background: #fee2e2;
  color: #dc2626;
}

.expand-icon {
  color: #94a3b8;
  transition: transform 0.2s;
}

.qa-item.expanded .expand-icon {
  transform: rotate(180deg);
}

.qa-content {
  padding: 0 16px 16px;
  border-top: 1px solid #e2e8f0;
}

.qa-answer-section,
.qa-evaluation-section,
.qa-suggestion-section {
  padding: 16px 0;
  border-bottom: 1px solid #e2e8f0;
}

.qa-answer-section:last-child,
.qa-evaluation-section:last-child,
.qa-suggestion-section:last-child {
  border-bottom: none;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: block;
  margin-bottom: 8px;
}

.section-content {
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  margin: 0;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  display: inline-block;
}

.btn.secondary {
  padding: 10px 20px;
  background: #f1f5f9;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.btn.secondary:hover {
  background: #e2e8f0;
}

/* 评分卡片 */
.detail-score-card {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  color: white;
}

.detail-score-circle {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 16px;
}

.detail-score-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.score-progress {
  transition: stroke-dashoffset 1s ease;
}

.detail-score-value {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.detail-score-number {
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
}

.detail-score-label {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 4px;
}

.detail-score-feedback {
  font-size: 14px;
  line-height: 1.6;
  opacity: 0.9;
  margin: 0;
}

/* 章节标题 */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px;
}

.section-title.success {
  color: #059669;
}

.section-title.warning {
  color: #d97706;
}

/* 列表样式 */
.detail-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
}

.list-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 8px;
  flex-shrink: 0;
}

.list-dot.success {
  background: #10b981;
}

.list-dot.warning {
  background: #f59e0b;
}

/* 问题分类标签 */
.qa-category {
  padding: 2px 10px;
  background: #eef2ff;
  color: #6366f1;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  flex-shrink: 0;
}

/* 未回答样式 */
.no-answer {
  color: #ef4444;
  font-style: italic;
}

/* 参考答案文本 */
.reference-text {
  font-size: 13px;
  line-height: 1.7;
  color: #334155;
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
}

/* 章节标签带图标 */
.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.section-label svg {
  color: #6366f1;
}
</style>
