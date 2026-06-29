<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { playlistsApi } from '@/api/playlists'
import { usePlayerStore } from '@/stores/player'
import { useToastStore } from '@/stores/toast'
import { formatTotal } from '@/utils/format'
import VinylDisc from '@/components/VinylDisc.vue'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const player = usePlayerStore()
const toast = useToastStore()

const playlists = ref([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    playlists.value = await playlistsApi.list()
  } catch {
    toast.error('Impossible de charger les playlists')
  } finally {
    loading.value = false
  }
}
onMounted(load)

function play(p) {
  if (p.tracks?.length) player.playQueue(p.tracks, 0)
  else toast.info('Cette playlist est vide')
}
</script>

<template>
  <div class="page">
    <header class="head">
      <div>
        <p class="eyebrow">Votre bibliothèque</p>
        <h1 class="display">Mes playlists</h1>
        <p class="muted sub">{{ playlists.length }} playlist{{ playlists.length > 1 ? 's' : '' }}</p>
      </div>
      <RouterLink to="/generate" class="btn btn-primary"><AppIcon name="sliders" :size="18" /> Nouvelle playlist</RouterLink>
    </header>

    <div v-if="loading" class="grid">
      <div v-for="i in 4" :key="i" class="skeleton" style="height: 240px"></div>
    </div>

    <div v-else-if="!playlists.length" class="empty card">
      <div class="empty-disc">🎶</div>
      <h3 class="display">Pas encore de playlist</h3>
      <p class="muted">Générez votre première sélection sur mesure.</p>
      <RouterLink to="/generate" class="btn btn-primary"><AppIcon name="sliders" :size="18" /> Générer une playlist</RouterLink>
    </div>

    <div v-else class="grid">
      <article v-for="p in playlists" :key="p.id" class="pcard card" @click="router.push(`/playlists/${p.id}`)">
        <div class="cover">
          <VinylDisc :size="116" :label="p.name" />
          <button class="play" @click.stop="play(p)"><AppIcon name="play" :size="20" /></button>
        </div>
        <div class="pmeta">
          <h3>{{ p.name }}</h3>
          <p class="muted">{{ p.tracks.length }} morceaux · {{ formatTotal(p.totalDuration) }}</p>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; margin: 0 auto; }
.head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 26px; }
.head h1 { font-size: 42px; margin: 4px 0; }
.sub { font-size: 14px; }

.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 18px; }

.pcard { padding: 18px; cursor: pointer; transition: transform 0.18s, box-shadow 0.18s, background 0.18s; }
.pcard:hover { transform: translateY(-4px); box-shadow: var(--shadow); background: var(--surface-2); }

.cover {
  position: relative;
  height: 150px;
  border-radius: var(--radius-sm);
  display: grid;
  place-items: center;
  margin-bottom: 14px;
  overflow: hidden;
  background:
    radial-gradient(400px 200px at 70% 20%, rgba(216, 116, 63, 0.3), transparent 60%),
    linear-gradient(160deg, var(--surface-3), var(--bg-elev));
}
.cover .play {
  position: absolute;
  right: 14px;
  bottom: 14px;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: none;
  background: var(--accent-grad);
  color: #2a1709;
  display: grid;
  place-items: center;
  cursor: pointer;
  opacity: 0;
  transform: translateY(8px);
  transition: all 0.2s;
  box-shadow: 0 8px 20px rgba(216, 116, 63, 0.45);
}
.pcard:hover .play { opacity: 1; transform: translateY(0); }

.pmeta h3 { font-size: 17px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pmeta p { font-size: 13px; margin: 4px 0 0; }

.empty { text-align: center; padding: 60px 30px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-disc { font-size: 52px; }
.empty h3 { font-size: 24px; }
.empty .btn { margin-top: 8px; }
</style>
