import axios from 'axios'

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const http = axios.create({ baseURL: API_URL })

// Injecte le JWT sur chaque requete
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Deconnexion automatique si le jeton est invalide / expire
http.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && (err.response.status === 401 || err.response.status === 403)) {
      const hadToken = !!localStorage.getItem('token')
      if (hadToken && !err.config.url.includes('/api/auth/')) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        if (!location.hash.includes('/login')) {
          location.hash = '#/login'
        }
      }
    }
    return Promise.reject(err)
  },
)

/** URL publique de streaming (utilisable directement dans <audio src>). */
export const streamUrl = (trackId) => `${API_URL}/api/tracks/${trackId}/stream`

export default http
