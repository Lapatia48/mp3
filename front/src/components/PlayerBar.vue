<script setup>
import { computed } from 'vue'
import { usePlayerStore } from '@/stores/player'
import { formatDuration } from '@/utils/format'
import VinylDisc from './VinylDisc.vue'
import AppIcon from './AppIcon.vue'

const player = usePlayerStore()
const t = computed(() => player.current)

function onSeek(e) {
  player.seek(Number(e.target.value))
}
function onVolume(e) {
  player.setVolume(Number(e.target.value))
}
</script>

<template>
  <footer class="player">
    <div class="now">
      <VinylDisc :size="58" :spinning="player.isPlaying" :label="t?.title || t?.artist || ''" />
      <div v-if="t" class="info">
        <span class="title">{{ t.title || t.fileName }}</span>
        <span class="artist muted">{{ t.artist || 'Artiste inconnu' }}</span>
      </div>
      <div v-else class="info">
        <span class="title dim">Aucune lecture</span>
        <span class="artist muted">Choisissez un morceau</span>
      </div>
    </div>

    <div class="center">
      <div class="controls">
        <button class="btn-icon" :disabled="!t" @click="player.prev()"><AppIcon name="prev" :size="20" /></button>
        <button class="main-btn" :disabled="!t" @click="player.toggle()">
          <AppIcon :name="player.isPlaying ? 'pause' : 'play'" :size="22" />
        </button>
        <button class="btn-icon" :disabled="!t" @click="player.next()"><AppIcon name="next" :size="20" /></button>
      </div>
      <div class="seek">
        <span class="t muted">{{ formatDuration(player.currentTime) }}</span>
        <input
          class="range"
          type="range"
          min="0"
          :max="player.duration || 0"
          step="0.1"
          :value="player.currentTime"
          :style="{ '--pct': (player.progress * 100) + '%' }"
          :disabled="!t"
          @input="onSeek"
        />
        <span class="t muted">{{ formatDuration(player.duration) }}</span>
      </div>
    </div>

    <div class="right">
      <AppIcon name="volume" :size="18" class="muted" />
      <input
        class="range vol"
        type="range"
        min="0"
        max="1"
        step="0.01"
        :value="player.volume"
        :style="{ '--pct': (player.volume * 100) + '%' }"
        @input="onVolume"
      />
    </div>
  </footer>
</template>

<style scoped>
.player {
  height: var(--player-h);
  display: grid;
  grid-template-columns: 1fr 2fr 1fr;
  align-items: center;
  gap: 20px;
  padding: 0 22px;
  background: linear-gradient(180deg, rgba(31, 24, 20, 0.85), var(--bg-elev));
  border-top: 1px solid var(--border);
  backdrop-filter: blur(12px);
}

.now { display: flex; align-items: center; gap: 14px; min-width: 0; }
.info { display: flex; flex-direction: column; min-width: 0; }
.info .title { font-weight: 600; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.info .artist { font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.center { display: flex; flex-direction: column; gap: 7px; }
.controls { display: flex; align-items: center; justify-content: center; gap: 16px; }
.main-btn {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: grid;
  place-items: center;
  background: var(--accent-grad);
  color: #2a1709;
  box-shadow: 0 8px 20px rgba(216, 116, 63, 0.4);
  transition: transform 0.12s;
}
.main-btn:hover { transform: scale(1.06); }
.main-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.seek { display: flex; align-items: center; gap: 11px; }
.seek .t { font-size: 11px; width: 38px; text-align: center; font-variant-numeric: tabular-nums; }

.right { display: flex; align-items: center; justify-content: flex-end; gap: 10px; }
.vol { max-width: 120px; }

.range {
  -webkit-appearance: none;
  appearance: none;
  flex: 1;
  height: 5px;
  border-radius: 999px;
  background: linear-gradient(to right, var(--accent) var(--pct), var(--surface-3) var(--pct));
  cursor: pointer;
  outline: none;
}
.range::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: var(--text);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
  transition: transform 0.1s;
}
.range:hover::-webkit-slider-thumb { transform: scale(1.2); }
.range:disabled { opacity: 0.6; }

@media (max-width: 820px) {
  .player { grid-template-columns: 1fr 1.6fr; }
  .right { display: none; }
}
</style>
