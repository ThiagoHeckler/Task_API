import { useSyncExternalStore } from 'react'

const TOKEN_KEY = 'task-api-demo-token'

// Components re-render when the token is set or cleared (login, 401).
const listeners = new Set<() => void>()

function emit() {
  listeners.forEach((listener) => listener())
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

// sessionStorage can throw (private mode, blocked storage); the demo then
// just behaves as logged out.
export function getToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token: string): void {
  try {
    sessionStorage.setItem(TOKEN_KEY, token)
  } catch {
    // ignored: see getToken
  }
  emit()
}

export function clearToken(): void {
  try {
    sessionStorage.removeItem(TOKEN_KEY)
  } catch {
    // ignored: see getToken
  }
  emit()
}

// The static export is prerendered without sessionStorage: logged out there,
// and the browser picks up a saved token on hydration.
export function useToken(): string | null {
  return useSyncExternalStore(subscribe, getToken, () => null)
}
