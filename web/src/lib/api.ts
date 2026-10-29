import { getToken } from './session'
import { addEntry, updateEntry, type TimelineEntry } from './timeline'
import type { ErrorResponse, ValidationErrorResponse } from './types'

export const API_BASE = '/api'

// Same text as SignupLimitReachedException.MESSAGE on the API.
const DAILY_LIMIT_MESSAGE = 'Limite de usuários que testaram o sistema hoje atingido'

type Method = TimelineEntry['method']

export class ApiError extends Error {
  status: number
  body: ErrorResponse | ValidationErrorResponse | null
  fields: Record<string, string>

  constructor(status: number, body: ErrorResponse | ValidationErrorResponse | null) {
    super(body && 'message' in body ? body.message : `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.body = body
    this.fields = body && 'fields' in body ? body.fields : {}
  }
}

let onUnauthorized: () => void = () => {}

export function configureApi(options: { onUnauthorized: () => void }): void {
  onUnauthorized = options.onUnauthorized
}

export function isDailyLimit(err: unknown): boolean {
  return (
    err instanceof ApiError &&
    err.status === 429 &&
    err.body !== null &&
    'message' in err.body &&
    err.body.message === DAILY_LIMIT_MESSAGE
  )
}

// The timeline is on screen (and may end up in a screenshot): never show a
// whole token there, neither in the Authorization header nor in the login body.
function abbreviateToken(token: string): string {
  return `${token.slice(0, 10)}…`
}

function headersForTimeline(headers: Record<string, string>): Record<string, string> {
  const token = headers.Authorization?.slice('Bearer '.length)
  if (!token) return headers
  return { ...headers, Authorization: `Bearer ${abbreviateToken(token)}` }
}

function bodyForTimeline(body: unknown): unknown {
  if (body === null || typeof body !== 'object' || !('token' in body)) return body
  if (typeof body.token !== 'string') return body
  return { ...body, token: abbreviateToken(body.token) }
}

async function readBody(response: Response): Promise<unknown> {
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export async function api<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const url = `${API_BASE}${path}`
  const token = getToken()
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  const startedAt = Date.now()
  const entryId = addEntry({
    method,
    path: url,
    requestHeaders: headersForTimeline(headers),
    requestBody: body ?? null,
    status: null,
    responseBody: null,
    durationMs: null,
    startedAt,
  })

  let response: Response
  try {
    response = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch (error) {
    updateEntry(entryId, { status: 0, durationMs: Date.now() - startedAt })
    throw error
  }

  const responseBody = await readBody(response)
  updateEntry(entryId, {
    status: response.status,
    responseBody: bodyForTimeline(responseBody),
    durationMs: Date.now() - startedAt,
  })

  if (!response.ok) {
    if (response.status === 401 && token) onUnauthorized()
    const errorBody =
      responseBody !== null && typeof responseBody === 'object'
        ? (responseBody as ErrorResponse | ValidationErrorResponse)
        : null
    throw new ApiError(response.status, errorBody)
  }

  return (responseBody ?? undefined) as T
}
