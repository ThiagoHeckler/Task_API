'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { useTypewriter } from '@/hooks/useTypewriter'
import { editedUserName } from '@/features/presets'
import { ErrorMessage, Field, FormActions, inputClass, NO_ERRORS, toFormErrors, type FormErrors } from '@/features/system/ui'
import { api } from '@/lib/api'
import { formatApiDate } from '@/lib/dates'
import { clearToken } from '@/lib/session'
import type { UserResponse } from '@/lib/types'

const ME_KEY = ['me'] as const

type NameFormProps = {
  current: string
  errors: FormErrors
  saving: boolean
  onSubmit: (name: string) => void
  onCancel: () => void
}

function NameForm({ current, errors, saving, onSubmit, onCancel }: NameFormProps) {
  const { values, setValue, done } = useTypewriter(
    { name: editedUserName(current) },
    { from: { name: current } },
  )

  function submit(event: FormEvent) {
    event.preventDefault()
    onSubmit(values.name)
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-3 rounded-lg border border-slate-200 p-4">
      <ErrorMessage message={errors.message} />
      <Field id="account-name" label="Nome" error={errors.fields.name}>
        {(props) => (
          <input
            {...props}
            type="text"
            autoComplete="off"
            value={values.name}
            onChange={(event) => setValue('name', event.target.value)}
            className={inputClass}
          />
        )}
      </Field>
      <FormActions ready={done} saving={saving} onCancel={onCancel} />
    </form>
  )
}

type AccountPageProps = {
  onWaitingChange: (waiting: boolean) => void
}

export function AccountPage({ onWaitingChange }: AccountPageProps) {
  const queryClient = useQueryClient()
  const me = useQuery({ queryKey: ME_KEY, queryFn: () => api<UserResponse>('GET', '/users/me') })
  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  const rename = useMutation({
    mutationFn: (name: string) => api<UserResponse>('PUT', '/users/me', { name }),
    onSuccess: () => {
      setEditing(false)
      return queryClient.invalidateQueries({ queryKey: ME_KEY })
    },
  })

  // Clearing the token swaps the system for the signup screen (DemoApp);
  // the cache goes too, so nothing of the deleted account is left in memory.
  const removeAccount = useMutation({
    mutationFn: () => api<void>('DELETE', '/users/me'),
    onSuccess: () => {
      clearToken()
      queryClient.clear()
    },
  })

  const waiting = editing && !rename.isPending
  useEffect(() => {
    onWaitingChange(waiting)
  }, [waiting, onWaitingChange])
  useEffect(() => () => onWaitingChange(false), [onWaitingChange])

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-semibold text-slate-900">Minha conta</h2>

      {me.isPending && <p className="text-slate-500">Carregando…</p>}
      {me.isError && <ErrorMessage message={toFormErrors(me.error).message} />}

      {me.isSuccess && (
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
          <dt className="text-slate-500">Nome</dt>
          <dd className="text-slate-900">{me.data.name}</dd>
          <dt className="text-slate-500">Email</dt>
          <dd className="break-all text-slate-900">{me.data.email}</dd>
          <dt className="text-slate-500">Criada em</dt>
          <dd className="text-slate-900">{formatApiDate(me.data.createdAt)}</dd>
        </dl>
      )}

      {me.isSuccess && editing && (
        <NameForm
          current={me.data.name}
          errors={rename.isError ? toFormErrors(rename.error) : NO_ERRORS}
          saving={rename.isPending}
          onSubmit={(name) => rename.mutate(name)}
          onCancel={() => setEditing(false)}
        />
      )}

      {me.isSuccess && !editing && (
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => {
              rename.reset()
              setEditing(true)
            }}
            className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-700"
          >
            Editar nome
          </button>
          {!confirmingDelete && (
            <button
              type="button"
              onClick={() => setConfirmingDelete(true)}
              className="rounded-md px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50"
            >
              Excluir conta
            </button>
          )}
        </div>
      )}

      {confirmingDelete && (
        <div className="flex flex-col gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm">
          <p className="text-red-800">A conta e todas as tarefas, categorias e tags são apagadas agora.</p>
          <ErrorMessage message={removeAccount.isError ? toFormErrors(removeAccount.error).message : null} />
          <div className="flex gap-2">
            <button
              type="button"
              disabled={removeAccount.isPending}
              onClick={() => removeAccount.mutate()}
              className="rounded-md bg-red-600 px-3 py-1.5 font-medium text-white hover:bg-red-700 disabled:opacity-50"
            >
              Confirmar exclusão da conta
            </button>
            <button
              type="button"
              onClick={() => setConfirmingDelete(false)}
              className="rounded-md px-3 py-1.5 text-slate-600 hover:bg-white"
            >
              Cancelar
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
