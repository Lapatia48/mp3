<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tracksApi } from '@/api/tracks'
import { playlistsApi } from '@/api/playlists'
import { usePlayerStore } from '@/stores/player'
import { useToastStore } from '@/stores/toast'
import { formatTotal } from '@/utils/format'
import TrackRow from '@/components/TrackRow.vue'
import TagSelect from '@/components/TagSelect.vue'
import AppModal from '@/components/AppModal.vue'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const player = usePlayerStore()
const toast = useToastStore()

const library = ref([])
const criteria = reactive({
  minMinutes: 30,
  maxMinutes: 90,
  genre: '',
  includeArtists: [],
  excludeArtists: [],
  includeAlbums: [],
  excludeAlbums: [],
})
const generating = ref(false)
const generated = ref(false)
const selection = ref([])

const genres = computed(() => [...new Set(library.value.map((t) => t.genre).filter(Boolean))].sort())
const artists = computed(() => [...new Set(library.value.map((t) => t.artist).filter(Boolean))].sort())
const albums = computed(() => [...new Set(library.value.map((t) => t.album).filter(Boolean))].sort())

const minSec = computed(() => (criteria.minMinutes || 0) * 60)
const maxSec = computed(() => (criteria.maxMinutes || 0) * 60)
const total = computed(() => selection.value.reduce((s, t) => s + (t.duration || 0), 0))
const matchPct = computed(() => (maxSec.value ? Math.min(100, Math.round((total.value / maxSec.value) * 100)) : 0))

// Le minimum ne peut pas depasser le maximum (et inversement).
watch(() => criteria.minMinutes, (v) => { if (v > criteria.maxMinutes) criteria.maxMinutes = v })
watch(() => criteria.maxMinutes, (v) => { if (v < criteria.minMinutes) criteria.minMinutes = v })

const presets = [
  { label: '20–40 min', min: 20, max: 40 },
  { label: '40–70 min', min: 40, max: 70 },
  { label: '60–90 min', min: 60, max: 90 },
  { label: '90–120 min', min: 90, max: 120 },
]
function applyPreset(p) {
  criteria.minMinutes = p.min
  criteria.maxMinutes = p.max
}
function rangeStyle(v) {
  return { '--pct': ((v - 5) / 175 * 100) + '%' }
}

onMounted(async () => {
  try {
    library.value = await tracksApi.list()
  } catch {
    /* ignore */
  }
})

async function generate() {
  generating.value = true
  try {
    const data = await playlistsApi.generate({
      minMinutes: Number(criteria.minMinutes) || 0,
      maxMinutes: Number(criteria.maxMinutes) || 60,
      genre: criteria.genre || null,
      includeArtists: criteria.includeArtists,
      excludeArtists: criteria.excludeArtists,
      includeAlbums: criteria.includeAlbums,
      excludeAlbums: criteria.excludeAlbums,
    })
    selection.value = [...data.tracks]
    generated.value = true
    if (!data.tracks.length) toast.info('Aucun morceau ne correspond à ces critères')
  } catch {
    toast.error('Échec de la génération')
  } finally {
    generating.value = false
  }
}

function removeTrack(t) {
  selection.value = selection.value.filter((x) => x.id !== t.id)
}
function addTrack(t) {
  if (!selection.value.some((x) => x.id === t.id)) selection.value.push(t)
}
function playAll() {
  if (selection.value.length) player.playQueue(selection.value, 0)
}

const candidates = computed(() =>
  library.value.filter((t) => !selection.value.some((s) => s.id === t.id)),
)

/* ---- Save ---- */
const showSave = ref(false)
const playlistName = ref('Ma playlist')
async function save() {
  if (!selection.value.length) {
    toast.error('La playlist est vide')
    return
  }
  try {
    const saved = await playlistsApi.save({
      name: playlistName.value || 'Ma playlist',
      trackIds: selection.value.map((t) => t.id),
    })
    toast.success('Playlist enregistrée')
    showSave.value = false
    router.push(`/playlists/${saved.id}`)
  } catch {
    toast.error('Échec de l’enregistrement')
  }
}

const showAddPanel = ref(false)
</script>

