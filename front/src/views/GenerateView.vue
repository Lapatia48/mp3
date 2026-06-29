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
  includeGenres: [],
  excludeGenres: [],
  includeArtists: [],
  onlyArtists: [],
  excludeArtists: [],
  includeAlbums: [],
  onlyAlbums: [],
  excludeAlbums: [],
})
const generating = ref(false)
const generated = ref(false)
const selection = ref([])

const genres = computed(() => [...new Set(library.value.map((t) => t.genre).filter(Boolean))].sort())
const artists = computed(() => [...new Set(library.value.map((t) => t.artist).filter(Boolean))].sort())

// Options d'albums avec l'artiste en libelle ("Album · Artiste") : sans cela la
// liste est illisible (plusieurs albums peuvent porter le meme nom).
const albumOptions = computed(() => {
  const byAlbum = new Map()
  for (const t of library.value) {
    if (!t.album) continue
    if (!byAlbum.has(t.album)) byAlbum.set(t.album, new Set())
    if (t.artist) byAlbum.get(t.album).add(t.artist)
  }
  return [...byAlbum.keys()]
    .sort((a, b) => a.localeCompare(b))
    .map((album) => {
      const who = [...byAlbum.get(album)]
      const tag = who.length === 0 ? '' : who.length === 1 ? who[0] : `${who.length} artistes`
      return { value: album, label: tag ? `${album} · ${tag}` : album }
    })
})

const minSec = computed(() => (criteria.minMinutes || 0) * 60)
const maxSec = computed(() => (criteria.maxMinutes || 0) * 60)
const total = computed(() => selection.value.reduce((s, t) => s + (t.duration || 0), 0))
const matchPct = computed(() => (maxSec.value ? Math.min(100, Math.round((total.value / maxSec.value) * 100)) : 0))
// Playlist plus courte que la cible : assume (les contraintes priment sur la duree).
const belowTarget = computed(() => generated.value && selection.value.length > 0 && total.value < minSec.value)

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

/* ------------------------------------------------------------------ */
/* Generation cote client : toutes les musiques sont deja chargees     */
/* dans `library`, on applique les contraintes ici en JS (pas d'appel  */
/* au backend pour la generation).                                     */
/* ------------------------------------------------------------------ */

