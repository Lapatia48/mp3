<script setup>
import { computed, ref } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  options: { type: Array, default: () => [] },
  addLabel: { type: String, default: 'Rechercher…' },
  emptyLabel: { type: String, default: '' },
  allowOnly: { type: Boolean, default: false },
  onlyValues: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'update:onlyValues'])

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

const available = computed(() => normalized.value.filter((o) => !props.modelValue.includes(o.value)))

const search = ref('')
const open = ref(false)

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return available.value
  return available.value.filter((o) => o.label.toLowerCase().includes(q))
})

function pick(v) {
  if (!props.modelValue.includes(v)) {
    emit('update:modelValue', [...props.modelValue, v])
  }
  search.value = ''
  open.value = false
}

function onBlur() {
  setTimeout(() => {
    open.value = false
    search.value = ''
  }, 160)
}

function remove(v) {
  emit('update:modelValue', props.modelValue.filter((x) => x !== v))
  if (props.allowOnly && props.onlyValues.includes(v)) {
    emit('update:onlyValues', props.onlyValues.filter((x) => x !== v))
  }
}

function isOnly(v) { return props.onlyValues.includes(v) }
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

    <div v-if="available.length" class="search-wrap">
      <div class="search-field" :class="{ focused: open }">
        <AppIcon name="search" :size="13" class="search-ico" />
        <input
          v-model="search"
          type="text"
          :placeholder="addLabel"
          class="search-inp"
          autocomplete="off"
          @focus="open = true"
          @blur="onBlur"
        />
      </div>
      <div v-if="open" class="dropdown">
        <button
          v-for="o in filtered"
          :key="o.value"
          type="button"
          class="drop-item"
          @mousedown.prevent="pick(o.value)"
        >{{ o.label }}</button>
        <p v-if="!filtered.length" class="drop-empty">Aucun résultat</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tag-select { display: flex; flex-direction: column; gap: 8px; position: relative; }

.tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 6px 4px 8px; border-radius: 999px;
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
  background: transparent; color: var(--text-dim); cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.tag-x:hover { background: var(--danger); color: #fff; }
.tag-empty { font-size: 12px; font-style: italic; margin: 0; }

.search-wrap { position: relative; }
.search-field {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 10px;
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: var(--bg);
  transition: border-color 0.15s;
}
.search-field.focused { border-color: var(--border-strong); }
.search-ico { color: var(--text-dim); flex: none; }
.search-inp {
  flex: 1; border: none; background: transparent; outline: none;
  font-size: 13px; color: var(--text);
}
.search-inp::placeholder { color: var(--text-dim); }

.dropdown {
  position: absolute; top: calc(100% + 4px); left: 0; right: 0;
  z-index: 60; max-height: 210px; overflow-y: auto;
  background: var(--bg-elev); border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
}
.drop-item {
  display: block; width: 100%; text-align: left;
  padding: 9px 14px; font-size: 13px; color: var(--text);
  border: none; background: transparent; cursor: pointer;
  border-bottom: 1px solid var(--border);
}
.drop-item:last-child { border-bottom: none; }
.drop-item:hover { background: var(--surface-2); }
.drop-empty { padding: 10px 14px; font-size: 13px; color: var(--text-dim); font-style: italic; margin: 0; }
</style>
