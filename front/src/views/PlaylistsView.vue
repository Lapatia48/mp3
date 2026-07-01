<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { playlistsApi } from '@/api/playlists'
import { usePlayerStore } from '@/stores/player'
import { useToastStore } from '@/stores/toast'
import { formatTotal } from '@/utils/format'
import VinylDisc from '@/components/VinylDisc.vue'
import AppIcon from '@/components/AppIcon.vue'
import AppModal from '@/components/AppModal.vue'

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

/* ---- Fusion de playlists (cote client) ---- */
const mergeMode = ref(false)
const selectedIds = ref([])
const showMergeModal = ref(false)
const mergeName = ref('')
const merging = ref(false)

function toggleMergeMode() {
  mergeMode.value = !mergeMode.value
  selectedIds.value = []
}

function isSelected(p) {
  return selectedIds.value.includes(p.id)
}

// En mode fusion, un clic sur une carte (dé)selectionne au lieu d'ouvrir.
function onCardClick(p) {
  if (mergeMode.value) toggleSelect(p)
  else router.push(`/playlists/${p.id}`)
}

function toggleSelect(p) {
  const i = selectedIds.value.indexOf(p.id)
  if (i >= 0) selectedIds.value.splice(i, 1)
  else selectedIds.value.push(p.id)
}

const selectedPlaylists = computed(() =>
  playlists.value.filter((p) => selectedIds.value.includes(p.id)),
)

// Morceaux fusionnes : concatenation des playlists selectionnees, doublons
// retires (on ne garde qu'une occurrence de chaque morceau, par id).
const mergedTracks = computed(() => {
  const seen = new Set()
  const out = []
  for (const p of selectedPlaylists.value) {
    for (const t of p.tracks || []) {
      if (seen.has(t.id)) continue
      seen.add(t.id)
      out.push(t)
    }
  }
  return out
})
const mergedDuration = computed(() =>
  mergedTracks.value.reduce((s, t) => s + (t.duration || 0), 0),
)

function openMerge() {
  if (selectedPlaylists.value.length < 2) {
    toast.info('Sélectionnez au moins deux playlists')
    return
  }
  mergeName.value = selectedPlaylists.value.map((p) => p.name).join(' + ')
  showMergeModal.value = true
}

async function confirmMerge() {
  const trackIds = mergedTracks.value.map((t) => t.id)
  if (!trackIds.length) {
    toast.error('La fusion ne contient aucun morceau')
    return
  }
  merging.value = true
  try {
    const saved = await playlistsApi.save({
      name: mergeName.value.trim() || 'Fusion',
      trackIds,
    })
    toast.success('Playlists fusionnées')
    showMergeModal.value = false
    mergeMode.value = false
    selectedIds.value = []
    await load()
    router.push(`/playlists/${saved.id}`)
  } catch {
    toast.error('Échec de la fusion')
  } finally {
    merging.value = false
  }
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
      <div class="head-actions">
        <button v-if="playlists.length > 1" class="btn" :class="{ 'btn-primary': mergeMode }" @click="toggleMergeMode">
          <AppIcon name="plus" :size="18" /> {{ mergeMode ? 'Annuler la fusion' : 'Fusionner' }}
        </button>
        <RouterLink to="/generate" class="btn btn-primary"><AppIcon name="sliders" :size="18" /> Nouvelle playlist</RouterLink>
      </div>
    </header>

    <!-- Barre de fusion -->
    <div v-if="mergeMode" class="mergebar card">
      <span class="mergebar-info">
        <AppIcon name="check" :size="16" />
        {{ selectedIds.length }} playlist{{ selectedIds.length > 1 ? 's' : '' }} sélectionnée{{ selectedIds.length > 1 ? 's' : '' }}
        <span class="muted"> · les doublons seront fusionnés</span>
      </span>
      <button class="btn btn-primary" :disabled="selectedIds.length < 2" @click="openMerge">
        <AppIcon name="plus" :size="16" /> Fusionner ({{ selectedIds.length }})
      </button>
    </div>

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
      <article
        v-for="p in playlists"
        :key="p.id"
        class="pcard card"
        :class="{ selectable: mergeMode, selected: mergeMode && isSelected(p) }"
        @click="onCardClick(p)"
      >
        <div v-if="mergeMode" class="check" :class="{ on: isSelected(p) }">
          <AppIcon v-if="isSelected(p)" name="check" :size="15" />
        </div>
        <div class="cover">
          <VinylDisc :size="116" :label="p.name" />
          <button v-if="!mergeMode" class="play" @click.stop="play(p)"><AppIcon name="play" :size="20" /></button>
        </div>
        <div class="pmeta">
          <h3>{{ p.name }}</h3>
          <p class="muted">{{ p.tracks.length }} morceaux · {{ formatTotal(p.totalDuration) }}</p>
        </div>
      </article>
    </div>

    <!-- Fusion : nom + apercu -->
    <AppModal v-if="showMergeModal" title="Fusionner les playlists" @close="showMergeModal = false">
      <div class="field">
        <label>Nom de la playlist fusionnée</label>
        <input v-model="mergeName" class="input" placeholder="Fusion" />
      </div>
      <p class="muted merge-summary">
        {{ selectedPlaylists.length }} playlists · {{ mergedTracks.length }} morceaux uniques ·
        {{ formatTotal(mergedDuration) }}
      </p>
      <template #footer>
        <button class="btn btn-ghost" @click="showMergeModal = false">Annuler</button>
        <button class="btn btn-primary" :disabled="merging" @click="confirmMerge">
          <span v-if="merging" class="spinner"></span>
          <template v-else>Fusionner</template>
        </button>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; margin: 0 auto; }
.head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 26px; }
.head h1 { font-size: 42px; margin: 4px 0; }
.sub { font-size: 14px; }
.head-actions { display: flex; gap: 10px; flex-wrap: wrap; }

.mergebar {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 14px 18px; margin-bottom: 20px; flex-wrap: wrap;
}
.mergebar-info { display: inline-flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; }

.merge-summary { margin-top: 12px; }

.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 18px; }

.pcard { position: relative; padding: 18px; cursor: pointer; transition: transform 0.18s, box-shadow 0.18s, background 0.18s; }
.pcard:hover { transform: translateY(-4px); box-shadow: var(--shadow); background: var(--surface-2); }
.pcard.selectable { cursor: pointer; }
.pcard.selected { outline: 2px solid var(--accent); outline-offset: -2px; background: var(--surface-2); }

.check {
  position: absolute; top: 14px; left: 14px; z-index: 2;
  width: 26px; height: 26px; border-radius: 50%;
  display: grid; place-items: center;
  border: 2px solid var(--border-strong); background: var(--bg-elev);
  color: #2a1709;
}
.check.on { background: var(--accent-grad); border-color: transparent; }

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
