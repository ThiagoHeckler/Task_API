'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { nextColor, nextPreset, CATEGORY_PRESETS } from '@/features/presets'
import { ErrorMessage, ItemActions, NO_ERRORS, SectionHeader, toFormErrors } from '@/features/system/ui'
import { TASKS_KEY } from '@/features/tasks/api'
import type { CategoryResponse } from '@/lib/types'
import {
  CATEGORIES_KEY,
  createCategory,
  deleteCategory,
  listCategories,
  updateCategory,
  type CategoryInput,
} from './api'
import { CategoryForm } from './CategoryForm'

type FormState = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; category: CategoryResponse }

type CategoriesPageProps = {
  onWaitingChange: (waiting: boolean) => void
}

export function CategoriesPage({ onWaitingChange }: CategoriesPageProps) {
  const queryClient = useQueryClient()
  const categories = useQuery({ queryKey: CATEGORIES_KEY, queryFn: listCategories })
  const [form, setForm] = useState<FormState>({ kind: 'closed' })
  // Bumped on every open so the form remounts and the typewriter starts over.
  const [formKey, setFormKey] = useState(0)

  // Every mutation is followed by a real GET of the list (visible in the timeline).
  // Tasks embed categories and tags, so their cached lists are marked stale too:
  // the GET goes out when the visitor opens Tarefas again.
  const refreshList = () => {
    queryClient.invalidateQueries({ queryKey: TASKS_KEY })
    return queryClient.invalidateQueries({ queryKey: CATEGORIES_KEY })
  }

  const save = useMutation({
    mutationFn: (input: CategoryInput) =>
      form.kind === 'edit' ? updateCategory(form.category.id, input) : createCategory(input),
    onSuccess: () => {
      setForm({ kind: 'closed' })
      return refreshList()
    },
  })

  const remove = useMutation({
    mutationFn: (id: number) => deleteCategory(id),
    onSuccess: refreshList,
  })

  const waiting = form.kind !== 'closed' && !save.isPending
  useEffect(() => {
    onWaitingChange(waiting)
  }, [waiting, onWaitingChange])
  useEffect(() => () => onWaitingChange(false), [onWaitingChange])

  function open(next: FormState) {
    save.reset()
    setForm(next)
    setFormKey((key) => key + 1)
  }

  const list = categories.data ?? []

  return (
    <div className="flex flex-col gap-4">
      <SectionHeader
        title="Categorias"
        actionLabel="Nova categoria"
        onAction={() => open({ kind: 'create' })}
        actionDisabled={categories.isPending}
      />

      {form.kind !== 'closed' && (
        <CategoryForm
          key={formKey}
          suggested={
            form.kind === 'edit'
              ? { name: form.category.name, color: nextColor(form.category.color) }
              : nextPreset(
                  CATEGORY_PRESETS,
                  list.map((category) => category.name),
                )
          }
          current={
            form.kind === 'edit' ? { name: form.category.name, color: form.category.color } : undefined
          }
          errors={save.isError ? toFormErrors(save.error) : NO_ERRORS}
          saving={save.isPending}
          onSubmit={(input) => save.mutate(input)}
          onCancel={() => setForm({ kind: 'closed' })}
        />
      )}

      {categories.isPending && <p className="text-slate-500">Carregando…</p>}
      {categories.isError && <ErrorMessage message={toFormErrors(categories.error).message} />}
      {remove.isError && <ErrorMessage message={toFormErrors(remove.error).message} />}
      {categories.isSuccess && list.length === 0 && (
        <p className="text-slate-500">Nenhuma categoria ainda</p>
      )}

      <ul className="flex flex-col divide-y divide-slate-200">
        {list.map((category) => (
          <li key={category.id} className="flex items-center justify-between gap-3 py-2">
            <span className="flex items-center gap-2">
              <span
                aria-hidden
                className="h-3 w-3 rounded-full"
                style={{ backgroundColor: category.color }}
              />
              <span className="text-slate-900">{category.name}</span>
            </span>
            <ItemActions
              label={category.name}
              onEdit={() => open({ kind: 'edit', category })}
              onDelete={() => remove.mutate(category.id)}
              deleting={remove.isPending}
            />
          </li>
        ))}
      </ul>
    </div>
  )
}
