<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

// Liste editable de valeurs (puces) avec un petit selecteur "+ Ajouter".
// Utilise pour les filtres inclure / exclure (artistes, albums).
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  options: { type: Array, default: () => [] },
  addLabel: { type: String, default: '+ Ajouter' },
  emptyLabel: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

// Options encore disponibles (non deja choisies).
const available = computed(() => props.options.filter((o) => !props.modelValue.includes(o)))

function add(e) {
  const v = e.target.value
  if (v && !props.modelValue.includes(v)) {
    emit('update:modelValue', [...props.modelValue, v])
  }
  e.target.value = ''
}
function remove(v) {
  emit('update:modelValue', props.modelValue.filter((x) => x !== v))
}
</script>

<template>
  <div class="tag-select">
    <div v-if="modelValue.length" class="tags">
      <span v-for="v in modelValue" :key="v" class="tag">
        <span class="tag-label">{{ v }}</span>
        <button type="button" class="tag-x" :title="`Retirer ${v}`" @click="remove(v)">
          <AppIcon name="close" :size="11" />
        </button>
      </span>
    </div>
    <p v-else-if="emptyLabel" class="tag-empty muted">{{ emptyLabel }}</p>

    <select v-if="available.length" class="select tag-add" @change="add">
      <option value="">{{ addLabel }}</option>
      <option v-for="o in available" :key="o" :value="o">{{ o }}</option>
    </select>
  </div>
</template>

<style scoped>
.tag-select { display: flex; flex-direction: column; gap: 8px; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 6px 5px 11px; border-radius: 999px;
  background: var(--surface-2); border: 1px solid var(--border);
  font-size: 12px; color: var(--text); font-weight: 500;
}
.tag-label { line-height: 1; }
.tag-x {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; padding: 0; border: none; border-radius: 50%;
  background: var(--surface-3); color: var(--text-dim); cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.tag-x:hover { background: var(--danger); color: #fff; }
.tag-empty { font-size: 12px; font-style: italic; margin: 0; }
.tag-add { font-size: 13px; padding: 9px 12px; }
</style>
