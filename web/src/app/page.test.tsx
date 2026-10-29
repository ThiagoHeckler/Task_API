import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import Page from './page'

test('rendersDemoTitle', () => {
  render(<Page />)
  expect(screen.getByRole('heading', { name: 'Task API' })).toBeInTheDocument()
})
