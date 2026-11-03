import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, test, vi } from 'vitest'
import { clearTimeline } from '@/lib/timeline'
import {
  bodyOf,
  createTestQueryClient,
  jsonResponse,
  renderWithQueryClient,
  requestsOf,
  stubApi,
} from '@/test/api'
import { TagsPage } from './TagsPage'

afterEach(() => {
  vi.unstubAllGlobals()
  clearTimeline()
})

async function waitForTypingThenSave() {
  const save = screen.getByRole('button', { name: 'Salvar' })
  await waitFor(() => expect(save).toBeEnabled())
  await userEvent.click(save)
}

test('createsTagWithPreset', async () => {
  const created = { id: 1, name: 'urgente' }
  const fetchMock = stubApi({
    'GET /api/tags': [() => jsonResponse(200, []), () => jsonResponse(200, [created])],
    'POST /api/tags': () => jsonResponse(201, created),
  })
  renderWithQueryClient(<TagsPage onWaitingChange={() => {}} />)
  await screen.findByText('Nenhuma tag ainda')

  await userEvent.click(screen.getByRole('button', { name: 'Nova tag' }))
  await waitForTypingThenSave()

  await waitFor(() =>
    expect(requestsOf(fetchMock)).toEqual(['GET /api/tags', 'POST /api/tags', 'GET /api/tags']),
  )
  expect(bodyOf(fetchMock, 'POST /api/tags')).toEqual({ name: 'urgente' })
  expect(await screen.findByText('urgente')).toBeInTheDocument()
})

test('editsTag', async () => {
  const edited = { id: 3, name: 'leitura-editada' }
  const fetchMock = stubApi({
    'GET /api/tags': [() => jsonResponse(200, [{ id: 3, name: 'leitura' }]), () => jsonResponse(200, [edited])],
    'PUT /api/tags/3': () => jsonResponse(200, edited),
  })
  renderWithQueryClient(<TagsPage onWaitingChange={() => {}} />)
  await screen.findByText('leitura')

  await userEvent.click(screen.getByRole('button', { name: 'Editar leitura' }))
  expect(screen.getByLabelText('Nome')).toHaveValue('leitura')
  await waitForTypingThenSave()

  await waitFor(() =>
    expect(requestsOf(fetchMock)).toEqual(['GET /api/tags', 'PUT /api/tags/3', 'GET /api/tags']),
  )
  expect(bodyOf(fetchMock, 'PUT /api/tags/3')).toEqual({ name: 'leitura-editada' })
  expect(await screen.findByText('leitura-editada')).toBeInTheDocument()
})

test('editingTagMarksTasksStale', async () => {
  const edited = { id: 3, name: 'leitura-editada' }
  stubApi({
    'GET /api/tags': [() => jsonResponse(200, [{ id: 3, name: 'leitura' }]), () => jsonResponse(200, [edited])],
    'PUT /api/tags/3': () => jsonResponse(200, edited),
  })
  const queryClient = createTestQueryClient()
  queryClient.setQueryData(['tasks', 'all'], [{ id: 9, tags: [{ id: 3, name: 'leitura' }] }])
  renderWithQueryClient(<TagsPage onWaitingChange={() => {}} />, queryClient)
  await screen.findByText('leitura')

  await userEvent.click(screen.getByRole('button', { name: 'Editar leitura' }))
  await waitForTypingThenSave()

  await waitFor(() =>
    expect(queryClient.getQueryState(['tasks', 'all'])?.isInvalidated).toBe(true),
  )
})
