'use client'

import { useEffect, useState, type FormEvent } from 'react'
import { useTypewriter } from '@/hooks/useTypewriter'
import { api, ApiError, isDailyLimit } from '@/lib/api'
import { setToken } from '@/lib/session'
import type { LoginResponse, UserResponse } from '@/lib/types'
import { demoIdentity } from './demoIdentity'
import { LockScreen } from './LockScreen'

const FIELDS = [
  { key: 'name', label: 'Nome' },
  { key: 'email', label: 'Email' },
  { key: 'password', label: 'Senha' },
] as const

type OnboardingProps = {
  onLoggedIn: () => void
  onWaitingChange: (waiting: boolean) => void
  notice?: string | null
}

export function Onboarding({ onLoggedIn, onWaitingChange, notice }: OnboardingProps) {
  const [identity] = useState(demoIdentity)
  const { values, setValue, done } = useTypewriter(identity)
  const [submitting, setSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [warning, setWarning] = useState<string | null>(null)
  const [lockMessage, setLockMessage] = useState<string | null>(null)
  // Set once POST /users succeeds: if the login then fails (429 per IP,
  // network), trying again must not create the same account (409).
  const [accountCreated, setAccountCreated] = useState(false)

  const waiting = !submitting && lockMessage === null
  useEffect(() => {
    onWaitingChange(waiting)
  }, [waiting, onWaitingChange])
  useEffect(() => () => onWaitingChange(false), [onWaitingChange])

  async function createAccount(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setFieldErrors({})
    setWarning(null)
    try {
      if (!accountCreated) {
        await api<UserResponse>('POST', '/users', values)
        setAccountCreated(true)
      }
      const login = await api<LoginResponse>('POST', '/auth/login', {
        email: values.email,
        password: values.password,
      })
      // Setting the token swaps this screen for the system (DemoApp).
      setToken(login.token)
      onLoggedIn()
    } catch (error) {
      if (isDailyLimit(error)) {
        setLockMessage((error as ApiError).message)
      } else if (error instanceof ApiError && Object.keys(error.fields).length > 0) {
        setFieldErrors(error.fields)
      } else if (error instanceof ApiError) {
        // Includes the per-IP 429, which only asks the visitor to wait.
        setWarning(error.message)
      } else {
        setWarning('Não foi possível falar com a API. Tente novamente.')
      }
      setSubmitting(false)
    }
  }

  if (lockMessage !== null) return <LockScreen message={lockMessage} />

  return (
    <form onSubmit={createAccount} className="mx-auto flex max-w-md flex-col gap-4 p-6">
      <h2 className="text-xl font-semibold text-slate-900">Experimente o sistema</h2>
      {notice && (
        <p role="status" className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {notice}
        </p>
      )}
      <p className="text-sm text-slate-600">Conta temporária de demonstração, apagada em 24 h</p>
      {FIELDS.map(({ key, label }) => {
        const error = fieldErrors[key]
        return (
          <div key={key} className="flex flex-col gap-1">
            <label htmlFor={`onboarding-${key}`} className="text-sm font-medium text-slate-700">
              {label}
            </label>
            <input
              id={`onboarding-${key}`}
              type="text"
              autoComplete="off"
              value={values[key]}
              onChange={(event) => setValue(key, event.target.value)}
              readOnly={accountCreated}
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? `onboarding-${key}-error` : undefined}
              className="rounded-md border border-slate-300 px-3 py-2 text-slate-900 focus:border-indigo-600 focus:outline-none"
            />
            {error && (
              <p id={`onboarding-${key}-error`} className="text-sm text-red-600">
                {error}
              </p>
            )}
          </div>
        )
      })}
      {warning && (
        <p role="alert" className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {warning}
        </p>
      )}
      <button
        type="submit"
        disabled={!done || submitting}
        aria-busy={submitting}
        className={`rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50 ${
          done && !submitting ? 'animate-pulse' : ''
        }`}
      >
        Criar conta e entrar
      </button>
    </form>
  )
}
