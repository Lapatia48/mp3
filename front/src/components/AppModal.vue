<script setup>
import AppIcon from './AppIcon.vue'

defineProps({
  title: { type: String, default: '' },
})
const emit = defineEmits(['close'])
</script>

<template>
  <Transition name="fade">
    <div class="overlay" @mousedown.self="emit('close')">
      <div class="modal card">
        <header>
          <h3 class="display">{{ title }}</h3>
          <button class="btn-icon" @click="emit('close')"><AppIcon name="close" /></button>
        </header>
        <div class="body">
          <slot />
        </div>
        <footer v-if="$slots.footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 150;
  background: rgba(6, 5, 4, 0.6);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.modal {
  width: 100%;
  max-width: 460px;
  padding: 24px;
  animation: pop 0.22s cubic-bezier(0.2, 0.9, 0.3, 1.2);
}
@keyframes pop {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
}
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
header h3 { font-size: 22px; }
.body { display: flex; flex-direction: column; gap: 14px; }
footer {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
