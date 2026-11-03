import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { clearToken, getToken } from '@/lib/session'
import { clearTimeline } from '@/lib/timeline'
import { Onboarding } from './Onboarding'

const DAILY_LIMIT = 'Limite de usuários que testaram o sistema hoje atingido'
const IP_LIMIT = 'Muitas requisições. Aguarde um minuto e tente novamente.'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubFetch(routes: Record<string, () => Response>) {
  const fetchMock = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>((url) =>
    Promise.resolve(routes[url]()),
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function requestBody(fetchMock: ReturnType<typeof stubFetch>, call: number) {
  const init = fetchMock.mock.calls[call][1]
  return JSON.parse(init?.body as string) as Record<string, string>
}

// Typing runs on fake timers, one act per tick so React commits in between;
// the API flow after the click runs on real ones.
function finishTyping() {
  while (vi.getTimerCount() > 0) {
    act(() => {
      vi.runOnlyPendingTimers()
    })
  }
  vi.useRealTimers()
}

function renderOnboarding() {
  const onLoggedIn = vi.fn()
  const onWaitingChange = vi.fn()
  render(<Onboarding onLoggedIn={onLoggedIn} onWaitingChange={onWaitingChange} />)
  finishTyping()
  return { onLoggedIn, onWaitingChange }
}

function clickCreate() {
  fireEvent.click(screen.getByRole('button', { name: 'Criar conta e entrar' }))
}

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
  clearToken()
  act(() => clearTimeline())
})

test('waitsForClickBeforeCallingApi', () => {
  const fetchMock = stubFetch({})

  const { onWaitingChange } = renderOnboarding()

  expect(fetchMock).not.toHaveBeenCalled()
  expect(screen.getByLabelText('Email')).not.toHaveValue('')
  expect(screen.getByRole('button', { name: 'Criar conta e entrar' })).toBeEnabled()
  expect(screen.getByText('Conta temporária de demonstração, apagada em 24 h')).toBeInTheDocument()
  expect(onWaitingChange).toHaveBeenLastCalledWith(true)
})

test('createsUserThenLogsIn', async () => {
  const fetchMock = stubFetch({
    '/api/users': () =>
      jsonResponse(201, { id: 1, name: 'Visitante', email: 'v@x', createdAt: '2026-10-29T12:00:00' }),
    '/api/auth/login': () => jsonResponse(200, { token: 'jwt-token-123', type: 'Bearer', expiresIn: 3600 }),
  })
  const { onLoggedIn } = renderOnboarding()
  const email = (screen.getByLabelText('Email') as HTMLInputElement).value

  clickCreate()

  await waitFor(() => expect(onLoggedIn).toHaveBeenCalledTimes(1))
  expect(fetchMock.mock.calls.map((call) => call[0])).toEqual(['/api/users', '/api/auth/login'])
  expect(requestBody(fetchMock, 0).email).toBe(email)
  expect(requestBody(fetchMock, 1)).toEqual({
    email,
    password: requestBody(fetchMock, 0).password,
  })
  expect(getToken()).toBe('jwt-token-123')
})

test('sendsWhatTheVisitorTyped', async () => {
  const fetchMock = stubFetch({
    '/api/users': () => jsonResponse(400, { status: 400, error: 'Bad Request', fields: {} }),
  })
  render(<Onboarding onLoggedIn={() => {}} onWaitingChange={() => {}} />)
  act(() => {
    vi.advanceTimersByTime(50)
  })

  fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Thiago' } })
  finishTyping()
  clickCreate()

  await waitFor(() => expect(fetchMock).toHaveBeenCalled())
  expect(requestBody(fetchMock, 0).name).toBe('Thiago')
})

test('showsLockScreenOnDailyLimit', async () => {
  stubFetch({
    '/api/users': () =>
      jsonResponse(429, { status: 429, error: 'Too Many Requests', message: DAILY_LIMIT }),
  })
  renderOnboarding()

  clickCreate()

  expect(await screen.findByText(DAILY_LIMIT)).toBeInTheDocument()
  expect(screen.getByText(/meia-noite \(horário de Brasília\)/)).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Ver somente API' })).toHaveAttribute(
    'href',
    '/api/swagger-ui/index.html',
  )
  expect(screen.queryByRole('button', { name: 'Criar conta e entrar' })).not.toBeInTheDocument()
})

test('showsWaitWarningOnIpLimit', async () => {
  stubFetch({
    '/api/users': () => jsonResponse(429, { status: 429, error: 'Too Many Requests', message: IP_LIMIT }),
  })
  renderOnboarding()

  clickCreate()

  expect(await screen.findByText(IP_LIMIT)).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Criar conta e entrar' })).toBeEnabled()
})

test('showsFieldErrorsOn400', async () => {
  stubFetch({
    '/api/users': () =>
      jsonResponse(400, { status: 400, error: 'Bad Request', fields: { email: 'Email inválido' } }),
  })
  renderOnboarding()

  clickCreate()

  expect(await screen.findByText('Email inválido')).toBeInTheDocument()
  expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
})

test('retryAfterFailedLoginOnlyLogsIn', async () => {
  const fetchMock = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>()
  fetchMock
    .mockResolvedValueOnce(
      jsonResponse(201, { id: 1, name: 'V', email: 'v@x', createdAt: '2026-10-29T12:00:00' }),
    )
    .mockResolvedValueOnce(jsonResponse(429, { status: 429, error: 'Too Many Requests', message: IP_LIMIT }))
    .mockResolvedValueOnce(jsonResponse(200, { token: 'jwt-token-123', type: 'Bearer', expiresIn: 3600 }))
  vi.stubGlobal('fetch', fetchMock)
  const { onLoggedIn } = renderOnboarding()

  clickCreate()
  await screen.findByText(IP_LIMIT)
  clickCreate()

  await waitFor(() => expect(onLoggedIn).toHaveBeenCalledTimes(1))
  expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
    '/api/users',
    '/api/auth/login',
    '/api/auth/login',
  ])
})
