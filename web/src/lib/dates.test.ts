import { expect, test } from 'vitest'
import { formatApiDate, toApiDateTime, toInputDateTime } from './dates'

test('formatsUtcAsSaoPaulo', () => {
  expect(formatApiDate('2026-10-29T02:10:25')).toBe('28/10/2026 23:10')
})

test('dashForNull', () => {
  expect(formatApiDate(null)).toBe('—')
})

test('convertsLocalInputToUtc', () => {
  expect(toApiDateTime('2026-11-30T18:00')).toBe('2026-11-30T21:00:00')
})

test('convertsApiDateToLocalInput', () => {
  expect(toInputDateTime('2026-11-30T21:00:00')).toBe('2026-11-30T18:00')
  expect(toInputDateTime(null)).toBe('')
})

test('passesInvalidDateThroughUnchanged', () => {
  // A 6-digit year is out of Date's range; the API answers with its own 400.
  expect(() => toApiDateTime('275760-09-14T00:00')).not.toThrow()
  expect(toApiDateTime('275760-09-14T00:00')).toBe('275760-09-14T00:00')
})
