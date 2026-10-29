'use client'

import type { FormEvent } from 'react'
import { useTypewriter } from '@/hooks/useTypewriter'
import { ErrorMessage, Field, FormActions, inputClass, type FormErrors } from '@/features/system/ui'
import type { TagInput } from './api'

type TagFormProps = {
  // Value the typewriter fills in; `current` is where an edit starts from.
  suggested: TagInput
  current?: TagInput
  errors: FormErrors
  saving: boolean
  onSubmit: (input: TagInput) => void
  onCancel: () => void
}

export function TagForm({ suggested, current, errors, saving, onSubmit, onCancel }: TagFormProps) {
  const { values, setValue, done } = useTypewriter(suggested, { from: current })

  function submit(event: FormEvent) {
    event.preventDefault()
    onSubmit(values)
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-3 rounded-lg border border-slate-200 p-4">
      <ErrorMessage message={errors.message} />
      <Field id="tag-name" label="Nome" error={errors.fields.name}>
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
