import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, test, vi } from 'vitest'
import { clearTimeline } from '@/lib/timeline'
import { bodyOf, jsonResponse, noContent, renderWithQueryClient, requestsOf, stubApi } from '@/test/api'
import { CategoriesPage } from './CategoriesPage'

const WORK = { id: 1, name: 'Trabalho', color: '#0ea5e9' }

afterEach(() => {
  vi.unstubAllGlobals()
  clearTimeline()
})

async function waitForTypingThenSave() {
  const save = screen.getByRole('button', { name: 'Salvar' })
  await waitFor(() => expect(save).toBeEnabled())
  await userEvent.click(save)
}

test('listsCategoriesOnOpen', async () => {
  const fetchMock = stubApi({ 'GET /api/categories': () => jsonResponse(200, [WORK]) })

  renderWithQueryClient(<CategoriesPage onWaitingChange={() => {}} />)

  expect(await screen.findByText('Trabalho')).toBeInTheDocument()
  expect(requestsOf(fetchMock)).toEqual(['GET /api/categories'])
})

test('createsCategoryWithPreset', async () => {
  const created = { id: 2, name: 'Estudos', color: '#4f46e5' }
  const fetchMock = stubApi({
    'GET /api/categories': [() => jsonResponse(200, []), () => jsonResponse(200, [created])],
    'POST /api/categories': () => jsonResponse(201, created),
  })
  const onWaitingChange = vi.fn()
  renderWithQueryClient(<CategoriesPage onWaitingChange={onWaitingChange} />)
  await screen.findByText('Nenhuma categoria ainda')

  await userEvent.click(screen.getByRole('button', { name: 'Nova categoria' }))
  expect(onWaitingChange).toHaveBeenLastCalledWith(true)
  await waitForTypingThenSave()

  await waitFor(() =>
    expect(requestsOf(fetchMock)).toEqual([
      'GET /api/categories',
      'POST /api/categories',
      'GET /api/categories',
    ]),
  )
  expect(bodyOf(fetchMock, 'POST /api/categories')).toEqual({ name: 'Estudos', color: '#4f46e5' })
  expect(await screen.findByText('Estudos')).toBeInTheDocument()
  expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
  expect(onWaitingChange).toHaveBeenLastCalledWith(false)
})

test('showsConflictMessageOn409', async () => {
  stubApi({
    'GET /api/categories': () => jsonResponse(200, []),
    'POST /api/categories': () =>
      jsonResponse(409, { status: 409, error: 'Conflict', message: 'Categoria já existe: Estudos' }),
  })
  renderWithQueryClient(<CategoriesPage onWaitingChange={() => {}} />)
  await screen.findByText('Nenhuma categoria ainda')

  await userEvent.click(screen.getByRole('button', { name: 'Nova categoria' }))
  await waitForTypingThenSave()

  expect(await screen.findByText('Categoria já existe: Estudos')).toBeInTheDocument()
  expect(screen.getByLabelText('Nome')).toBeInTheDocument()
})

test('showsFieldErrorOn400', async () => {
  stubApi({
    'GET /api/categories': () => jsonResponse(200, []),
    'POST /api/categories': () =>
      jsonResponse(400, {
        status: 400,
        error: 'Bad Request',
        fields: { color: 'A cor deve estar no formato hexadecimal (ex: #6366f1)' },
      }),
  })
  renderWithQueryClient(<CategoriesPage onWaitingChange={() => {}} />)
  await screen.findByText('Nenhuma categoria ainda')

  await userEvent.click(screen.getByRole('button', { name: 'Nova categoria' }))
  await waitForTypingThenSave()

  expect(
    await screen.findByText('A cor deve estar no formato hexadecimal (ex: #6366f1)'),
  ).toBeInTheDocument()
  expect(screen.getByLabelText('Cor')).toHaveAttribute('aria-invalid', 'true')
})

test('deletesAfterConfirmation', async () => {
  const fetchMock = stubApi({
    'GET /api/categories': [() => jsonResponse(200, [WORK]), () => jsonResponse(200, [])],
    'DELETE /api/categories/1': noContent,
  })
  renderWithQueryClient(<CategoriesPage onWaitingChange={() => {}} />)
  await screen.findByText('Trabalho')

  await userEvent.click(screen.getByRole('button', { name: 'Excluir Trabalho' }))
  expect(requestsOf(fetchMock)).toEqual(['GET /api/categories'])
  await userEvent.click(screen.getByRole('button', { name: 'Confirmar exclusão' }))

  await waitFor(() =>
    expect(requestsOf(fetchMock)).toEqual([
      'GET /api/categories',
      'DELETE /api/categories/1',
      'GET /api/categories',
    ]),
  )
  await waitFor(() => expect(screen.queryByText('Trabalho')).not.toBeInTheDocument())
})
