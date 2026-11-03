import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, test, vi } from 'vitest'
import { toApiDateTime } from '@/lib/dates'
import { clearTimeline } from '@/lib/timeline'
import type { TaskResponse } from '@/lib/types'
import {
  bodyOf,
  fastForwardUntil,
  jsonResponse,
  renderWithQueryClient,
  requestsOf,
  stubApi,
} from '@/test/api'
import { TasksPage } from './TasksPage'

function task(overrides: Partial<TaskResponse>): TaskResponse {
  return {
    id: 1,
    title: 'Revisar o README',
    description: null,
    status: 'PENDING',
    priority: 'MEDIUM',
    dueDate: null,
    completedAt: null,
    createdAt: '2026-10-29T12:00:00',
    updatedAt: '2026-10-29T12:00:00',
    category: null,
    tags: [],
    parentTaskId: null,
    ...overrides,
  }
}

const CATEGORY = { id: 5, name: 'Estudos', color: '#4f46e5' }
const TAG = { id: 7, name: 'urgente' }

afterEach(() => {
  vi.unstubAllGlobals()
  clearTimeline()
})

// The task form types ~100 characters, one async act per tick: fine alone,
// but over 5 s when the whole suite runs in parallel on a busy machine.
const TYPING_TEST_TIMEOUT_MS = 20_000

function saveIsEnabled() {
  const save = screen.queryByRole('button', { name: 'Salvar' })
  return save !== null && !save.hasAttribute('disabled')
}

test('createsTaskWithPresetAndConvertsDueDateToUtc', async () => {
  const created = task({ id: 9, title: 'Estudar Spring Security' })
  const fetchMock = stubApi({
    'GET /api/tasks': [() => jsonResponse(200, []), () => jsonResponse(200, [created])],
    'GET /api/categories': () => jsonResponse(200, [CATEGORY]),
    'GET /api/tags': () => jsonResponse(200, [TAG]),
    'POST /api/tasks': () => jsonResponse(201, created),
  })
  renderWithQueryClient(<TasksPage onWaitingChange={() => {}} />)
  await screen.findByText('Nenhuma tarefa ainda')

  fireEvent.click(screen.getByRole('button', { name: 'Nova tarefa' }))
  await fastForwardUntil(saveIsEnabled)
  const dueInput = (screen.getByLabelText('Prazo') as HTMLInputElement).value
  expect(dueInput).toMatch(/^\d{4}-\d{2}-\d{2}T18:00$/)
  await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))

  await waitFor(() => expect(requestsOf(fetchMock)).toContain('POST /api/tasks'))
  const body = bodyOf(fetchMock, 'POST /api/tasks') as Record<string, unknown>
  expect(body).toMatchObject({
    title: 'Estudar Spring Security',
    priority: 'HIGH',
    categoryId: 5,
    tagIds: [7],
  })
  // 18:00 in São Paulo is 21:00 UTC, sent without offset.
  expect(body.dueDate).toBe(toApiDateTime(dueInput))
  expect(body.dueDate).toMatch(/T21:00:00$/)
  expect(body).not.toHaveProperty('parentTaskId')
  await waitFor(() => expect(requestsOf(fetchMock).at(-1)).toBe('GET /api/tasks'))
}, TYPING_TEST_TIMEOUT_MS)

test('completingShowsCompletedAt', async () => {
  const pending = task({ id: 1 })
  const done = task({ id: 1, status: 'COMPLETED', completedAt: '2026-10-29T02:10:25' })
  const fetchMock = stubApi({
    'GET /api/tasks': [() => jsonResponse(200, [pending]), () => jsonResponse(200, [done])],
    'PUT /api/tasks/1': () => jsonResponse(200, done),
  })
  renderWithQueryClient(<TasksPage onWaitingChange={() => {}} />)
  await screen.findByText('Revisar o README')

  await userEvent.click(screen.getByRole('button', { name: 'Concluir Revisar o README' }))

  expect(await screen.findByText(/Concluída em 28\/10\/2026 23:10/)).toBeInTheDocument()
  expect(bodyOf(fetchMock, 'PUT /api/tasks/1')).toEqual({ status: 'COMPLETED' })
  expect(screen.getByRole('button', { name: 'Reabrir Revisar o README' })).toBeInTheDocument()
})

test('filterByStatusCallsQueryParam', async () => {
  const fetchMock = stubApi({
    'GET /api/tasks': () => jsonResponse(200, []),
    'GET /api/tasks?status=COMPLETED': () => jsonResponse(200, []),
  })
  renderWithQueryClient(<TasksPage onWaitingChange={() => {}} />)
  await screen.findByText('Nenhuma tarefa ainda')

  await userEvent.click(screen.getByRole('button', { name: 'Concluídas' }))

  await waitFor(() =>
    expect(requestsOf(fetchMock)).toEqual(['GET /api/tasks', 'GET /api/tasks?status=COMPLETED']),
  )
  expect(screen.getByRole('button', { name: 'Concluídas' })).toHaveAttribute('aria-pressed', 'true')
})

