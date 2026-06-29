import http from './http'

export const tracksApi = {
  list(params = {}) {
    return http.get('/api/tracks', { params }).then((r) => r.data)
  },
  get(id) {
    return http.get(`/api/tracks/${id}`).then((r) => r.data)
  },
  create(file, fields) {
    const fd = new FormData()
    fd.append('file', file)
    Object.entries(fields).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') fd.append(k, v)
    })
    return http.post('/api/tracks', fd).then((r) => r.data)
  },
  update(id, body) {
    return http.put(`/api/tracks/${id}`, body).then((r) => r.data)
  },
  remove(id) {
    return http.delete(`/api/tracks/${id}`)
  },
}
