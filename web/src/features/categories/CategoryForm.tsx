'use client'

import type { FormEvent } from 'react'
import { useTypewriter } from '@/hooks/useTypewriter'
import { ErrorMessage, Field, FormActions, inputClass, type FormErrors } from '@/features/system/ui'
import type { CategoryInput } from './api'

type CategoryFormProps = {
  // Values the typewriter fills in; `current` is where an edit starts from.
  suggested: CategoryInput
  current?: CategoryInput
  errors: FormErrors
  saving: boolean
  onSubmit: (input: CategoryInput) => void
  onCancel: () => void
}

export function CategoryForm({ suggested, current, errors, saving, onSubmit, onCancel }: CategoryFormProps) {
  const { values, setValue, done } = useTypewriter(suggested, { from: current })

  function submit(event: FormEvent) {
    event.preventDefault()
    onSubmit(values)
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-3 rounded-lg border border-slate-200 p-4">
      <ErrorMessage message={errors.message} />
      <Field id="category-name" label="Nome" error={errors.fields.name}>
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
      <Field id="category-color" label="Cor" error={errors.fields.color}>
        {(props) => (
          <div className="flex items-center gap-2">
            <span
              aria-hidden
              className="h-8 w-8 shrink-0 rounded-md border border-slate-300"
              style={{ backgroundColor: values.color }}
            />
            <input
              {...props}
              type="text"
              autoComplete="off"
              value={values.color}
              onChange={(event) => setValue('color', event.target.value)}
              className={`${inputClass} flex-1 font-mono`}
            />
          </div>
        )}
      </Field>
      <FormActions ready={done} saving={saving} onCancel={onCancel} />
    </form>
  )
}
