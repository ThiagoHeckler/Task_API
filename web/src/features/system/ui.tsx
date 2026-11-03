'use client'

import { useState, type ReactNode } from 'react'
import { ApiError } from '@/lib/api'

// Pieces shared by the system sections (categories, tags, tasks).

export type FormErrors = { fields: Record<string, string>; message: string | null }

export const NO_ERRORS: FormErrors = { fields: {}, message: null }

// 400 goes next to each field; anything else (409, 404, 422, network) above the form.
export function toFormErrors(error: unknown): FormErrors {
  if (error instanceof ApiError && Object.keys(error.fields).length > 0) {
    return { fields: error.fields, message: null }
  }
  if (error instanceof ApiError) return { fields: {}, message: error.message }
  return { fields: {}, message: 'Não foi possível falar com a API. Tente novamente.' }
}

type FieldProps = {
  id: string
  label: string
  error?: string
  children: (props: {
    id: string
    'aria-invalid'?: true
    'aria-describedby'?: string
  }) => ReactNode
}

export function Field({ id, label, error, children }: FieldProps) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      {children({
        id,
        'aria-invalid': error ? true : undefined,
        'aria-describedby': error ? `${id}-error` : undefined,
      })}
      {error && (
        <p id={`${id}-error`} className="text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}

export const inputClass =
  'rounded-md border border-slate-300 px-3 py-2 text-slate-900 focus:border-indigo-600 focus:outline-none'

export function ErrorMessage({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
      {message}
    </p>
  )
}

type FormActionsProps = {
  ready: boolean
  saving: boolean
  onCancel: () => void
}

// "Salvar" waits for the typewriter (or the visitor's edit), then pulses.
export function FormActions({ ready, saving, onCancel }: FormActionsProps) {
  return (
    <div className="flex gap-2">
      <button
        type="submit"
        disabled={!ready || saving}
        aria-busy={saving}
        className={`rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50 ${
          ready && !saving ? 'animate-pulse' : ''
        }`}
      >
        Salvar
      </button>
      <button
        type="button"
        onClick={onCancel}
        className="rounded-md px-4 py-2 text-slate-600 hover:bg-slate-100"
      >
        Cancelar
      </button>
    </div>
  )
}

type ItemActionsProps = {
  label: string
  // Shown next to the confirmation (e.g. what else gets deleted).
  warning?: string
  onEdit: () => void
  onDelete: () => void
  deleting: boolean
}

// Edit plus a delete that asks for confirmation inline before calling the API.
export function ItemActions({ label, warning, onEdit, onDelete, deleting }: ItemActionsProps) {
  const [confirming, setConfirming] = useState(false)

  if (confirming) {
    return (
      <div className="flex flex-wrap items-center gap-2 text-sm">
        {warning && <span className="text-red-700">{warning}</span>}
        <button
          type="button"
          onClick={onDelete}
          disabled={deleting}
          className="rounded-md bg-red-600 px-3 py-1 font-medium text-white hover:bg-red-700 disabled:opacity-50"
        >
          Confirmar exclusão
        </button>
        <button
          type="button"
          onClick={() => setConfirming(false)}
          className="px-2 py-1 text-slate-600 hover:text-slate-900"
        >
          Cancelar
        </button>
      </div>
    )
  }

  return (
    <div className="flex items-center gap-2 text-sm">
      <button
        type="button"
        aria-label={`Editar ${label}`}
        onClick={onEdit}
        className="px-2 py-1 text-indigo-600 hover:text-indigo-800"
      >
        Editar
      </button>
      <button
        type="button"
        aria-label={`Excluir ${label}`}
        onClick={() => setConfirming(true)}
        className="px-2 py-1 text-red-600 hover:text-red-800"
      >
        Excluir
      </button>
    </div>
  )
}

type SectionHeaderProps = {
  title: string
  actionLabel: string
  onAction: () => void
  actionDisabled: boolean
}

export function SectionHeader({ title, actionLabel, onAction, actionDisabled }: SectionHeaderProps) {
  return (
    <div className="flex items-center justify-between gap-3">
      <h2 className="text-xl font-semibold text-slate-900">{title}</h2>
      <button
        type="button"
        onClick={onAction}
        disabled={actionDisabled}
        className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        {actionLabel}
      </button>
    </div>
  )
}
