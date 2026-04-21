<template>
  <router-view />
</template>

<script setup>
// Removed App-level transitions to restore the specific internal layout transitions
// as requested by the user: "Restore the curtain effect from bottom-right".
// The previous change moved transitions to App.vue which overrode the AppLayout.vue transitions.
// By reverting App.vue to a simple router-view, the AppLayout.vue transitions will take effect again.
</script>

<style>
/* Global styles can remain */
</style>
