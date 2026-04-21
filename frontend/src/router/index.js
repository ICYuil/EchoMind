import { createRouter, createWebHistory } from 'vue-router'
import { isAuthed } from '../lib/auth'
// 导入新组件
import DashboardNewHome from '../pages/DashboardNewHome.vue'
import Auth from '../pages/Auth.vue'
import AppLayout from '../layouts/AppLayout.vue'

import InterviewRoom from '../pages/InterviewRoom.vue'
import RealisticInterviewRoom from '../pages/RealisticInterviewRoom.vue'
import InterviewHistory from '../pages/InterviewHistory.vue'
import ResumeSelect from '../pages/interview/ResumeSelect.vue'
import KnowledgeBaseSelect from '../pages/interview/KnowledgeBaseSelect.vue'
import InterviewSettings from '../pages/interview/InterviewSettings.vue'
import InterviewLoading from '../pages/interview/InterviewLoading.vue'
import InterviewReportLoading from '../pages/interview/InterviewReportLoading.vue'
import ResumeUpload from '../pages/ResumeUpload.vue'
import ResumeAnalysis from '../pages/ResumeAnalysis.vue'
import ResumeList from '../pages/ResumeList.vue'
import Analytics from '../pages/Analytics.vue'
import QuestionBank from '../pages/QuestionBank.vue'
import Checkin from '../pages/Checkin.vue'
import Profile from '../pages/Profile.vue'
import Settings from '../pages/Settings.vue'
import KnowledgeBase from '../pages/KnowledgeBase.vue'
import KnowledgeBaseChat from '../pages/KnowledgeBaseChat.vue'
import StudyPlan from '../pages/study/StudyPlan.vue'
import NotFound from '../pages/NotFound.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: () => (isAuthed() ? '/app/home' : '/login') },

    { path: '/login', component: Auth, meta: { public: true } },
    { path: '/register', component: Auth, meta: { public: true } },

    {
      path: '/app',
      component: AppLayout,
      children: [
        { path: '', redirect: '/app/home' },
        {
  path: 'home',
  component: DashboardNewHome,
  meta: { title: '主页' },
  children: [
    {
      path: 'interview',
      component: InterviewRoom,
      meta: { title: '面试房间' }
    },
    {
      path: 'interview/select-resume',
      component: ResumeSelect,
      meta: { title: '选择简历' }
    },
    {
      path: 'interview/settings',
      component: InterviewSettings,
      meta: { title: '面试设置' }
    },
    {
      path: 'interview/knowledgebase-select',
      component: KnowledgeBaseSelect,
      meta: { title: '选择知识库' }
    },
    {
      path: 'interview/room',
      component: InterviewRoom,
      meta: { title: '面试房间' }
    },
    {
      path: 'interview/room-realistic',
      component: RealisticInterviewRoom,
      meta: { title: '真实面试模拟' }
    },
    {
      path: 'interview/loading',
      component: InterviewLoading,
      meta: { title: '准备面试' }
    },
    {
      path: 'interview/report-loading',
      component: InterviewReportLoading,
      meta: { title: '生成面试报告' }
    },
    {
      path: 'qa',
      component: KnowledgeBaseChat,
      meta: { title: '问答助手' }
    },
    {
      path: 'resumes',
      component: ResumeList,
      meta: { title: '简历库' }
    },
    {
      path: 'study-plan',
      component: StudyPlan,
      meta: { title: '学习计划' }
    }
  ]
},
        { path: 'interview', component: ResumeUpload, meta: { title: '上传简历' } },
        { path: 'resumes', component: ResumeList, meta: { title: '简历库' } },
        { path: 'resume-analysis/:resumeId', component: ResumeAnalysis, meta: { title: '简历分析' } },
        { path: 'records', component: InterviewHistory, meta: { title: '面试记录' } },
        { path: 'analytics', component: Analytics, meta: { title: '面试统计' } },
        { path: 'bank', component: QuestionBank, meta: { title: '题库练习' } },
        { path: 'checkin', component: Checkin, meta: { title: '打卡中心' } },
        { path: 'profile', component: Profile, meta: { title: '个人中心' } },
        { path: 'settings', component: Settings, meta: { title: '设置' } },
        { path: 'knowledge', component: KnowledgeBase, meta: { title: '知识库' } },
        { path: 'knowledge-chat', component: KnowledgeBaseChat, meta: { title: '知识问答' } },
      ],
    },

    // 独立的面试房间页面
    { path: '/interview-room', component: InterviewRoom, meta: { title: '面试房间' } },
    { path: '/interview-room-realistic', component: RealisticInterviewRoom, meta: { title: '真实面试模拟' } },

    { path: '/:pathMatch(.*)*', component: NotFound, meta: { public: true } },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!to.path.startsWith('/app')) return true
  if (isAuthed()) return true
  return { path: '/login', query: { next: to.fullPath } }
})

export default router
