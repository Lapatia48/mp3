<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

// Liste editable de valeurs (puces) avec un petit selecteur "+ Ajouter".
// Utilise pour les filtres inclure / exclure (artistes, albums, genres).
// Les options acceptent soit une chaine, soit un objet { value, label } afin
// d'afficher un libelle lisible (ex. « Album · Artiste ») tout en gardant une
// valeur simple. Avec `allowOnly`, chaque puce expose un bouton « uniquement »
// (v-model:onlyValues) qui marque la valeur comme exclusive.
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  options: { type: Array, default: () => [] },
  addLabel: { type: String, default: '+ Ajouter' },
  emptyLabel: { type: String, default: '' },
  allowOnly: { type: Boolean, default: false },
  onlyValues: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'update:onlyValues'])

// Normalise les options en { value, label }.
const normalized = computed(() =>
  props.options.map((o) => (typeof o === 'string' ? { value: o, label: o } : o)),
)
const labelMap = computed(() => {
  const m = new Map()
  normalized.value.forEach((o) => m.set(o.value, o.label))
  return m
})
function labelFor(v) {
  return labelMap.value.get(v) ?? v
}

// Options encore disponibles (non deja choisies).
const available = computed(() => normalized.value.filter((o) => !props.modelValue.includes(o.value)))

function add(e) {
  const v = e.target.value
  if (v && !props.modelValue.includes(v)) {
    emit('update:modelValue', [...props.modelValue, v])
  }
  e.target.value = ''
}
function remove(v) {
  emit('update:modelValue', props.modelValue.filter((x) => x !== v))
  if (props.allowOnly && props.onlyValues.includes(v)) {
    emit('update:onlyValues', props.onlyValues.filter((x) => x !== v))
  }
}
function isOnly(v) {
  return props.onlyValues.includes(v)
}
function toggleOnly(v) {
  if (isOnly(v)) emit('update:onlyValues', props.onlyValues.filter((x) => x !== v))
  else emit('update:onlyValues', [...props.onlyValues, v])
}
</script>

<template>
  <div class="tag-select">
    <div v-if="modelValue.length" class="tags">
      <span v-for="v in modelValue" :key="v" class="tag" :class="{ 'is-only': allowOnly && isOnly(v) }">
        <button
          v-if="allowOnly"
          type="button"
          class="tag-only"
          :class="{ active: isOnly(v) }"
          :title="isOnly(v)
            ? `Uniquement « ${labelFor(v)} » — cliquer pour revenir en priorité`
            : `Marquer « ${labelFor(v)} » comme uniquement (exclusif)`"
          @click="toggleOnly(v)"
        >
          <AppIcon name="star" :size="12" />
        </button>
        <span class="tag-label">{{ labelFor(v) }}</span>
        <button type="button" class="tag-x" :title="`Retirer ${labelFor(v)}`" @click="remove(v)">
          <AppIcon name="close" :size="11" />
        </button>
      </span>
    </div>
    <p v-else-if="emptyLabel" class="tag-empty muted">{{ emptyLabel }}</p>

    <select v-if="available.length" class="select tag-add" @change="add">
      <option value="">{{ addLabel }}</option>
      <option v-for="o in available" :key="o.value" :value="o.value">{{ o.label }}</option>
    </select>
  </div>
</template>

<style scoped>
.tag-select { display: flex; flex-direction: column; gap: 8px; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 6px 5px 8px; border-radius: 999px;
  background: var(--surface-2); border: 1px solid var(--border);
  font-size: 12px; color: var(--text); font-weight: 500;
}
.tag.is-only {
  background: var(--accent-soft);
  border-color: rgba(231, 183, 101, 0.4);
}
.tag-label { line-height: 1; }
.tag-only {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; padding: 0; border: none; border-radius: 50%;
  background: var(--surface-3); color: var(--text-dim); cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.tag-only:hover { color: var(--accent); }
.tag-only.active { background: var(--accent); color: #2a1709; }
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
