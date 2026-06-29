<script setup>
import { useToastStore } from '@/stores/toast'
import AppIcon from './AppIcon.vue'

const toast = useToastStore()
const iconFor = (t) => (t === 'success' ? 'check' : t === 'error' ? 'close' : 'music')
</script>

<template>
  <div class="toast-host">
    <TransitionGroup name="slide-up">
      <div v-for="t in toast.items" :key="t.id" class="toast" :class="t.type">
        <AppIcon :name="iconFor(t.type)" :size="16" />
        <span>{{ t.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  right: 22px;
  bottom: calc(var(--player-h) + 22px);
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}
.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--surface-2);
  border: 1px solid var(--border-strong);
  box-shadow: var(--shadow);
  font-size: 14px;
  font-weight: 500;
  max-width: 340px;
}
.toast.success { border-color: rgba(111, 207, 151, 0.4); color: var(--ok); }
.toast.error { border-color: rgba(226, 87, 76, 0.45); color: #f1948c; }
.toast.info { color: var(--accent); border-color: rgba(231, 183, 101, 0.35); }
</style>
