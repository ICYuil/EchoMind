<template>
  <canvas ref="canvas" class="particle-canvas"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvas = ref(null)
let ctx = null
let particles = []
let animationId = null

// 粒子类
class Particle {
  constructor(canvasWidth, canvasHeight) {
    this.canvasWidth = canvasWidth
    this.canvasHeight = canvasHeight
    this.reset()
  }

  reset() {
    this.x = Math.random() * this.canvasWidth
    this.y = Math.random() * this.canvasHeight
    this.size = Math.random() * 3 + 2
    this.speedX = (Math.random() - 0.5) * 0.8
    this.speedY = (Math.random() - 0.5) * 0.8
    this.opacity = Math.random() * 0.4 + 0.4
    this.color = this.getRandomColor()
  }

  getRandomColor() {
    const colors = [
      '100, 108, 255',   // 蓝紫
      '13, 148, 136',    // 青绿
      '139, 92, 246',    // 紫色
      '255, 255, 255'    // 白色
    ]
    return colors[Math.floor(Math.random() * colors.length)]
  }

  update() {
    this.x += this.speedX
    this.y += this.speedY

    // 边界检查
    if (this.x < 0 || this.x > this.canvasWidth) this.speedX *= -1
    if (this.y < 0 || this.y > this.canvasHeight) this.speedY *= -1
  }

  draw() {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(${this.color}, ${this.opacity})`
    ctx.fill()
  }
}

// 初始化粒子
const initParticles = () => {
  const width = canvas.value.width
  const height = canvas.value.height
  const particleCount = Math.floor((width * height) / 10000) // 更多粒子

  particles = []
  for (let i = 0; i < particleCount; i++) {
    particles.push(new Particle(width, height))
  }
}

// 绘制连线
const drawConnections = () => {
  const maxDistance = 150
  const maxConnections = 5

  for (let i = 0; i < particles.length; i++) {
    let connections = 0

    for (let j = i + 1; j < particles.length; j++) {
      if (connections >= maxConnections) break

      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < maxDistance) {
        const opacity = (1 - distance / maxDistance) * 0.4
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.strokeStyle = `rgba(100, 108, 255, ${opacity})`
        ctx.lineWidth = 1
        ctx.stroke()
        connections++
      }
    }
  }
}

// 动画循环
const animate = () => {
  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)

  // 更新和绘制粒子
  particles.forEach(particle => {
    particle.update()
    particle.draw()
  })

  // 绘制连线
  drawConnections()

  animationId = requestAnimationFrame(animate)
}

// 调整画布大小
const resizeCanvas = () => {
  canvas.value.width = window.innerWidth
  canvas.value.height = window.innerHeight
  initParticles()
}

onMounted(() => {
  ctx = canvas.value.getContext('2d')
  resizeCanvas()
  animate()

  window.addEventListener('resize', resizeCanvas)
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', resizeCanvas)
})
</script>

<style scoped>
.particle-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
