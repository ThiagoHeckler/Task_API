import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { api, ApiError, configureApi, isDailyLimit } from './api'
import { clearToken, setToken } from './session'
import { clearTimeline, getTimeline } from './timeline'

const TOKEN = 'eyJhbGciOiJIUzI1NiJ9.payload.signature'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubFetch(response: Response | Error) {
  const fetchMock = vi.fn(() =>
    response instanceof Error ? Promise.reject(response) : Promise.resolve(response),
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

beforeEach(() => {
  configureApi({ onUnauthorized: () => {} })
})

afterEach(() => {
  vi.unstubAllGlobals()
  clearToken()
  clearTimeline()
})

test('sendsBearerTokenWhenLoggedIn', async () => {
  setToken(TOKEN)
  const fetchMock = stubFetch(jsonResponse(200, []))

  await api('GET', '/tasks')

  const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
  expect(url).toBe('/api/tasks')
  expect((init.headers as Record<string, string>).Authorization).toBe(`Bearer ${TOKEN}`)
})

test('abbreviatesAuthorizationHeader', async () => {
  setToken(TOKEN)
  stubFetch(jsonResponse(200, []))

  await api('GET', '/tasks')

  const [entry] = getTimeline()
  expect(entry.path).toBe('/api/tasks')
  expect(JSON.stringify(entry)).not.toContain(TOKEN)
  expect(entry.requestHeaders.Authorization).toBe(`Bearer ${TOKEN.slice(0, 10)}…`)
})

test('recordsRealErrorBody', async () => {
  const body = { status: 400, error: 'Bad Request', fields: { title: 'O título é obrigatório' } }
  stubFetch(jsonResponse(400, body))

  const error = await api('POST', '/tasks', { title: '' }).catch((e: unknown) => e)

  expect(error).toBeInstanceOf(ApiError)
  expect((error as ApiError).fields.title).toBe('O título é obrigatório')
  const [entry] = getTimeline()
  expect(entry.status).toBe(400)
  expect(entry.responseBody).toEqual(body)
  expect(entry.requestBody).toEqual({ title: '' })
})

test('callsOnUnauthorizedOn401WithToken', async () => {
  const onUnauthorized = vi.fn()
  configureApi({ onUnauthorized })
  setToken(TOKEN)
  stubFetch(jsonResponse(401, { status: 401, error: 'Unauthorized', message: 'Token inválido' }))

  await expect(api('GET', '/tasks')).rejects.toBeInstanceOf(ApiError)

  expect(onUnauthorized).toHaveBeenCalledTimes(1)
})

test('doesNotCallOnUnauthorizedWithoutToken', async () => {
  const onUnauthorized = vi.fn()
  configureApi({ onUnauthorized })
  stubFetch(jsonResponse(401, { status: 401, error: 'Unauthorized', message: 'Credenciais inválidas' }))

  await expect(api('POST', '/auth/login', {})).rejects.toBeInstanceOf(ApiError)

  expect(onUnauthorized).not.toHaveBeenCalled()
})

test('classifiesDailyLimit', async () => {
  stubFetch(
    jsonResponse(429, {
      status: 429,
      error: 'Too Many Requests',
      message: 'Limite de usuários que testaram o sistema hoje atingido',
    }),
  )
  const daily = await api('POST', '/users', {}).catch((e: unknown) => e)

  stubFetch(
    jsonResponse(429, {
      status: 429,
      error: 'Too Many Requests',
      message: 'Muitas requisições. Aguarde um minuto e tente novamente.',
    }),
  )
  const perMinute = await api('POST', '/users', {}).catch((e: unknown) => e)

  expect(isDailyLimit(daily)).toBe(true)
  expect(isDailyLimit(perMinute)).toBe(false)
  expect(isDailyLimit(new Error('x'))).toBe(false)
})

test('recordsNetworkFailureAsStatusZero', async () => {
  stubFetch(new TypeError('Failed to fetch'))

  await expect(api('GET', '/tasks')).rejects.toThrow()

  const [entry] = getTimeline()
  expect(entry.status).toBe(0)
  expect(entry.durationMs).not.toBeNull()
})

test('returnsUndefinedOn204', async () => {
  stubFetch(new Response(null, { status: 204 }))

  const result = await api('DELETE', '/tasks/1')

  expect(result).toBeUndefined()
  const [entry] = getTimeline()
  expect(entry.status).toBe(204)
  expect(entry.responseBody).toBeNull()
})

test('abbreviatesTokenInResponseBody', async () => {
  stubFetch(jsonResponse(200, { token: TOKEN, type: 'Bearer', expiresIn: 3600 }))

  const login = await api<{ token: string }>('POST', '/auth/login', { email: 'a@b', password: 'x' })

  expect(login.token).toBe(TOKEN)
  const [entry] = getTimeline()
  expect(JSON.stringify(entry)).not.toContain(TOKEN)
  expect(entry.responseBody).toEqual({ token: `${TOKEN.slice(0, 10)}…`, type: 'Bearer', expiresIn: 3600 })
})
