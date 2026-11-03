import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Sem globals do Vitest, o Testing Library não desmonta sozinho entre os testes.
afterEach(() => {
  cleanup()
})

// jsdom has no matchMedia. Default to a narrow screen; tests can stub their own.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    addEventListener: () => {},
    removeEventListener: () => {},
  }),
})
