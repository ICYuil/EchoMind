import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useResumeModalStore = defineStore('resumeModal', () => {
  // 打开的弹窗列表
  const modals = ref([])
  
  // 基础 z-index
  const baseZIndex = 1000
  
  // 计算当前最高 z-index
  const maxZIndex = computed(() => {
    if (modals.value.length === 0) return baseZIndex
    return Math.max(...modals.value.map(m => m.zIndex))
  })
  
  // 生成唯一 ID
  let idCounter = 0
  const generateId = () => {
    return `resume-modal-${++idCounter}`
  }
  
  // 计算新弹窗位置（避免重叠）
  const calculatePosition = () => {
    const offset = modals.value.length * 30
    const maxOffset = 200
    const finalOffset = Math.min(offset, maxOffset)
    return {
      x: 100 + finalOffset,
      y: 50 + finalOffset
    }
  }
  
  // 打开弹窗
  const openModal = (resumeId) => {
    // 检查是否已打开
    const existingModal = modals.value.find(m => m.resumeId === resumeId)
    if (existingModal) {
      // 如果已打开，将其置顶
      bringToFront(existingModal.id)
      return existingModal.id
    }
    
    const id = generateId()
    const position = calculatePosition()
    
    modals.value.push({
      id,
      resumeId,
      position,
      zIndex: maxZIndex.value + 1,
      isMinimized: false
    })
    
    return id
  }
  
  // 关闭弹窗
  const closeModal = (id) => {
    const index = modals.value.findIndex(m => m.id === id)
    if (index > -1) {
      modals.value.splice(index, 1)
    }
  }
  
  // 最小化弹窗
  const minimizeModal = (id) => {
    const modal = modals.value.find(m => m.id === id)
    if (modal) {
      modal.isMinimized = true
    }
  }
  
  // 恢复弹窗
  const restoreModal = (id) => {
    const modal = modals.value.find(m => m.id === id)
    if (modal) {
      modal.isMinimized = false
      bringToFront(id)
    }
  }
  
  // 置顶弹窗
  const bringToFront = (id) => {
    const modal = modals.value.find(m => m.id === id)
    if (modal) {
      modal.zIndex = maxZIndex.value + 1
    }
  }
  
  // 更新弹窗位置
  const updatePosition = (id, position) => {
    const modal = modals.value.find(m => m.id === id)
    if (modal) {
      modal.position = position
    }
  }
  
  // 关闭所有弹窗
  const closeAll = () => {
    modals.value = []
  }
  
  return {
    modals,
    maxZIndex,
    openModal,
    closeModal,
    minimizeModal,
    restoreModal,
    bringToFront,
    updatePosition,
    closeAll
  }
})
