import { expect, test } from 'vitest'
import { formatApiDate, toApiDateTime } from './dates'

test('formatsUtcAsSaoPaulo', () => {
  expect(formatApiDate('2026-10-29T02:10:25')).toBe('28/10/2026 23:10')
})

test('dashForNull', () => {
  expect(formatApiDate(null)).toBe('—')
})

test('convertsLocalInputToUtc', () => {
  expect(toApiDateTime('2026-11-30T18:00')).toBe('2026-11-30T21:00:00')
})
