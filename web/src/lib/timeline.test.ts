import { renderHook } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { addEntry, clearTimeline, updateEntry, useTimeline } from './timeline'

const base = {
  method: 'GET' as const,
  requestHeaders: {},
  requestBody: null,
  status: null,
  responseBody: null,
  durationMs: null,
  startedAt: 0,
}

afterEach(() => {
  clearTimeline()
})

test('newestEntryComesFirst', () => {
  addEntry({ ...base, path: '/api/tasks' })
  addEntry({ ...base, path: '/api/tags' })

  const { result } = renderHook(() => useTimeline())

  expect(result.current.map((e) => e.path)).toEqual(['/api/tags', '/api/tasks'])
})

test('updateEntryChangesOnlyThatEntry', () => {
  const first = addEntry({ ...base, path: '/api/tasks' })
  const second = addEntry({ ...base, path: '/api/tags' })

  updateEntry(first, { status: 200, durationMs: 12 })

  const { result } = renderHook(() => useTimeline())
  const byId = Object.fromEntries(result.current.map((e) => [e.id, e]))
  expect(byId[first]).toMatchObject({ status: 200, durationMs: 12 })
  expect(byId[second]).toMatchObject({ status: null, durationMs: null })
})
