import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { addEntry, clearTimeline } from '@/lib/timeline'
import { SplitLayout } from './SplitLayout'

// jsdom has no matchMedia; "(min-width: 768px)" is false here, as on a phone.
function stubNarrowScreen() {
  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      matches: false,
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })),
  )
}

beforeEach(() => {
  stubNarrowScreen()
})

afterEach(() => {
  vi.unstubAllGlobals()
  act(() => clearTimeline())
})

test('rendersTabsOnNarrowScreen', () => {
  render(<SplitLayout system={<p>sistema</p>} api={<p>api</p>} />)

  expect(screen.getByRole('tablist')).toBeInTheDocument()
  expect(screen.getByRole('tab', { name: /Sistema/ })).toHaveAttribute('aria-selected', 'true')
  expect(screen.getByText('sistema')).toBeVisible()
  expect(screen.getByText('api')).not.toBeVisible()
})

test('switchesToApiTab', async () => {
  render(<SplitLayout system={<p>sistema</p>} api={<p>api</p>} />)

  await userEvent.click(screen.getByRole('tab', { name: /API/ }))

  expect(screen.getByRole('tab', { name: /API/ })).toHaveAttribute('aria-selected', 'true')
  expect(screen.getByText('api')).toBeVisible()
  expect(screen.getByText('sistema')).not.toBeVisible()
})

test('countsNewCallsWhileApiTabHidden', async () => {
  render(<SplitLayout system={<p>sistema</p>} api={<p>api</p>} />)
  const call = {
    method: 'GET' as const,
    path: '/api/tasks',
    requestHeaders: {},
    requestBody: null,
    status: 200,
    responseBody: [],
    durationMs: 10,
    startedAt: 0,
  }

  act(() => {
    addEntry(call)
    addEntry(call)
  })
  expect(screen.getByRole('tab', { name: 'API, 2 novas' })).toBeInTheDocument()

  await userEvent.click(screen.getByRole('tab', { name: /API/ }))
  await userEvent.click(screen.getByRole('tab', { name: /Sistema/ }))

  expect(screen.getByRole('tab', { name: 'API' })).toBeInTheDocument()
})