// Ensemble en minuscules, sans valeurs vides.
function lowerSet(arr) {
  return new Set((arr || []).filter(Boolean).map((s) => String(s).trim().toLowerCase()))
}
// La valeur (insensible a la casse) est-elle dans l'ensemble ?
function inSet(value, set) {
  return value != null && set.has(String(value).toLowerCase())
}
function durOf(t) {
  return t.duration && t.duration > 0 ? t.duration : 0
}
// Melange (Fisher–Yates) pour varier les playlists d'une fois a l'autre.
function shuffled(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

function buildPlaylist() {
  const minSec = (Number(criteria.minMinutes) || 0) * 60
  const maxSec = (Number(criteria.maxMinutes) || 60) * 60

  // Exclusions absolues : ces morceaux n'apparaissent jamais.
  const exArtists = lowerSet(criteria.excludeArtists)
  const exAlbums = lowerSet(criteria.excludeAlbums)
  const exGenres = lowerSet(criteria.excludeGenres)
  // Filtres / restrictions.
  const inGenres = lowerSet(criteria.includeGenres)
  const onlyArtists = lowerSet(criteria.onlyArtists)
  const onlyAlbums = lowerSet(criteria.onlyAlbums)
  // « graines » (inclus mais pas « uniquement »).
  const incArtists = lowerSet(criteria.includeArtists)
  const incAlbums = lowerSet(criteria.includeAlbums)
  const softArtists = new Set([...incArtists].filter((x) => !onlyArtists.has(x)))
  const softAlbums = new Set([...incAlbums].filter((x) => !onlyAlbums.has(x)))

  // 1) Pool autorise : exclusions, genre inclus, restriction « artiste uniquement ».
  const pool = library.value.filter((t) => {
    if (!(t.duration > 0)) return false
    if (inSet(t.artist, exArtists)) return false
    if (inSet(t.album, exAlbums)) return false
    if (inSet(t.genre, exGenres)) return false
    if (inGenres.size && !inSet(t.genre, inGenres)) return false
    if (onlyArtists.size && !inSet(t.artist, onlyArtists)) return false
    return true
  })

  // 2) Albums « uniquement » : tout l'album est obligatoire.
  const mandatory = pool.filter((t) => inSet(t.album, onlyAlbums))
  const hasSoftSeeds = softArtists.size > 0 || softAlbums.size > 0
  // « album uniquement » seul : on prend l'album entier et on s'arrete la.
  const stopAfterMandatory = onlyAlbums.size > 0 && !hasSoftSeeds

  // 3) Graines prioritaires puis le reste du pool (hors albums obligatoires).
  const isSeed = (t) => inSet(t.artist, softArtists) || inSet(t.album, softAlbums)
  const preferred = pool.filter((t) => !inSet(t.album, onlyAlbums) && isSeed(t))
  const others = pool.filter((t) => !inSet(t.album, onlyAlbums) && !isSeed(t))

  // 4) Composition.
  const selected = []
  const used = new Set()
  let sum = 0
  for (const t of mandatory) {
    if (used.has(t.id)) continue
    used.add(t.id)
    selected.push(t)
    sum += durOf(t)
  }
  if (stopAfterMandatory) return selected

  const queue = [...shuffled(preferred), ...shuffled(others)]
  // Remplissage glouton sans depasser le maximum.
  for (const t of queue) {
    if (sum >= maxSec) break
    if (used.has(t.id)) continue
    const d = durOf(t)
    if (d <= 0) continue
    if (sum + d <= maxSec) {
      selected.push(t)
      used.add(t.id)
      sum += d
    }
  }
  // Comble le minimum avec le morceau le plus adapte (leger depassement tolere).
  if (sum < minSec) {
    let best = null
    let bestDiff = Infinity
    const gap = minSec - sum
    for (const t of queue) {
      if (used.has(t.id)) continue
      const d = durOf(t)
      if (d <= 0) continue
      const diff = Math.abs(gap - d)
      if (diff < bestDiff) {
        bestDiff = diff
        best = t
      }
    }
    if (best) selected.push(best)
  }
  return selected
}

async function generate() {
  generating.value = true
  try {
    // Petit delai pour laisser apparaitre l'indicateur de chargement.
    await new Promise((r) => setTimeout(r, 120))
    const tracks = buildPlaylist()
    selection.value = tracks
    generated.value = true
    if (!tracks.length) toast.info('Aucun morceau ne correspond à ces critères')
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
const searchAdd = ref('')
const filteredCandidates = computed(() => {
  const q = searchAdd.value.trim().toLowerCase()
  if (!q) return candidates.value
  return candidates.value.filter(
    (t) =>
      (t.title && t.title.toLowerCase().includes(q)) ||
      (t.artist && t.artist.toLowerCase().includes(q)) ||
      (t.album && t.album.toLowerCase().includes(q)),
  )
})
function closeAddPanel() {
  showAddPanel.value = false
  searchAdd.value = ''
}
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
        <h3 class="filters-title">Filtres</h3>

        <!-- Genres -->
        <div class="fgroup">
          <div class="fgroup-title">Genres <span class="opt">facultatif</span></div>
          <div class="constraint-grid">
            <div class="constraint-col constraint-inc">
              <div class="constraint-head">Inclure</div>
              <TagSelect v-model="criteria.includeGenres" :options="genres"
                         add-label="Rechercher un genre…" empty-label="Tous les genres" />
            </div>
            <div class="constraint-col constraint-exc">
              <div class="constraint-head">Exclure</div>
              <TagSelect v-model="criteria.excludeGenres" :options="genres"
                         add-label="Rechercher un genre…" empty-label="Aucun" />
            </div>
          </div>
        </div>

        <!-- Artistes -->
        <div class="fgroup">
          <div class="fgroup-title">Artistes <span class="opt">facultatif</span></div>
          <div class="constraint-grid">
            <div class="constraint-col constraint-inc">
              <div class="constraint-head">Inclure</div>
              <TagSelect v-model="criteria.includeArtists" v-model:onlyValues="criteria.onlyArtists"
                         :options="artists" allow-only
                         add-label="Rechercher un artiste…" empty-label="Tous les artistes" />
            </div>
            <div class="constraint-col constraint-exc">
              <div class="constraint-head">Exclure</div>
              <TagSelect v-model="criteria.excludeArtists" :options="artists"
                         add-label="Rechercher un artiste…" empty-label="Aucun" />
            </div>
          </div>
        </div>

        <!-- Albums -->
        <div class="fgroup">
          <div class="fgroup-title">Albums <span class="opt">facultatif</span></div>
          <div class="constraint-grid">
            <div class="constraint-col constraint-inc">
              <div class="constraint-head">Inclure</div>
              <TagSelect v-model="criteria.includeAlbums" v-model:onlyValues="criteria.onlyAlbums"
                         :options="albumOptions" allow-only
                         add-label="Rechercher un album…" empty-label="Tous les albums" />
            </div>
            <div class="constraint-col constraint-exc">
              <div class="constraint-head">Exclure</div>
              <TagSelect v-model="criteria.excludeAlbums" :options="albumOptions"
                         add-label="Rechercher un album…" empty-label="Aucun" />
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
          <div class="bar"><div class="fill" :class="{ short: belowTarget }" :style="{ width: matchPct + '%' }"></div></div>
          <span class="dim">{{ formatTotal(total) }} · cible {{ formatTotal(minSec) }}–{{ formatTotal(maxSec) }}</span>
          <span v-if="belowTarget" class="gauge-hint">
            <AppIcon name="clock" :size="12" /> Durée réduite : vos contraintes priment sur la cible.
          </span>
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
        <div class="empty-disc"><AppIcon name="sliders" :size="40" /></div>
        <p class="muted">Aucun morceau retenu. Ajoutez-en manuellement ou changez les critères.</p>
        <button class="btn" @click="showAddPanel = true"><AppIcon name="plus" :size="16" /> Ajouter des morceaux</button>
      </div>
    </section>

    <!-- Panneau d'ajout -->
    <AppModal v-if="showAddPanel" title="Ajouter des morceaux" @close="closeAddPanel">
      <div class="add-search">
        <AppIcon name="search" :size="14" class="add-search-ico" />
        <input
          v-model="searchAdd"
          type="text"
          placeholder="Rechercher par titre, artiste, album…"
          class="add-search-inp"
          autocomplete="off"
        />
      </div>
      <div v-if="filteredCandidates.length" class="add-list">
        <TrackRow v-for="t in filteredCandidates" :key="t.id" :track="t" can-add @add="addTrack" />
      </div>
      <p v-else class="muted">
        <template v-if="searchAdd">Aucun résultat pour « {{ searchAdd }} ».</template>
        <template v-else>Tous les morceaux de la bibliothèque sont déjà dans la playlist.</template>
      </p>
      <template #footer>
        <button class="btn btn-primary" @click="closeAddPanel">Terminé</button>
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
.filters .gen { height: 46px; margin-top: 2px; }
.opt { font-weight: 400; color: var(--muted); font-size: 11px; text-transform: none; letter-spacing: 0; }

.filters-title { font-size: 18px; font-weight: 700; margin: 0; }

.fgroup {
  display: flex; flex-direction: column; gap: 12px;
  padding: 16px; border-radius: var(--radius-sm);
  border: 1px solid var(--border); background: var(--bg-elev);
}
.fgroup-title {
  display: flex; align-items: center; gap: 7px;
  font-size: 11px; font-weight: 800; color: var(--text-dim);
  text-transform: uppercase; letter-spacing: 0.8px;
}

.constraint-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.constraint-col {
  display: flex; flex-direction: column; gap: 10px;
  padding: 14px; border-radius: var(--radius-sm);
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-top-width: 2px;
}
.constraint-inc { border-top-color: var(--ok); }
.constraint-exc { border-top-color: var(--danger); }
.constraint-head {
  font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.8px;
}
.constraint-inc .constraint-head { color: var(--ok); }
.constraint-exc .constraint-head { color: var(--danger); }

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
.gauge .fill.short { background: linear-gradient(135deg, #d8743f, #c85f33); }
.gauge span { font-size: 12px; }
.gauge-hint {
  display: inline-flex; align-items: center; gap: 5px;
  color: var(--amber); font-weight: 500;
}
.result-actions { display: flex; gap: 10px; flex-wrap: wrap; }

.list { padding: 8px; }
.add-list { max-height: 50vh; overflow-y: auto; }
.empty { text-align: center; padding: 44px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-disc { color: var(--text-dim); }

.add-search {
  display: flex; align-items: center; gap: 9px;
  padding: 9px 12px; border: 1px solid var(--border);
  border-radius: var(--radius-sm); background: var(--bg-elev);
  transition: border-color 0.15s;
}
.add-search:focus-within { border-color: var(--border-strong); }
.add-search-ico { color: var(--text-dim); flex: none; }
.add-search-inp {
  flex: 1; border: none; background: transparent; outline: none;
  font-size: 14px; color: var(--text);
}
.add-search-inp::placeholder { color: var(--text-dim); }

@media (max-width: 820px) {
  .criteria { grid-template-columns: 1fr; }
  .constraint-grid { grid-template-columns: 1fr; }
}
</style>
