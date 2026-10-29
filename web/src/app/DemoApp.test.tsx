import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { clearToken, getToken, setToken } from '@/lib/session'
import { clearTimeline } from '@/lib/timeline'
import { DemoApp } from './DemoApp'

afterEach(() => {
  vi.unstubAllGlobals()
  clearToken()
  act(() => clearTimeline())
})

test('unauthorizedReturnsToOnboarding', async () => {
  setToken('expired-token')
  const queryClient = new QueryClient()
  queryClient.setQueryData(['tasks'], [{ id: 1 }])
  // The system opens on Tarefas, so its GET /api/tasks is the call that gets the 401.
  vi.stubGlobal(
    'fetch',
    vi.fn(() =>
      Promise.resolve(
        new Response(JSON.stringify({ status: 401, error: 'Unauthorized', message: 'Token inválido' }), {
          status: 401,
        }),
      ),
    ),
  )
  render(
    <QueryClientProvider client={queryClient}>
      <DemoApp />
    </QueryClientProvider>,
  )
  expect(screen.queryByRole('button', { name: 'Criar conta e entrar' })).not.toBeInTheDocument()

  expect(await screen.findByRole('button', { name: 'Criar conta e entrar' })).toBeInTheDocument()
  expect(getToken()).toBeNull()
  expect(queryClient.getQueryData(['tasks'])).toBeUndefined()
  expect(queryClient.getQueryData(['tasks', 'all'])).toBeUndefined()
  expect(
    screen.getByText('Sua sessão expirou. Crie uma nova conta de demonstração.'),
  ).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Criar conta e entrar' })).toBeInTheDocument()
})
