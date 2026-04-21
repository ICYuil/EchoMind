<template>
  <div class="card" style="padding:12px 14px;">
    <div class="muted2" style="font-size:12px;">{{ label }}</div>
    <div class="row space center" style="margin-top:6px;">
      <div style="font-size:22px; font-weight: 950; letter-spacing:-0.2px;">{{ value }}</div>
      <div v-if="badge" class="badge">{{ badge }}</div>
    </div>
    <div v-if="hint" class="muted" style="font-size:12px; margin-top:8px;">{{ hint }}</div>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  label: String,
  value: [String, Number],
  badge: String,
  hint: String,
})
</script>
