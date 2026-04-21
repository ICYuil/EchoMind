<template>
  <div class="page-with-bg">
    <div class="container page-content">
      <div class="card" style="padding:18px;">
        <div style="font-size:18px; font-weight:950;">404</div>
        <div class="muted" style="margin-top:10px;">页面不存在。</div>
        <div class="row gap10" style="margin-top:12px;">
          <RouterLink class="btn primary btn-glow" to="/">回到首页</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'

onMounted(() => {
  document.querySelectorAll('.btn-glow').forEach(btn => {
    btn.addEventListener('mousemove', (e) => {
      const rect = btn.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top
      btn.style.setProperty('--mouse-x', `${x}px`)
      btn.style.setProperty('--mouse-y', `${y}px`)
    })
  })
})
</script>
