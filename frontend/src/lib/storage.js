export function lsGet(key, fallback) {
  try {
    const v = localStorage.getItem(key)
    if (v === null || v === undefined) return fallback
    return JSON.parse(v)
  } catch {
    return fallback
  }
}

export function lsSet(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

export function lsDel(key) {
  localStorage.removeItem(key)
}
