import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  { path: '/', name: 'library', component: () => import('@/views/LibraryView.vue') },
  { path: '/generate', name: 'generate', component: () => import('@/views/GenerateView.vue') },
  { path: '/playlists', name: 'playlists', component: () => import('@/views/PlaylistsView.vue') },
  { path: '/playlists/:id', name: 'playlist-detail', component: () => import('@/views/PlaylistDetailView.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login' }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'library' }
  }
  return true
})

export default router
