<template>
  <div class="topbar">
    <div class="tb-left">
      <div class="h1">{{ title }}</div>
      <div class="h2">{{ subtitle }}</div>
    </div>
    <div class="tb-right">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
})
</script>
