import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, test } from 'vitest'
import { addEntry, clearTimeline, type TimelineEntry } from '@/lib/timeline'
import { TimelinePanel } from './TimelinePanel'

function entry(overrides: Partial<Omit<TimelineEntry, 'id'>>): Omit<TimelineEntry, 'id'> {
  return {
    method: 'GET',
    path: '/api/tasks',
    requestHeaders: { Accept: 'application/json' },
    requestBody: null,
    status: 200,
    responseBody: [],
    durationMs: 15,
    startedAt: 0,
    ...overrides,
  }
}

afterEach(() => {
  act(() => clearTimeline())
})

test('showsEmptyState', () => {
  render(<TimelinePanel waiting={false} />)

  expect(screen.getByText('As chamadas da API aparecem aqui')).toBeInTheDocument()
})

test('showsEntriesNewestFirst', () => {
  addEntry(entry({ method: 'POST', path: '/api/users' }))
  addEntry(entry({ method: 'GET', path: '/api/tasks' }))

  render(<TimelinePanel waiting={false} />)

  const rows = screen.getAllByRole('listitem')
  expect(within(rows[0]).getByText('/api/tasks')).toBeInTheDocument()
  expect(within(rows[1]).getByText('/api/users')).toBeInTheDocument()
})

test('expandsRowToShowRealBodies', async () => {
  addEntry(
    entry({
      method: 'POST',
      path: '/api/tasks',
      requestBody: { title: '' },
      status: 400,
      responseBody: { status: 400, fields: { title: 'O título é obrigatório' } },
    }),
  )
  addEntry(entry({ path: '/api/tags' }))
  render(<TimelinePanel waiting={false} />)

  // The newest starts expanded; the older one is collapsed until clicked.
  expect(screen.queryByText(/O título é obrigatório/)).not.toBeInTheDocument()

  await userEvent.click(screen.getByRole('button', { name: /\/api\/tasks/ }))

  expect(screen.getByText(/O título é obrigatório/)).toBeInTheDocument()
  expect(screen.getByText(/"title": ""/)).toBeInTheDocument()
})

test('showsWaitingLine', () => {
  render(<TimelinePanel waiting />)

  expect(screen.getByText('aguardando você salvar…')).toBeInTheDocument()
})

test('keepsLoginResponseOpenAndHighlightsToken', () => {
  addEntry(
    entry({
      method: 'POST',
      path: '/api/auth/login',
      status: 200,
      responseBody: { token: 'eyJhbGciOi…', type: 'Bearer', expiresIn: 3600 },
    }),
  )
  addEntry(entry({ path: '/api/tasks' }))

  render(<TimelinePanel waiting={false} />)

  expect(screen.getByRole('button', { name: /\/api\/auth\/login/ })).toHaveAttribute('aria-expanded', 'true')
  expect(screen.getByText('Token JWT gerado')).toBeInTheDocument()
  expect(screen.getByText(/eyJhbGciOi…/)).toBeInTheDocument()
})