<template>
  <div class="page">
    <header class="head">
      <div>
        <p class="eyebrow">Composition automatique</p>
        <h1 class="display">Générer une playlist</h1>
        <p class="muted sub">Choisissez une durée et des critères : Vinylia s’approche de la durée idéale.</p>
      </div>
    </header>

    <!-- Criteres -->
    <section class="criteria card">
      <div class="duration">
        <label class="dim">Durée souhaitée</label>
        <div class="dur-value display">{{ criteria.minMinutes }}–{{ criteria.maxMinutes }} <small>min</small></div>

        <div class="dur-slider">
          <span class="dim dur-tag">Min</span>
          <input v-model.number="criteria.minMinutes" type="range" min="5" max="180" step="5"
                 class="range" :style="rangeStyle(criteria.minMinutes)" />
          <span class="dur-num">{{ criteria.minMinutes }}</span>
        </div>
        <div class="dur-slider">
          <span class="dim dur-tag">Max</span>
          <input v-model.number="criteria.maxMinutes" type="range" min="5" max="180" step="5"
                 class="range" :style="rangeStyle(criteria.maxMinutes)" />
          <span class="dur-num">{{ criteria.maxMinutes }}</span>
        </div>

        <div class="presets">
          <button v-for="p in presets" :key="p.label" class="chip"
                  :class="{ 'chip-accent': criteria.minMinutes === p.min && criteria.maxMinutes === p.max }"
                  @click="applyPreset(p)">{{ p.label }}</button>
        </div>
      </div>

      <div class="filters">
        <div class="field">
          <label>Genre</label>
          <select v-model="criteria.genre" class="select">
            <option value="">Tous</option>
            <option v-for="g in genres" :key="g" :value="g">{{ g }}</option>
          </select>
        </div>

        <div class="field">
          <label>Artistes <span class="opt">(facultatif)</span></label>
          <div class="inc-exc">
            <div class="inc-col">
              <span class="inc-tag inc"> Inclure</span>
              <TagSelect v-model="criteria.includeArtists" :options="artists"
                         add-label="+ Inclure un artiste" empty-label="Tous les artistes" />
            </div>
            <div class="inc-col">
              <span class="inc-tag exc"> Exclure</span>
              <TagSelect v-model="criteria.excludeArtists" :options="artists"
                         add-label="+ Exclure un artiste" empty-label="Aucun" />
            </div>
          </div>
        </div>

        <div class="field">
          <label>Albums <span class="opt">(facultatif)</span></label>
          <div class="inc-exc">
            <div class="inc-col">
              <span class="inc-tag inc"> Inclure</span>
              <TagSelect v-model="criteria.includeAlbums" :options="albums"
                         add-label="+ Inclure un album" empty-label="Tous les albums" />
            </div>
            <div class="inc-col">
              <span class="inc-tag exc"> Exclure</span>
              <TagSelect v-model="criteria.excludeAlbums" :options="albums"
                         add-label="+ Exclure un album" empty-label="Aucun" />
            </div>
          </div>
        </div>

        <button class="btn btn-primary gen" :disabled="generating" @click="generate">
          <span v-if="generating" class="spinner"></span>
          <template v-else><AppIcon name="sliders" :size="18" /> Générer</template>
        </button>
      </div>
    </section>

    <!-- Resultat -->
    <section v-if="generated" class="result">
      <div class="result-head card">
        <div>
          <h2 class="display">Playlist proposée</h2>
          <p class="muted">{{ selection.length }} morceaux · objectif {{ criteria.minMinutes }}–{{ criteria.maxMinutes }} min</p>
        </div>
        <div class="gauge">
          <div class="bar"><div class="fill" :style="{ width: matchPct + '%' }"></div></div>
          <span class="dim">{{ formatTotal(total) }} · cible {{ formatTotal(minSec) }}–{{ formatTotal(maxSec) }}</span>
        </div>
        <div class="result-actions">
          <button class="btn" :disabled="!selection.length" @click="playAll"><AppIcon name="play" :size="16" /> Lire</button>
          <button class="btn" @click="showAddPanel = true"><AppIcon name="plus" :size="16" /> Ajouter</button>
          <button class="btn btn-primary" :disabled="!selection.length" @click="showSave = true">
            <AppIcon name="check" :size="16" /> Enregistrer
          </button>
        </div>
      </div>

      <div v-if="selection.length" class="list card">
        <TrackRow
          v-for="(t, i) in selection"
          :key="t.id"
          :track="t"
          :index="i"
          :context="selection"
          can-remove
          @remove="removeTrack"
        />
      </div>
      <div v-else class="empty card">
        <div class="empty-disc">🎚️</div>
        <p class="muted">Aucun morceau retenu. Ajoutez-en manuellement ou changez les critères.</p>
        <button class="btn" @click="showAddPanel = true"><AppIcon name="plus" :size="16" /> Ajouter des morceaux</button>
      </div>
    </section>

    <!-- Panneau d'ajout -->
    <AppModal v-if="showAddPanel" title="Ajouter des morceaux" @close="showAddPanel = false">
      <div v-if="candidates.length" class="add-list">
        <TrackRow v-for="t in candidates" :key="t.id" :track="t" can-add @add="addTrack" />
      </div>
      <p v-else class="muted">Tous les morceaux de la bibliothèque sont déjà dans la playlist.</p>
      <template #footer>
        <button class="btn btn-primary" @click="showAddPanel = false">Terminé</button>
      </template>
    </AppModal>

    <!-- Enregistrement -->
    <AppModal v-if="showSave" title="Enregistrer la playlist" @close="showSave = false">
      <div class="field">
        <label>Nom de la playlist</label>
        <input v-model="playlistName" class="input" placeholder="Ma playlist" />
      </div>
      <p class="muted">{{ selection.length }} morceaux · {{ formatTotal(total) }}</p>
      <template #footer>
        <button class="btn btn-ghost" @click="showSave = false">Annuler</button>
        <button class="btn btn-primary" @click="save">Enregistrer</button>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; margin: 0 auto; }
