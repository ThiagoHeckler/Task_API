import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { vi } from 'vitest'

type Route = () => Response

export function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function noContent(): Response {
  return new Response(null, { status: 204 })
}

// Routes keyed by "METHOD /api/path". An array answers in order and then
// keeps repeating its last response (e.g. the list before and after a save).
export function stubApi(routes: Record<string, Route | Route[]>) {
  const fetchMock = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>((url, init) => {
    const key = `${init?.method ?? 'GET'} ${url}`
    const route = routes[key]
    if (!route) return Promise.reject(new Error(`Unexpected request: ${key}`))
    const handler = Array.isArray(route) ? (route.length > 1 ? route.shift()! : route[0]) : route
    return Promise.resolve(handler())
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

export function requestsOf(fetchMock: ReturnType<typeof stubApi>): string[] {
  return fetchMock.mock.calls.map(([url, init]) => `${init?.method ?? 'GET'} ${url}`)
}

export function bodyOf(fetchMock: ReturnType<typeof stubApi>, request: string): unknown {
  const index = requestsOf(fetchMock).indexOf(request)
  return JSON.parse(fetchMock.mock.calls[index][1]?.body as string)
}

export function renderWithQueryClient(ui: ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}
