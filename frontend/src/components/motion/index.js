/**
 * Vue Motion 动画组件封装
 * 基于 @vueuse/motion 提供流畅动画效果
 */

export { default as MotionDiv } from './MotionDiv.vue'
export { default as MotionGroup } from './MotionGroup.vue'

// 常用动画预设
export const fadeIn = {
  initial: { opacity: 0 },
  enter: { opacity: 1 },
  transition: { duration: 300 }
}

export const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  enter: { opacity: 1, y: 0 },
  transition: { duration: 500 }
}

export const fadeInDown = {
  initial: { opacity: 0, y: -20 },
  enter: { opacity: 1, y: 0 },
  transition: { duration: 500 }
}

export const fadeInLeft = {
  initial: { opacity: 0, x: -20 },
  enter: { opacity: 1, x: 0 },
  transition: { duration: 500 }
}

export const fadeInRight = {
  initial: { opacity: 0, x: 20 },
  enter: { opacity: 1, x: 0 },
  transition: { duration: 500 }
}

export const scaleIn = {
  initial: { opacity: 0, scale: 0.9 },
  enter: { opacity: 1, scale: 1 },
  transition: { duration: 300 }
}

export const slideIn = {
  initial: { opacity: 0, x: 100 },
  enter: { opacity: 1, x: 0 },
  transition: { type: 'spring', stiffness: 300, damping: 30 }
}

// 弹簧动画配置
export const springConfig = {
  type: 'spring',
  stiffness: 100,
  damping: 10
}

// 页面过渡动画
export const pageTransition = {
  initial: { opacity: 0, y: 20 },
  enter: { opacity: 1, y: 0 },
  transition: { duration: 300 }
}

// 列表项动画（带 stagger）
export const listItem = (index) => ({
  initial: { opacity: 0, y: 20 },
  enter: { opacity: 1, y: 0 },
  transition: { duration: 300, delay: index * 100 }
})

// 卡片悬停效果
export const cardHover = {
  hovered: { scale: 1.02, y: -4 },
  tapped: { scale: 0.98 },
  transition: { type: 'spring', stiffness: 400, damping: 17 }
}

// 按钮点击效果
export const buttonTap = {
  tapped: { scale: 0.95 },
  transition: { duration: 100 }
}
