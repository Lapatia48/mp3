<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import VinylDisc from '@/components/VinylDisc.vue'
import AppIcon from '@/components/AppIcon.vue'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const mode = ref('login')
const loading = ref(false)
const form = reactive({ username: '', email: '', password: '' })

async function submit() {
  if (!form.username || !form.password) {
    toast.error('Nom d’utilisateur et mot de passe requis')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(form.username, form.password)
    } else {
      await auth.register(form.username, form.email, form.password)
    }
    toast.success(`Bienvenue, ${auth.user.username} !`)
    router.push('/')
  } catch (e) {
    const msg = e.response?.data?.message || 'Échec de l’authentification'
    toast.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth">
    <section class="hero">
      <div class="floaty"><VinylDisc :size="240" :spinning="true" label="Vinylia" /></div>
      <div class="hero-text">
        <p class="eyebrow">Votre collection, sublimée</p>
        <h1 class="display">Vinylia</h1>
        <p class="dim">
          Importez vos morceaux, composez des playlists à la durée parfaite,
          et savourez chaque face comme un vinyle.
        </p>
      </div>
    </section>

    <section class="panel">
      <div class="form-card card">
        <div class="tabs">
          <button :class="{ on: mode === 'login' }" @click="mode = 'login'">Connexion</button>
          <button :class="{ on: mode === 'register' }" @click="mode = 'register'">Inscription</button>
        </div>

        <form @submit.prevent="submit">
          <div class="field">
            <label>Nom d’utilisateur</label>
            <input v-model="form.username" class="input" placeholder="ex. john_lennon" autocomplete="username" />
          </div>

          <Transition name="fade">
            <div v-if="mode === 'register'" class="field">
              <label>Email <span class="muted">(facultatif)</span></label>
              <input v-model="form.email" class="input" type="email" placeholder="vous@exemple.com" />
            </div>
          </Transition>

          <div class="field">
            <label>Mot de passe</label>
            <input v-model="form.password" class="input" type="password" placeholder="••••••••" autocomplete="current-password" />
          </div>

          <button class="btn btn-primary submit" type="submit" :disabled="loading">
            <span v-if="loading" class="spinner"></span>
            <template v-else>
              <AppIcon :name="mode === 'login' ? 'play' : 'user'" :size="18" />
              {{ mode === 'login' ? 'Se connecter' : 'Créer mon compte' }}
            </template>
          </button>
        </form>

        <p class="switch muted">
          {{ mode === 'login' ? 'Pas encore de compte ?' : 'Déjà inscrit ?' }}
          <a @click="mode = mode === 'login' ? 'register' : 'login'">
            {{ mode === 'login' ? 'Créer un compte' : 'Se connecter' }}
          </a>
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
}
.hero {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 44px;
  padding: 40px;
  overflow: hidden;
  background:
    radial-gradient(700px 500px at 30% 30%, rgba(216, 116, 63, 0.22), transparent 60%),
    radial-gradient(600px 500px at 70% 80%, rgba(231, 183, 101, 0.16), transparent 60%);
}
.floaty { animation: floaty 6s ease-in-out infinite; }
.hero-text { text-align: center; max-width: 420px; }
.hero-text h1 { font-size: 64px; line-height: 1; margin: 6px 0 14px; }
.hero-text .dim { font-size: 16px; line-height: 1.6; }

.panel { display: flex; align-items: center; justify-content: center; padding: 40px; }
.form-card { width: 100%; max-width: 380px; padding: 30px; }

.tabs {
  display: flex;
  gap: 6px;
  background: var(--bg-elev);
  padding: 5px;
  border-radius: 999px;
  margin-bottom: 24px;
}
.tabs button {
  flex: 1;
  border: none;
  background: transparent;
  color: var(--text-dim);
  padding: 10px;
  border-radius: 999px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.tabs button.on { background: var(--surface-2); color: var(--text); box-shadow: var(--shadow-soft); }

form { display: flex; flex-direction: column; gap: 16px; }
.submit { width: 100%; margin-top: 6px; height: 46px; }
.switch { text-align: center; margin: 18px 0 0; font-size: 14px; }
.switch a { color: var(--accent); cursor: pointer; font-weight: 600; }
.switch a:hover { text-decoration: underline; }

@media (max-width: 880px) {
  .auth { grid-template-columns: 1fr; }
  .hero { display: none; }
}
</style>
