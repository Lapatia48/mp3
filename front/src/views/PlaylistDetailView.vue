<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { playlistsApi } from '@/api/playlists'
import { tracksApi } from '@/api/tracks'
import { usePlayerStore } from '@/stores/player'
import { useToastStore } from '@/stores/toast'
import { formatTotal } from '@/utils/format'
import VinylDisc from '@/components/VinylDisc.vue'
import TrackRow from '@/components/TrackRow.vue'
import AppModal from '@/components/AppModal.vue'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const toast = useToastStore()

const id = route.params.id
const playlist = ref(null)
const tracks = ref([])
const library = ref([])
const loading = ref(true)

const total = computed(() => tracks.value.reduce((s, t) => s + (t.duration || 0), 0))
const candidates = computed(() => library.value.filter((t) => !tracks.value.some((s) => s.id === t.id)))

async function load() {
  loading.value = true
  try {
    playlist.value = await playlistsApi.get(id)
    tracks.value = [...playlist.value.tracks]
  } catch {
    toast.error('Playlist introuvable')
    router.push('/playlists')
  } finally {
    loading.value = false
  }
}
onMounted(async () => {
  await load()
  try {
    library.value = await tracksApi.list()
  } catch {
    /* ignore */
  }
})

async function persist(msg) {
  try {
    await playlistsApi.update(id, { name: playlist.value.name, trackIds: tracks.value.map((t) => t.id) })
    if (msg) toast.success(msg)
  } catch {
    toast.error('Échec de la mise à jour')
    await load()
  }
}

function playAll() {
  if (tracks.value.length) player.playQueue(tracks.value, 0)
}
function removeTrack(t) {
  tracks.value = tracks.value.filter((x) => x.id !== t.id)
  persist()
}
function addTrack(t) {
  tracks.value.push(t)
  persist()
}
async function download() {
  try {
    await playlistsApi.download(id, playlist.value.name)
    toast.success('Téléchargement lancé')
  } catch {
    toast.error('Échec du téléchargement')
  }
}

/* rename */
const showRename = ref(false)
const newName = ref('')
function openRename() {
  newName.value = playlist.value.name
  showRename.value = true
}
async function rename() {
  playlist.value.name = newName.value || playlist.value.name
  showRename.value = false
  await persist('Playlist renommée')
}

/* delete */
const showDelete = ref(false)
async function remove() {
  try {
    await playlistsApi.remove(id)
    toast.success('Playlist supprimée')
    router.push('/playlists')
  } catch {
    toast.error('Échec de la suppression')
  }
}

const showAdd = ref(false)
</script>

<template>
  <div class="page" v-if="!loading && playlist">
    <RouterLink to="/playlists" class="back muted"><AppIcon name="chevron" :size="16" style="transform: rotate(180deg)" /> Mes playlists</RouterLink>

    <header class="hero">
      <VinylDisc :size="180" :spinning="player.isPlaying && tracks.some((t) => player.isCurrent(t))" :label="playlist.name" />
      <div class="hero-info">
        <p class="eyebrow">Playlist</p>
        <h1 class="display" @click="openRename">{{ playlist.name }}</h1>
        <p class="muted">{{ tracks.length }} morceaux · {{ formatTotal(total) }}</p>
        <div class="actions">
          <button class="btn btn-primary" :disabled="!tracks.length" @click="playAll"><AppIcon name="play" :size="18" /> Lire</button>
          <button class="btn" :disabled="!tracks.length" @click="download"><AppIcon name="download" :size="18" /> ZIP</button>
          <button class="btn" @click="showAdd = true"><AppIcon name="plus" :size="18" /> Ajouter</button>
          <button class="btn" @click="openRename"><AppIcon name="edit" :size="17" /> Renommer</button>
          <button class="btn btn-danger" @click="showDelete = true"><AppIcon name="trash" :size="17" /> Supprimer</button>
        </div>
      </div>
    </header>

    <div v-if="tracks.length" class="list card">
      <TrackRow
        v-for="(t, i) in tracks"
        :key="t.id"
        :track="t"
        :index="i"
        :context="tracks"
        can-remove
        @remove="removeTrack"
      />
    </div>
    <div v-else class="empty card">
      <div class="empty-disc">🎵</div>
      <p class="muted">Playlist vide. Ajoutez des morceaux.</p>
      <button class="btn btn-primary" @click="showAdd = true"><AppIcon name="plus" :size="16" /> Ajouter des morceaux</button>
    </div>

    <!-- add modal -->
    <AppModal v-if="showAdd" title="Ajouter des morceaux" @close="showAdd = false">
      <div v-if="candidates.length" class="add-list">
        <TrackRow v-for="t in candidates" :key="t.id" :track="t" can-add @add="addTrack" />
      </div>
      <p v-else class="muted">Tous les morceaux sont déjà dans la playlist.</p>
      <template #footer>
        <button class="btn btn-primary" @click="showAdd = false">Terminé</button>
      </template>
    </AppModal>

    <!-- rename modal -->
    <AppModal v-if="showRename" title="Renommer la playlist" @close="showRename = false">
      <div class="field"><label>Nom</label><input v-model="newName" class="input" /></div>
      <template #footer>
        <button class="btn btn-ghost" @click="showRename = false">Annuler</button>
        <button class="btn btn-primary" @click="rename">Enregistrer</button>
      </template>
    </AppModal>

    <!-- delete modal -->
    <AppModal v-if="showDelete" title="Supprimer la playlist ?" @close="showDelete = false">
      <p class="dim">« {{ playlist.name }} » sera supprimée (les morceaux restent dans la bibliothèque).</p>
      <template #footer>
        <button class="btn btn-ghost" @click="showDelete = false">Annuler</button>
        <button class="btn btn-danger" @click="remove">Supprimer</button>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; margin: 0 auto; }
.back { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; font-weight: 600; margin-bottom: 18px; }
.back:hover { color: var(--text); }

.hero { display: flex; gap: 30px; align-items: flex-end; margin-bottom: 28px; }
.hero-info { display: flex; flex-direction: column; gap: 8px; }
.hero-info h1 { font-size: 48px; cursor: pointer; }
.hero-info h1:hover { color: var(--accent); }
.actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 12px; }

.list { padding: 8px; }
.add-list { max-height: 50vh; overflow-y: auto; }
.empty { text-align: center; padding: 50px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-disc { font-size: 48px; }

@media (max-width: 820px) {
  .hero { flex-direction: column; align-items: flex-start; gap: 18px; }
  .hero-info h1 { font-size: 34px; }
}
</style>
