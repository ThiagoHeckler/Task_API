import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, test, vi } from 'vitest'
import { DemoApp } from '@/app/DemoApp'
import { clearToken, getToken, setToken } from '@/lib/session'
import { clearTimeline } from '@/lib/timeline'
import { bodyOf, jsonResponse, noContent, renderWithQueryClient, requestsOf, stubApi } from '@/test/api'
import { AccountPage } from './AccountPage'

const ME = { id: 1, name: 'Visitante 1234', email: 'visitante@demo.thiagoheckler.com.br', createdAt: '2026-10-29T02:10:25' }

afterEach(() => {
  vi.unstubAllGlobals()
  clearToken()
  act(() => clearTimeline())
})

test('updatesName', async () => {
  const renamed = { ...ME, name: 'Visitante 1234 (editado)' }
  const fetchMock = stubApi({
    'GET /api/users/me': [() => jsonResponse(200, ME), () => jsonResponse(200, renamed)],
    'PUT /api/users/me': () => jsonResponse(200, renamed),
  })
  renderWithQueryClient(<AccountPage onWaitingChange={() => {}} />)
  expect(await screen.findByText('Visitante 1234')).toBeInTheDocument()
  expect(screen.getByText('28/10/2026 23:10')).toBeInTheDocument()

  await userEvent.click(screen.getByRole('button', { name: 'Editar nome' }))
  const save = screen.getByRole('button', { name: 'Salvar' })
  await waitFor(() => expect(save).toBeEnabled())
  await userEvent.click(save)

  await waitFor(() =>
    expect(requestsOf(fetchMock)).toEqual(['GET /api/users/me', 'PUT /api/users/me', 'GET /api/users/me']),
  )
  expect(bodyOf(fetchMock, 'PUT /api/users/me')).toEqual({ name: 'Visitante 1234 (editado)' })
  expect(await screen.findByText('Visitante 1234 (editado)')).toBeInTheDocument()
})

test('deletingAccountReturnsToOnboarding', async () => {
  setToken('valid-token')
  const queryClient = new QueryClient()
  const fetchMock = stubApi({
    'GET /api/tasks': () => jsonResponse(200, []),
    'GET /api/users/me': () => jsonResponse(200, ME),
    'DELETE /api/users/me': noContent,
  })
  render(
    <QueryClientProvider client={queryClient}>
      <DemoApp />
    </QueryClientProvider>,
  )

  await userEvent.click(screen.getByRole('button', { name: 'Minha conta' }))
  await screen.findByText('Visitante 1234')
  await userEvent.click(screen.getByRole('button', { name: 'Excluir conta' }))
  await userEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão da conta' }))

  expect(await screen.findByRole('button', { name: 'Criar conta e entrar' })).toBeInTheDocument()
  // Tarefas is the section the system opens on.
  expect(requestsOf(fetchMock)).toEqual(['GET /api/tasks', 'GET /api/users/me', 'DELETE /api/users/me'])
  expect(getToken()).toBeNull()
  expect(queryClient.getQueryData(['me'])).toBeUndefined()
})
