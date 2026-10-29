const TOKEN_KEY = 'task-api-demo-token'

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
}

export function clearToken(): void {
  try {
    sessionStorage.removeItem(TOKEN_KEY)
  } catch {
    // ignored: see getToken
  }
}
