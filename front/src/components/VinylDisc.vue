<script setup>
import { computed } from 'vue'
import { initials } from '@/utils/format'

const props = defineProps({
  spinning: { type: Boolean, default: false },
  size: { type: Number, default: 120 },
  label: { type: String, default: '' },
})

const px = computed(() => `${props.size}px`)
</script>

<template>
  <div class="vinyl" :class="{ spinning }" :style="{ width: px, height: px }">
    <div class="grooves"></div>
    <div class="sheen"></div>
    <div class="label">
      <span>{{ initials(label) }}</span>
    </div>
    <div class="hole"></div>
  </div>
</template>

<style scoped>
.vinyl {
  position: relative;
  border-radius: 50%;
  background:
    radial-gradient(circle at 30% 25%, rgba(255, 255, 255, 0.06), transparent 40%),
    #0a0908;
  box-shadow: var(--shadow), inset 0 0 0 1px rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}
.spinning { animation: spin 4s linear infinite; }

.grooves {
  position: absolute;
  inset: 6%;
  border-radius: 50%;
  background: repeating-radial-gradient(
    circle at center,
    #141210 0px,
    #141210 1px,
    #050505 2px,
    #050505 3px
  );
  opacity: 0.85;
}
.sheen {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: conic-gradient(
    from 200deg,
    transparent 0deg,
    rgba(231, 183, 101, 0.16) 40deg,
    transparent 90deg,
    transparent 220deg,
    rgba(255, 255, 255, 0.07) 250deg,
    transparent 300deg
  );
  pointer-events: none;
}
.label {
  position: absolute;
  inset: 33%;
  border-radius: 50%;
  background: var(--accent-grad);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3a2208;
  font-family: var(--display);
  font-weight: 700;
  font-size: calc(v-bind(px) * 0.13);
  box-shadow: inset 0 0 0 2px rgba(0, 0, 0, 0.15);
}
.hole {
  position: absolute;
  inset: 47%;
  border-radius: 50%;
  background: #0a0908;
  box-shadow: inset 0 0 3px rgba(0, 0, 0, 0.8);
}
</style>
