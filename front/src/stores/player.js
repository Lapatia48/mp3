import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { streamUrl } from '@/api/http'

/**
 * Moteur audio global : un seul element Audio() persiste entre les pages.
 * Gere la file de lecture, l'etat lecture/pause, la progression et le volume.
 */
export const usePlayerStore = defineStore('player', () => {
  const audio = new Audio()
  audio.volume = 0.8

  const queue = ref([])
  const index = ref(-1)
  const current = ref(null)
  const isPlaying = ref(false)
  const currentTime = ref(0)
  const duration = ref(0)
  const volume = ref(0.8)

  audio.addEventListener('timeupdate', () => (currentTime.value = audio.currentTime))
  audio.addEventListener('loadedmetadata', () => (duration.value = audio.duration || 0))
  audio.addEventListener('play', () => (isPlaying.value = true))
  audio.addEventListener('pause', () => (isPlaying.value = false))
  audio.addEventListener('ended', () => next())

  function load(track) {
    if (!track) return
    current.value = track
    audio.src = streamUrl(track.id)
    audio.play().catch(() => {})
  }

  /** Lance une liste de morceaux a partir d'un index. */
  function playQueue(tracks, startIndex = 0) {
    if (!tracks || !tracks.length) return
    queue.value = [...tracks]
    index.value = Math.max(0, startIndex)
    load(queue.value[index.value])
  }

  /** Lance un morceau, eventuellement dans le contexte d'une liste. */
  function playTrack(track, contextQueue = null) {
    if (contextQueue && contextQueue.length) {
      queue.value = [...contextQueue]
      index.value = contextQueue.findIndex((t) => t.id === track.id)
      if (index.value < 0) index.value = 0
    } else {
      queue.value = [track]
      index.value = 0
    }
    load(queue.value[index.value])
  }

  function toggle() {
    if (!current.value) return
    if (audio.paused) audio.play().catch(() => {})
    else audio.pause()
  }

  function next() {
    if (index.value < queue.value.length - 1) {
      index.value++
      load(queue.value[index.value])
    } else {
      isPlaying.value = false
    }
  }

  function prev() {
    if (audio.currentTime > 3) {
      audio.currentTime = 0
      return
    }
    if (index.value > 0) {
      index.value--
      load(queue.value[index.value])
    }
  }

  function seek(time) {
    audio.currentTime = time
  }

  function setVolume(v) {
    volume.value = v
    audio.volume = v
  }

  const progress = computed(() => (duration.value ? currentTime.value / duration.value : 0))
  const isCurrent = (track) => current.value && track && current.value.id === track.id

  return {
    queue, index, current, isPlaying, currentTime, duration, volume, progress,
    playQueue, playTrack, toggle, next, prev, seek, setVolume, isCurrent,
  }
})
