import { useEffect, useSyncExternalStore } from 'react'
import { api } from '../api/api'
import type { SiteSettings } from '../types'

/**
 * Website texts (name, phone, hours ...) are loaded from the database through the API ("dynamic page").
 * One shared copy is kept here so that when the owner saves changes, the navbar/footer/home page update at once.
 */
let current: SiteSettings | null = null
let started = false
const listeners = new Set<() => void>()

function emit(): void {
  listeners.forEach((l) => l())
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function setSiteSettings(settings: SiteSettings): void {
  current = settings
  emit()
}

export function useSiteSettings(): { settings: SiteSettings | null } {
  const settings = useSyncExternalStore(subscribe, () => current)

  useEffect(() => {
    if (started) {
      return
    }
    started = true
    api
      .settings()
      .then(setSiteSettings)
      .catch(() => {
        started = false // try again next time a component mounts
      })
  }, [])

  return { settings }
}
