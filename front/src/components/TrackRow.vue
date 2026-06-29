<script setup>
import { computed } from 'vue'
import { usePlayerStore } from '@/stores/player'
import { formatDuration } from '@/utils/format'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  track: { type: Object, required: true },
  index: { type: Number, default: null },
  context: { type: Array, default: null },
  canAdd: { type: Boolean, default: false },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false },
  canRemove: { type: Boolean, default: false },
})
const emit = defineEmits(['add', 'edit', 'delete', 'remove'])

const player = usePlayerStore()
const isCurrent = computed(() => player.isCurrent(props.track))
const isPlaying = computed(() => isCurrent.value && player.isPlaying)

function onPlay() {
  if (isCurrent.value) player.toggle()
  else player.playTrack(props.track, props.context)
}
</script>

<template>
  <div class="row" :class="{ current: isCurrent }" @dblclick="onPlay">
    <div class="lead">
      <span v-if="index !== null && !isCurrent" class="num">{{ index + 1 }}</span>
      <div v-if="isPlaying" class="eq"><i></i><i></i><i></i></div>
      <button class="play" @click="onPlay">
        <AppIcon :name="isPlaying ? 'pause' : 'play'" :size="16" />
      </button>
    </div>

    <div class="main">
      <span class="title" :class="{ accent: isCurrent }">{{ track.title || track.fileName }}</span>
      <span class="artist muted">{{ track.artist || 'Artiste inconnu' }}</span>
    </div>

    <div class="album dim">{{ track.album || '—' }}</div>
    <div class="genre"><span v-if="track.genre" class="chip">{{ track.genre }}</span></div>
    <div class="time muted">{{ formatDuration(track.duration) }}</div>

    <div class="actions">
      <button v-if="canAdd" class="btn-icon" title="Ajouter à la sélection" @click="emit('add', track)">
        <AppIcon name="plus" :size="18" />
      </button>
      <button v-if="canRemove" class="btn-icon" title="Retirer" @click="emit('remove', track)">
        <AppIcon name="close" :size="18" />
      </button>
      <button v-if="canEdit" class="btn-icon" title="Modifier" @click="emit('edit', track)">
        <AppIcon name="edit" :size="17" />
      </button>
      <button v-if="canDelete" class="btn-icon danger" title="Supprimer" @click="emit('delete', track)">
        <AppIcon name="trash" :size="17" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.row {
  display: grid;
  grid-template-columns: 44px 2.4fr 1.5fr 1fr 56px auto;
  align-items: center;
  gap: 14px;
  padding: 9px 14px;
  border-radius: 12px;
  transition: background 0.15s;
}
.row:hover { background: var(--surface); }
.row.current { background: var(--accent-soft); }

.lead { position: relative; width: 44px; height: 36px; display: grid; place-items: center; }
.num { color: var(--muted); font-size: 14px; font-variant-numeric: tabular-nums; }
.play {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--accent-grad);
  color: #2a1709;
  display: none;
  place-items: center;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(216, 116, 63, 0.4);
}
.row:hover .play { display: grid; }
.row:hover .num { display: none; }
.row.current .play { display: grid; }

.main { display: flex; flex-direction: column; min-width: 0; }
.title { font-weight: 600; font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.title.accent { color: var(--accent); }
.artist { font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.album, .genre { font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.time { font-size: 13px; text-align: right; font-variant-numeric: tabular-nums; }

.actions { display: flex; gap: 2px; opacity: 0; transition: opacity 0.15s; }
.row:hover .actions { opacity: 1; }
.btn-icon.danger:hover { color: #f1948c; background: rgba(226, 87, 76, 0.12); }

.eq { display: flex; align-items: flex-end; gap: 2px; height: 16px; }
.row:hover .eq { display: none; }
.eq i {
  width: 3px;
  background: var(--accent);
  border-radius: 2px;
  animation: bounce 0.9s ease-in-out infinite;
}
.eq i:nth-child(1) { height: 40%; animation-delay: -0.2s; }
.eq i:nth-child(2) { height: 90%; animation-delay: -0.5s; }
.eq i:nth-child(3) { height: 60%; }
@keyframes bounce { 0%, 100% { transform: scaleY(0.4); } 50% { transform: scaleY(1); } }

@media (max-width: 820px) {
  .row { grid-template-columns: 40px 1fr 56px auto; }
  .album, .genre { display: none; }
}
</style>
