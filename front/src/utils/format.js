/** Secondes -> "m:ss" (ex. 183 -> "3:03"). */
export function formatDuration(seconds) {
  if (seconds == null || isNaN(seconds)) return '0:00'
  const total = Math.floor(seconds)
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

/** Secondes -> duree lisible (ex. 3780 -> "1 h 03", 720 -> "12 min"). */
export function formatTotal(seconds) {
  if (!seconds) return '0 min'
  const m = Math.round(seconds / 60)
  if (m < 60) return `${m} min`
  const h = Math.floor(m / 60)
  const rem = m % 60
  return `${h} h ${rem.toString().padStart(2, '0')}`
}

/** Initiales pour les avatars / labels de disque. */
export function initials(text) {
  if (!text) return '♪'
  return text
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
}
