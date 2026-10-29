import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import Page from './page'
import { Providers } from './providers'

test('rendersDemoTitle', () => {
  render(
    <Providers>
      <Page />
    </Providers>,
  )
  expect(screen.getByRole('heading', { name: 'Task API' })).toBeInTheDocument()
})
