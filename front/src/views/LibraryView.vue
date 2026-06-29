<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { tracksApi } from '@/api/tracks'
import { usePlayerStore } from '@/stores/player'
import { useToastStore } from '@/stores/toast'
import { formatTotal } from '@/utils/format'
import TrackRow from '@/components/TrackRow.vue'
import AppModal from '@/components/AppModal.vue'
import AppIcon from '@/components/AppIcon.vue'

const player = usePlayerStore()
const toast = useToastStore()

const tracks = ref([])
const loading = ref(true)
const search = ref('')
const genreFilter = ref('')

const genres = computed(() => [...new Set(tracks.value.map((t) => t.genre).filter(Boolean))].sort())

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return tracks.value.filter((t) => {
    const matchQ =
      !q ||
      [t.title, t.artist, t.album].some((f) => (f || '').toLowerCase().includes(q))
    const matchG = !genreFilter.value || t.genre === genreFilter.value
    return matchQ && matchG
  })
})

const totalDuration = computed(() => filtered.value.reduce((s, t) => s + (t.duration || 0), 0))

async function load() {
  loading.value = true
  try {
    tracks.value = await tracksApi.list()
  } catch {
    toast.error('Impossible de charger la bibliothèque')
  } finally {
    loading.value = false
  }
}
onMounted(load)

function playAll() {
  if (filtered.value.length) player.playQueue(filtered.value, 0)
}
function shuffle() {
  if (!filtered.value.length) return
  const arr = [...filtered.value].sort(() => Math.random() - 0.5)
  player.playQueue(arr, 0)
}

/* ---- Import ---- */
const showAdd = ref(false)
const adding = ref(false)
const file = ref(null)
const addForm = reactive({ title: '', artist: '', album: '', genre: '', year: '' })

function onFile(e) {
  file.value = e.target.files[0] || null
}
async function submitAdd() {
  if (!file.value) {
    toast.error('Choisissez un fichier MP3')
    return
  }
  adding.value = true
  try {
    await tracksApi.create(file.value, { ...addForm })
    toast.success('Morceau ajouté')
    showAdd.value = false
    Object.assign(addForm, { title: '', artist: '', album: '', genre: '', year: '' })
    file.value = null
    await load()
  } catch (e) {
    toast.error(e.response?.data?.message || 'Échec de l’import')
  } finally {
    adding.value = false
  }
}

/* ---- Edit ---- */
const showEdit = ref(false)
const editForm = reactive({ id: null, title: '', artist: '', album: '', genre: '', year: '' })
function openEdit(t) {
  Object.assign(editForm, {
    id: t.id, title: t.title, artist: t.artist, album: t.album, genre: t.genre, year: t.year,
  })
  showEdit.value = true
}
async function submitEdit() {
  try {
    await tracksApi.update(editForm.id, { ...editForm })
    toast.success('Morceau modifié')
    showEdit.value = false
    await load()
  } catch {
    toast.error('Échec de la modification')
  }
}

/* ---- Delete ---- */
const toDelete = ref(null)
async function confirmDelete() {
  try {
    await tracksApi.remove(toDelete.value.id)
    toast.success('Morceau supprimé')
    toDelete.value = null
    await load()
  } catch {
    toast.error('Échec de la suppression')
  }
}
</script>

