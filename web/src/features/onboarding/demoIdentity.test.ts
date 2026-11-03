import { expect, test } from 'vitest'
import { demoIdentity } from './demoIdentity'

test('generatesThrowawayIdentity', () => {
  const identity = demoIdentity()

  expect(identity.name).toMatch(/^Visitante \d{4}$/)
  expect(identity.email).toMatch(/^visitante-[a-z0-9]+@demo\.thiagoheckler\.com\.br$/)
  expect(identity.password).toMatch(/^[A-Za-z0-9]{12}$/)
  expect(demoIdentity().email).not.toBe(identity.email)
})
