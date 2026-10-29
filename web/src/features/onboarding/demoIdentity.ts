const LOWER_AND_DIGITS = 'abcdefghijklmnopqrstuvwxyz0123456789'
const ALPHANUMERIC = `ABCDEFGHIJKLMNOPQRSTUVWXYZ${LOWER_AND_DIGITS}`

function randomString(length: number, alphabet: string): string {
  const bytes = crypto.getRandomValues(new Uint8Array(length))
  return Array.from(bytes, (byte) => alphabet[byte % alphabet.length]).join('')
}

// Throwaway account: unique email on a fictitious domain, generated password.
export function demoIdentity(): { name: string; email: string; password: string } {
  const number = 1000 + (crypto.getRandomValues(new Uint16Array(1))[0] % 9000)
  return {
    name: `Visitante ${number}`,
    email: `visitante-${Date.now().toString(36)}${randomString(4, LOWER_AND_DIGITS)}@demo.thiagoheckler.com.br`,
    password: randomString(12, ALPHANUMERIC),
  }
}
