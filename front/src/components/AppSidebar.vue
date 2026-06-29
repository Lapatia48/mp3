<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { initials } from '@/utils/format'
import AppIcon from './AppIcon.vue'

const auth = useAuthStore()
const router = useRouter()

const links = [
  { to: '/', icon: 'home', label: 'Bibliothèque' },
  { to: '/generate', icon: 'sliders', label: 'Générer' },
  { to: '/playlists', icon: 'list', label: 'Mes playlists' },
]

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <span class="disc"><AppIcon name="disc" :size="26" /></span>
      <span class="name display">Vinylia</span>
    </div>

    <nav>
      <RouterLink v-for="l in links" :key="l.to" :to="l.to" class="nav-link" active-class="active">
        <AppIcon :name="l.icon" :size="20" />
        <span>{{ l.label }}</span>
      </RouterLink>
    </nav>

    <div class="spacer"></div>

    <div class="user card">
      <div class="avatar">{{ initials(auth.user?.username) }}</div>
      <div class="meta">
        <strong>{{ auth.user?.username }}</strong>
        <small class="muted">{{ auth.user?.role }}</small>
      </div>
      <button class="btn-icon" title="Déconnexion" @click="logout">
        <AppIcon name="logout" :size="18" />
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--sidebar-w);
  flex-shrink: 0;
  padding: 24px 18px;
  display: flex;
  flex-direction: column;
  gap: 22px;
  background: linear-gradient(180deg, var(--bg-elev), rgba(15, 12, 11, 0.6));
  border-right: 1px solid var(--border);
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 8px;
}
.brand .disc {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--accent-grad);
  color: #2a1709;
  box-shadow: 0 8px 20px rgba(216, 116, 63, 0.4);
}
.brand .name { font-size: 24px; font-weight: 700; }

nav { display: flex; flex-direction: column; gap: 4px; }
.nav-link {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  color: var(--text-dim);
  font-weight: 600;
  font-size: 15px;
  transition: background 0.18s, color 0.18s, transform 0.18s;
}
.nav-link:hover { background: var(--surface); color: var(--text); transform: translateX(2px); }
.nav-link.active {
  color: var(--text);
  background: var(--accent-soft);
  box-shadow: inset 3px 0 0 var(--accent);
}
.nav-link.active :deep(svg) { color: var(--accent); }

.spacer { flex: 1; }

.user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
}
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #2a1709;
  background: var(--accent-grad);
  flex-shrink: 0;
}
.user .meta { display: flex; flex-direction: column; line-height: 1.3; overflow: hidden; }
.user .meta strong { font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user .meta small { font-size: 11px; text-transform: capitalize; }
</style>
