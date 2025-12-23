<template>
  <div v-if="loading" class="loading-container" :class="{ fullscreen }">
    <div class="loading-content">
      <div class="spinner" :style="{ width: size, height: size }">
        <div class="spinner-circle"></div>
      </div>
      <p v-if="text" class="loading-text">{{ text }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  loading?: boolean;
  text?: string;
  size?: string;
  fullscreen?: boolean;
}>();
</script>

<style scoped>
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.loading-container.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.spinner {
  position: relative;
}

.spinner-circle {
  width: 100%;
  height: 100%;
  border: 3px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.dark .loading-container.fullscreen {
  background: rgba(15, 23, 42, 0.9);
}
</style>
