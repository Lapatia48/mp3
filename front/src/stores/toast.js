import { defineStore } from 'pinia'
import { ref } from 'vue'

let nextId = 1

export const useToastStore = defineStore('toast', () => {
  const items = ref([])

  function push(message, type = 'info') {
    const id = nextId++
    items.value.push({ id, message, type })
    setTimeout(() => remove(id), 3600)
  }

  function remove(id) {
    items.value = items.value.filter((t) => t.id !== id)
  }

  const success = (m) => push(m, 'success')
  const error = (m) => push(m, 'error')
  const info = (m) => push(m, 'info')

  return { items, push, remove, success, error, info }
})
