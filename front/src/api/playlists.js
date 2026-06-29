import http from './http'

export const playlistsApi = {
  generate(criteria) {
    return http.post('/api/playlists/generate', criteria).then((r) => r.data)
  },
  save(body) {
    return http.post('/api/playlists', body).then((r) => r.data)
  },
  list() {
    return http.get('/api/playlists').then((r) => r.data)
  },
  get(id) {
    return http.get(`/api/playlists/${id}`).then((r) => r.data)
  },
  update(id, body) {
    return http.put(`/api/playlists/${id}`, body).then((r) => r.data)
  },
  remove(id) {
    return http.delete(`/api/playlists/${id}`)
  },
  async download(id, name = 'playlist') {
    const res = await http.get(`/api/playlists/${id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = `${name}.zip`
    a.click()
    URL.revokeObjectURL(url)
  },
}