test('rootFilterCallsRootEndpoint', async () => {
  const fetchMock = stubApi({
    'GET /api/tasks': () => jsonResponse(200, []),
    'GET /api/tasks/root': () => jsonResponse(200, []),
  })
  renderWithQueryClient(<TasksPage onWaitingChange={() => {}} />)
  await screen.findByText('Nenhuma tarefa ainda')

  await userEvent.click(screen.getByRole('button', { name: 'Principais' }))

  await waitFor(() =>
    expect(requestsOf(fetchMock)).toEqual(['GET /api/tasks', 'GET /api/tasks/root']),
  )
})

test('nestsSubtasksUnderParent', async () => {
  stubApi({
    'GET /api/tasks': () =>
      jsonResponse(200, [task({ id: 1 }), task({ id: 2, title: 'Escrever a seção da demo', parentTaskId: 1 })]),
  })
  renderWithQueryClient(<TasksPage onWaitingChange={() => {}} />)

  const parent = (await screen.findByText('Revisar o README')).closest('li')!

  expect(within(parent).getByText('Escrever a seção da demo')).toBeInTheDocument()
})

test('showsBusinessRuleErrorOn422', async () => {
  const fetchMock = stubApi({
    'GET /api/tasks': () =>
      jsonResponse(200, [task({ id: 1 }), task({ id: 2, title: 'Escrever a seção da demo', parentTaskId: 1 })]),
    'GET /api/categories': () => jsonResponse(200, []),
    'GET /api/tags': () => jsonResponse(200, []),
    'POST /api/tasks': () =>
      jsonResponse(422, {
        status: 422,
        error: 'Unprocessable Content',
        message: 'Uma subtarefa não pode ter subtarefas',
      }),
  })
  renderWithQueryClient(<TasksPage onWaitingChange={() => {}} />)
  await screen.findByText('Revisar o README')

  fireEvent.click(screen.getByRole('button', { name: 'Nova tarefa' }))
  await fastForwardUntil(saveIsEnabled)
  const save = screen.getByRole('button', { name: 'Salvar' })
  await userEvent.selectOptions(screen.getByLabelText('Subtarefa de'), '2')
  await userEvent.click(save)

  expect(await screen.findByText('Uma subtarefa não pode ter subtarefas')).toBeInTheDocument()
  expect(bodyOf(fetchMock, 'POST /api/tasks')).toMatchObject({ parentTaskId: 2 })
  expect(screen.getByLabelText('Título')).toBeInTheDocument()
}, TYPING_TEST_TIMEOUT_MS)

test('deleteWarnsAboutSubtasks', async () => {
  stubApi({ 'GET /api/tasks': () => jsonResponse(200, [task({ id: 1 })]) })
  renderWithQueryClient(<TasksPage onWaitingChange={() => {}} />)
  await screen.findByText('Revisar o README')

  await userEvent.click(screen.getByRole('button', { name: 'Excluir Revisar o README' }))

  expect(screen.getByText('As subtarefas são excluídas junto')).toBeInTheDocument()
})


test('leavesOutPriorityWhenTypingStoppedBeforeIt', async () => {
  const fetchMock = stubApi({
    'GET /api/tasks': () => jsonResponse(200, []),
    'GET /api/categories': () => jsonResponse(200, []),
    'GET /api/tags': () => jsonResponse(200, []),
    'POST /api/tasks': () => jsonResponse(201, task({ id: 3, title: 'Minha tarefa' })),
  })
  renderWithQueryClient(<TasksPage onWaitingChange={() => {}} />)
  await screen.findByText('Nenhuma tarefa ainda')

  await userEvent.click(screen.getByRole('button', { name: 'Nova tarefa' }))
  // Editing the title stops the typewriter before it reaches the priority.
  fireEvent.change(await screen.findByLabelText('Título'), { target: { value: 'Minha tarefa' } })
  await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))

  await waitFor(() => expect(requestsOf(fetchMock)).toContain('POST /api/tasks'))
  expect(bodyOf(fetchMock, 'POST /api/tasks')).toEqual({ title: 'Minha tarefa' })
})

test('startingTaskSetsInProgress', async () => {
  const pending = task({ id: 1 })
  const started = task({ id: 1, status: 'IN_PROGRESS' })
  const fetchMock = stubApi({
    'GET /api/tasks': [() => jsonResponse(200, [pending]), () => jsonResponse(200, [started])],
    'PUT /api/tasks/1': () => jsonResponse(200, started),
  })
  renderWithQueryClient(<TasksPage onWaitingChange={() => {}} />)
  await screen.findByText('Revisar o README')

  await userEvent.click(screen.getByRole('button', { name: 'Iniciar Revisar o README' }))

  await waitFor(() => expect(screen.getByText('Em andamento', { selector: 'span' })).toBeInTheDocument())
  expect(bodyOf(fetchMock, 'PUT /api/tasks/1')).toEqual({ status: 'IN_PROGRESS' })
  expect(screen.getByRole('button', { name: 'Concluir Revisar o README' })).toBeInTheDocument()
})
