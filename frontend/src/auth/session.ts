import { useSyncExternalStore } from 'react'

/*
 * Where the JWT is kept, and the one tradeoff worth understanding here.
 *
 * XSS TRADEOFF: localStorage is readable by any JavaScript running on this
 * origin. That means a cross-site-scripting hole anywhere in this app -- our own
 * code or any npm dependency it pulls in -- can read the token and impersonate
 * the user for as long as it stays valid. The safer alternative is an HttpOnly
 * Secure SameSite cookie, which script simply cannot read; the cost is that the
 * server has to issue and clear cookies, and CSRF protection comes back onto the
 * table because the browser would then attach the credential automatically.
 *
 * We accept the exposure for this iteration and keep the blast radius small:
 * tokens expire after an hour (security.jwt.expiration on the server), the
 * password itself is never stored, and the token is dropped the moment the API
 * answers 401.
 */
const TOKEN_KEY = 'csci602.auth.token'
const USER_ID_KEY = 'csci602.auth.userId'
const USERNAME_KEY = 'csci602.auth.username'

export type Session = {
  readonly token: string
  readonly userId: number
  readonly username: string
}

function readFromStorage(): Session | null {
  const token = localStorage.getItem(TOKEN_KEY)
  const storedUserId = localStorage.getItem(USER_ID_KEY)
  if (!token || !storedUserId) {
    return null
  }

  const userId = Number(storedUserId)
  if (!Number.isInteger(userId)) {
    return null
  }

  return { token, userId, username: localStorage.getItem(USERNAME_KEY) ?? '' }
}

/*
 * useSyncExternalStore compares snapshots by identity, so getSnapshot has to
 * return the very same object until something actually changes. Reading
 * localStorage on every call would build a new object each time and spin the
 * component forever, hence this cache.
 */
let cached: Session | null = readFromStorage()
let listeners: Array<() => void> = []

function emit(): void {
  for (const listener of listeners) {
    listener()
  }
}

function subscribe(listener: () => void): () => void {
  listeners = [...listeners, listener]
  return () => {
    listeners = listeners.filter((candidate) => candidate !== listener)
  }
}

export function getSession(): Session | null {
  return cached
}

export function saveSession(session: Session): void {
  localStorage.setItem(TOKEN_KEY, session.token)
  localStorage.setItem(USER_ID_KEY, String(session.userId))
  localStorage.setItem(USERNAME_KEY, session.username)
  cached = session
  emit()
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_ID_KEY)
  localStorage.removeItem(USERNAME_KEY)
  cached = null
  emit()
}

/** Re-renders the caller whenever the session is saved or cleared. */
export function useSession(): Session | null {
  return useSyncExternalStore(subscribe, getSession)
}

// Keeps other tabs honest: logging out in one tab logs out the rest.
window.addEventListener('storage', (event) => {
  if (event.key !== null
      && event.key !== TOKEN_KEY
      && event.key !== USER_ID_KEY
      && event.key !== USERNAME_KEY) {
    return
  }
  cached = readFromStorage()
  emit()
})
