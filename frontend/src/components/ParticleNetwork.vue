<template>
  <canvas ref="canvas" class="particle-network"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvas = ref(null)
let ctx = null
let particles = []
let animationId = null
let mouse = { x: null, y: null }

// 配置参数
const config = {
  particleCount: 60,        // 粒子数量
  connectionDistance: 150,  // 连线距离
  mouseDistance: 200,       // 鼠标影响距离
  speed: 0.5,               // 粒子速度
  particleRadius: 2,        // 粒子半径
  lineWidth: 0.5,           // 连线宽度
}

// 获取当前主题颜色
const getThemeColor = () => {
  const themeId = document.body.getAttribute('data-theme') || '1'
  const colors = {
    '1': { particle: 'rgba(100, 108, 255, 0.8)', line: 'rgba(100, 108, 255, 0.15)' },
    '2': { particle: 'rgba(255, 90, 90, 0.8)', line: 'rgba(255, 90, 90, 0.15)' },
    '3': { particle: 'rgba(54, 211, 153, 0.8)', line: 'rgba(54, 211, 153, 0.15)' },
    '4': { particle: 'rgba(200, 169, 110, 0.8)', line: 'rgba(200, 169, 110, 0.15)' },
    '5': { particle: 'rgba(139, 92, 246, 0.8)', line: 'rgba(139, 92, 246, 0.15)' },
    '6': { particle: 'rgba(100, 108, 255, 0.6)', line: 'rgba(100, 108, 255, 0.1)' },
    '7': { particle: 'rgba(255, 90, 90, 0.6)', line: 'rgba(255, 90, 90, 0.1)' },
    '8': { particle: 'rgba(54, 211, 153, 0.6)', line: 'rgba(54, 211, 153, 0.1)' },
    '9': { particle: 'rgba(200, 169, 110, 0.6)', line: 'rgba(200, 169, 110, 0.1)' },
    '10': { particle: 'rgba(139, 92, 246, 0.6)', line: 'rgba(139, 92, 246, 0.1)' },
  }
  return colors[themeId] || colors['1']
}

class Particle {
  constructor(canvasWidth, canvasHeight) {
    this.x = Math.random() * canvasWidth
    this.y = Math.random() * canvasHeight
    this.vx = (Math.random() - 0.5) * config.speed
    this.vy = (Math.random() - 0.5) * config.speed
    this.radius = Math.random() * config.particleRadius + 1
  }

  update(canvasWidth, canvasHeight) {
    // 鼠标影响
    if (mouse.x !== null && mouse.y !== null) {
      const dx = mouse.x - this.x
      const dy = mouse.y - this.y
      const distance = Math.sqrt(dx * dx + dy * dy)
      
      if (distance < config.mouseDistance) {
        const force = (config.mouseDistance - distance) / config.mouseDistance
        const angle = Math.atan2(dy, dx)
        this.vx -= Math.cos(angle) * force * 0.02
        this.vy -= Math.sin(angle) * force * 0.02
      }
    }

    // 更新位置
    this.x += this.vx
    this.y += this.vy

    // 边界反弹
    if (this.x < 0 || this.x > canvasWidth) this.vx *= -1
    if (this.y < 0 || this.y > canvasHeight) this.vy *= -1

    // 限制速度
    const maxSpeed = config.speed * 2
    this.vx = Math.max(-maxSpeed, Math.min(maxSpeed, this.vx))
    this.vy = Math.max(-maxSpeed, Math.min(maxSpeed, this.vy))
  }

  draw(ctx, color) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
    ctx.fillStyle = color
    ctx.fill()
  }
}

const init = () => {
  if (!canvas.value) return
  
  ctx = canvas.value.getContext('2d')
  resize()
  
  // 创建粒子
  particles = []
  for (let i = 0; i < config.particleCount; i++) {
    particles.push(new Particle(canvas.value.width, canvas.value.height))
  }
  
  animate()
}

const resize = () => {
  if (!canvas.value) return
  canvas.value.width = window.innerWidth
  canvas.value.height = window.innerHeight
}

const drawLine = (p1, p2, distance, colors) => {
  const opacity = (1 - distance / config.connectionDistance) * 0.5
  ctx.beginPath()
  ctx.moveTo(p1.x, p1.y)
  ctx.lineTo(p2.x, p2.y)
  ctx.strokeStyle = colors.line.replace('0.15', opacity.toFixed(2)).replace('0.1', opacity.toFixed(2))
  ctx.lineWidth = config.lineWidth
  ctx.stroke()
}

const animate = () => {
  if (!ctx || !canvas.value) return
  
  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  const colors = getThemeColor()
  
  // 更新和绘制粒子
  particles.forEach(particle => {
    particle.update(canvas.value.width, canvas.value.height)
    particle.draw(ctx, colors.particle)
  })
  
  // 绘制连线
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)
      
      if (distance < config.connectionDistance) {
        drawLine(particles[i], particles[j], distance, colors)
      }
    }
    
    // 与鼠标的连线
    if (mouse.x !== null && mouse.y !== null) {
      const dx = particles[i].x - mouse.x
      const dy = particles[i].y - mouse.y
      const distance = Math.sqrt(dx * dx + dy * dy)
      
      if (distance < config.mouseDistance) {
        drawLine(particles[i], { x: mouse.x, y: mouse.y }, distance, colors)
      }
    }
  }
  
  animationId = requestAnimationFrame(animate)
}

const handleMouseMove = (e) => {
  mouse.x = e.clientX
  mouse.y = e.clientY
}

const handleMouseLeave = () => {
  mouse.x = null
  mouse.y = null
}

onMounted(() => {
  init()
  window.addEventListener('resize', resize)
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseleave', handleMouseLeave)
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', resize)
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseleave', handleMouseLeave)
})
</script>

<style scoped>
.particle-network {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