<template>
  <div class="page">
    <header class="head">
      <div>
        <p class="eyebrow">Votre collection</p>
        <h1 class="display">Bibliothèque</h1>
        <p class="muted sub">
          {{ filtered.length }} morceau{{ filtered.length > 1 ? 'x' : '' }} · {{ formatTotal(totalDuration) }}
        </p>
      </div>
      <div class="head-actions">
        <button class="btn" :disabled="!filtered.length" @click="shuffle">
          <AppIcon name="disc" :size="18" /> Aléatoire
        </button>
        <button class="btn btn-primary" :disabled="!filtered.length" @click="playAll">
          <AppIcon name="play" :size="18" /> Tout lire
        </button>
        <button class="btn" @click="showAdd = true"><AppIcon name="upload" :size="18" /> Importer</button>
      </div>
    </header>

    <div class="toolbar">
      <div class="search">
        <AppIcon name="search" :size="18" class="muted" />
        <input v-model="search" class="input" placeholder="Rechercher titre, artiste, album…" />
      </div>
      <select v-model="genreFilter" class="select genre">
        <option value="">Tous les genres</option>
        <option v-for="g in genres" :key="g" :value="g">{{ g }}</option>
      </select>
    </div>

    <div v-if="loading" class="list">
      <div v-for="i in 6" :key="i" class="skeleton" style="height: 54px; margin-bottom: 8px"></div>
    </div>

    <div v-else-if="!filtered.length" class="empty card">
      <div class="empty-disc">💿</div>
      <h3 class="display">Aucun morceau</h3>
      <p class="muted">Importez un MP3 ou lancez le pipeline d’import automatique.</p>
      <button class="btn btn-primary" @click="showAdd = true"><AppIcon name="upload" :size="18" /> Importer un MP3</button>
    </div>

    <div v-else class="list card">
      <div class="list-head">
        <span>#</span><span>Titre</span><span>Album</span><span>Genre</span><span class="r">Durée</span><span></span>
      </div>
      <TrackRow
        v-for="(t, i) in filtered"
        :key="t.id"
        :track="t"
        :index="i"
        :context="filtered"
        can-edit
        can-delete
        @edit="openEdit"
        @delete="toDelete = $event"
      />
    </div>

    <!-- Import modal -->
    <AppModal v-if="showAdd" title="Importer un morceau" @close="showAdd = false">
      <label class="dropzone">
        <AppIcon name="upload" :size="26" />
        <span v-if="!file">Choisir un fichier MP3</span>
        <strong v-else>{{ file.name }}</strong>
        <input type="file" accept="audio/mpeg,.mp3" hidden @change="onFile" />
      </label>
      <div class="grid2">
        <div class="field"><label>Titre</label><input v-model="addForm.title" class="input" /></div>
        <div class="field"><label>Artiste</label><input v-model="addForm.artist" class="input" /></div>
        <div class="field"><label>Album</label><input v-model="addForm.album" class="input" /></div>
        <div class="field"><label>Genre</label><input v-model="addForm.genre" class="input" /></div>
        <div class="field"><label>Année</label><input v-model="addForm.year" class="input" /></div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="showAdd = false">Annuler</button>
        <button class="btn btn-primary" :disabled="adding" @click="submitAdd">
          <span v-if="adding" class="spinner"></span><template v-else>Importer</template>
        </button>
      </template>
    </AppModal>

    <!-- Edit modal -->
    <AppModal v-if="showEdit" title="Modifier le morceau" @close="showEdit = false">
      <div class="grid2">
        <div class="field"><label>Titre</label><input v-model="editForm.title" class="input" /></div>
        <div class="field"><label>Artiste</label><input v-model="editForm.artist" class="input" /></div>
        <div class="field"><label>Album</label><input v-model="editForm.album" class="input" /></div>
        <div class="field"><label>Genre</label><input v-model="editForm.genre" class="input" /></div>
        <div class="field"><label>Année</label><input v-model="editForm.year" class="input" /></div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="showEdit = false">Annuler</button>
        <button class="btn btn-primary" @click="submitEdit">Enregistrer</button>
      </template>
    </AppModal>

    <!-- Delete confirm -->
    <AppModal v-if="toDelete" title="Supprimer le morceau ?" @close="toDelete = null">
      <p class="dim">« {{ toDelete.title || toDelete.fileName }} » sera supprimé définitivement (base + fichier).</p>
      <template #footer>
        <button class="btn btn-ghost" @click="toDelete = null">Annuler</button>
        <button class="btn btn-danger" @click="confirmDelete">Supprimer</button>
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

.toolbar { display: flex; gap: 12px; margin-bottom: 18px; }
.search { position: relative; flex: 1; display: flex; align-items: center; }
.search :deep(svg) { position: absolute; left: 14px; }
.search .input { padding-left: 42px; }
.genre { max-width: 200px; }

.list { padding: 8px; }
.list-head {
  display: grid;
  grid-template-columns: 44px 2.4fr 1.5fr 1fr 56px auto;
  gap: 14px;
  padding: 6px 14px 12px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--muted);
  border-bottom: 1px solid var(--border);
  margin-bottom: 6px;
}
.list-head .r { text-align: right; }

.empty { text-align: center; padding: 60px 30px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-disc { font-size: 56px; line-height: 1; margin-bottom: 8px; }
.empty h3 { font-size: 24px; }
.empty .btn { margin-top: 10px; }

.dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 26px;
  border: 2px dashed var(--border-strong);
  border-radius: var(--radius);
  cursor: pointer;
  color: var(--text-dim);
  transition: border-color 0.2s, background 0.2s;
}
.dropzone:hover { border-color: var(--accent); background: var(--accent-soft); }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

@media (max-width: 820px) {
  .list-head { grid-template-columns: 40px 1fr 56px auto; }
  .list-head span:nth-child(3), .list-head span:nth-child(4) { display: none; }
  .grid2 { grid-template-columns: 1fr; }
}
</style>
