<template>
  <div class="bottom-bar">
    <!-- 左侧：日历任务备注 -->
    <div class="bottom-left">
      <div class="calendar-tasks">
        <div class="tasks-header">
          <span class="tasks-icon">📋</span>
          <span class="tasks-title">任务备注</span>
          <span v-if="calendarTasks.length > 0" class="tasks-count">{{ calendarTasks.length }}</span>
        </div>
        
        <div class="tasks-list" v-if="calendarTasks.length > 0">
          <div 
            v-for="(task, index) in calendarTasks" 
            :key="task.id || index"
            class="task-item"
            :class="{ 'checked': task.isChecked, 'today': task.isToday }"
          >
            <span class="task-date">{{ task.month }}/{{ task.day }}</span>
            <span class="task-text" :title="task.assignment">{{ task.assignment }}</span>
            <span v-if="task.isChecked" class="task-status">✓</span>
          </div>
        </div>
        
        <div v-else class="tasks-empty">
          <span>暂无任务备注</span>
        </div>
      </div>
    </div>

    <!-- 右侧：时间显示 -->
    <div class="bottom-right">
      <div class="current-time">{{ currentTime }}</div>
      <div class="weather">☀️ 18°</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getCalendar, getCheckinRecords } from '../../api/calendar.js'

// ===== 时间显示 =====
const currentTime = ref('')
let timeTimer = null

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// ===== 日历任务备注 =====
const calendarTasks = ref([])

// 获取日历任务（有备注的日期）
const loadCalendarTasks = async () => {
  try {
    const now = new Date()
    const year = now.getFullYear()
    const month = now.getMonth() + 1
    
    // 获取日历数据
    const calendarData = await getCalendar(year, month)
    // 获取打卡记录
    const checkinData = await getCheckinRecords(year, month)
    
    const checkinSet = new Set()
    if (checkinData && Array.isArray(checkinData)) {
      checkinData.forEach(record => {
        if (record.day) {
          checkinSet.add(record.day)
        }
      })
    }
    
    // 过滤有备注的日期并按时间排序
    let tasks = []
    if (calendarData && Array.isArray(calendarData)) {
      tasks = calendarData
        .filter(item => item.assignment && item.assignment.trim())
        .map(item => ({
          id: item.day, // 使用 day 作为 id
          year: year,   // 使用当前年份
          month: month, // 使用当前月份
          day: item.day,
          assignment: item.assignment,
          isChecked: checkinSet.has(item.day),
          isToday: item.day === now.getDate()
        }))
        .sort((a, b) => {
          // 按日期排序（最新的在前）
          return b.day - a.day
        })
    }
    
    // 只显示最近5条
    calendarTasks.value = tasks.slice(0, 5)
  } catch (err) {
    console.error('获取日历任务失败:', err)
    calendarTasks.value = []
  }
}

let tasksTimer = null

// ===== 生命周期 =====
onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  
  loadCalendarTasks()
  // 每5分钟刷新一次
  tasksTimer = setInterval(loadCalendarTasks, 5 * 60 * 1000)
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
  if (tasksTimer) clearInterval(tasksTimer)
})
</script>

<style scoped>
.bottom-bar {
  height: 48px;
  background: var(--panel);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 1px solid var(--stroke);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  font-size: 13px;
  color: var(--text);
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

/* 左侧日历任务 */
.bottom-left {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.calendar-tasks {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 100%;
}

.tasks-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: var(--panel2);
  border-radius: 16px;
  border: 1px solid var(--stroke);
  white-space: nowrap;
  flex-shrink: 0;
}

.tasks-icon {
  font-size: 14px;
}

.tasks-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
}

.tasks-count {
  font-size: 11px;
  padding: 2px 6px;
  background: var(--brand);
  color: white;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.tasks-list {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow-x: auto;
  padding: 4px 0;
  scrollbar-width: none;
  flex: 1;
}

.tasks-list::-webkit-scrollbar {
  display: none;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: var(--panel2);
  border-radius: 16px;
  border: 1px solid var(--stroke);
  white-space: nowrap;
  transition: all 0.2s;
  cursor: pointer;
}

.task-item:hover {
  border-color: var(--brand);
  background: var(--panel);
}

.task-item.today {
  border-color: var(--brand);
  background: rgba(100, 108, 255, 0.1);
}

.task-item.checked {
  border-color: var(--good);
  background: rgba(34, 197, 94, 0.1);
}

.task-date {
  font-size: 11px;
  font-weight: 600;
  color: var(--brand);
  padding: 2px 6px;
  background: var(--panel);
  border-radius: 8px;
}

.task-text {
  font-size: 12px;
  color: var(--text);
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-status {
  font-size: 12px;
  color: var(--good);
  font-weight: 700;
}

.tasks-empty {
  color: var(--muted2);
  font-size: 12px;
  font-style: italic;
  padding: 4px 0;
}

/* 右侧 - 时间显示 */
.bottom-right {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-left: 24px;
  padding-left: 24px;
  border-left: 1px solid var(--stroke);
}

.current-time {
  font-family: monospace;
  background: var(--panel2);
  padding: 4px 12px;
  border-radius: 20px;
  color: var(--text);
}

.weather {
  color: var(--muted);
}

/* 响应式 */
@media (max-width: 768px) {
  .bottom-bar {
    padding: 0 16px;
  }
  
  .timeline-items {
    gap: 16px;
  }
  
  .timeline-text {
    font-size: 12px;
  }
  
  .timeline-countdown {
    display: none;
  }
  
  .bottom-right {
    gap: 12px;
    margin-left: 12px;
    padding-left: 12px;
  }
}

@media (max-width: 480px) {
  .timeline-text {
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .current-time {
    display: none;  /* 小屏幕隐藏时间 */
  }
}
</style>