.head { margin-bottom: 24px; }
.head h1 { font-size: 42px; margin: 4px 0; }
.sub { font-size: 14px; }

.criteria { padding: 24px; display: grid; grid-template-columns: 1fr 1.4fr; gap: 30px; }
.duration { display: flex; flex-direction: column; gap: 12px; }
.dur-value { font-size: 40px; }
.dur-value small { font-size: 18px; color: var(--muted); }
.dur-slider { display: grid; grid-template-columns: 34px 1fr 34px; align-items: center; gap: 10px; }
.dur-tag { font-size: 12px; font-weight: 600; }
.dur-num { font-size: 13px; text-align: right; color: var(--text-dim); }
.presets { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
.presets .chip { cursor: pointer; }

.filters { display: flex; flex-direction: column; gap: 16px; }
.filters .gen { height: 46px; }
.opt { font-weight: 400; color: var(--muted); font-size: 12px; }

.inc-exc { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.inc-col {
  display: flex; flex-direction: column; gap: 8px;
  padding: 12px; border-radius: var(--radius-sm);
  background: var(--bg-elev); border: 1px solid var(--border);
}
.inc-tag {
  display: inline-flex; align-items: center; gap: 5px; align-self: flex-start;
  font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.6px;
}
.inc-tag.inc { color: var(--ok); }
.inc-tag.exc { color: var(--danger); }

.range {
  -webkit-appearance: none; appearance: none; height: 6px; border-radius: 999px;
  background: linear-gradient(to right, var(--accent) var(--pct), var(--surface-3) var(--pct));
  outline: none; cursor: pointer;
}
.range::-webkit-slider-thumb {
  -webkit-appearance: none; width: 18px; height: 18px; border-radius: 50%;
  background: var(--text); box-shadow: 0 2px 6px rgba(0,0,0,0.5); cursor: pointer;
}

.result { margin-top: 26px; display: flex; flex-direction: column; gap: 16px; }
.result-head { padding: 20px 24px; display: flex; align-items: center; justify-content: space-between; gap: 24px; flex-wrap: wrap; }
.result-head h2 { font-size: 26px; }
.gauge { flex: 1; min-width: 180px; display: flex; flex-direction: column; gap: 6px; }
.gauge .bar { height: 8px; border-radius: 999px; background: var(--surface-3); overflow: hidden; }
.gauge .fill { height: 100%; background: var(--accent-grad); border-radius: 999px; transition: width 0.4s; }
.gauge span { font-size: 12px; }
.result-actions { display: flex; gap: 10px; flex-wrap: wrap; }

.list { padding: 8px; }
.add-list { max-height: 50vh; overflow-y: auto; }
.empty { text-align: center; padding: 44px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-disc { font-size: 44px; }

@media (max-width: 820px) {
  .criteria { grid-template-columns: 1fr; }
  .inc-exc { grid-template-columns: 1fr; }
}
</style>
