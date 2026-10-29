import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { useTypewriter } from './useTypewriter'

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

// Each tick schedules the next one from an effect, so React has to commit
// between timers: one act per pending batch.
function typeAll() {
  while (vi.getTimerCount() > 0) {
    act(() => {
      vi.runOnlyPendingTimers()
    })
  }
}

function tick(ms: number) {
  act(() => {
    vi.advanceTimersByTime(ms)
  })
}

test('typesFieldsInOrder', () => {
  const { result } = renderHook(() => useTypewriter({ a: 'ab', b: 'cd' }, { charDelayMs: 10 }))

  expect(result.current.values).toEqual({ a: '', b: '' })
  tick(10)
  expect(result.current.values).toEqual({ a: 'a', b: '' })
  tick(10)
  expect(result.current.values).toEqual({ a: 'ab', b: '' })
  tick(10)
  expect(result.current.values).toEqual({ a: 'ab', b: 'c' })
})

test('marksDoneAtEnd', () => {
  const { result } = renderHook(() => useTypewriter({ a: 'ab', b: 'cd' }, { charDelayMs: 10 }))
  expect(result.current.typing).toBe(true)

  typeAll()

  expect(result.current.values).toEqual({ a: 'ab', b: 'cd' })
  expect(result.current.done).toBe(true)
  expect(result.current.typing).toBe(false)
})

test('showsInstantFieldsAtOnce', () => {
  const { result } = renderHook(() =>
    useTypewriter({ a: 'a', when: '2026-11-30' }, { charDelayMs: 10, instant: ['when'] }),
  )

  tick(10)
  tick(10)

  expect(result.current.values).toEqual({ a: 'a', when: '2026-11-30' })
})

test('stopsWhenUserEdits', () => {
  const { result } = renderHook(() => useTypewriter({ a: 'abc', b: 'cd' }, { charDelayMs: 10 }))
  tick(10)

  act(() => {
    result.current.setValue('a', 'zz')
  })
  typeAll()

  expect(result.current.values).toEqual({ a: 'zz', b: '' })
  expect(result.current.done).toBe(true)
})

test('restartTypesNewTarget', () => {
  const { result } = renderHook(() => useTypewriter({ a: 'ab' }, { charDelayMs: 10 }))
  typeAll()

  act(() => {
    result.current.restart({ a: 'xy' })
  })
  expect(result.current.values).toEqual({ a: '' })
  typeAll()

  expect(result.current.values).toEqual({ a: 'xy' })
  expect(result.current.done).toBe(true)
})
