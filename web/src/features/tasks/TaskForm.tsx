'use client'

import type { FormEvent } from 'react'
import { useTypewriter } from '@/hooks/useTypewriter'
import { ErrorMessage, Field, FormActions, inputClass, type FormErrors } from '@/features/system/ui'
import type { CategoryResponse, TagResponse, TaskPriority, TaskResponse } from '@/lib/types'
import { PRIORITY_LABELS } from './labels'

// Every field as the string the inputs hold, in the order the typewriter
// fills them. tagIds is a comma-separated list of ids.
export type TaskFormValues = {
  title: string
  description: string
  priority: string
  dueDate: string
  categoryId: string
  tagIds: string
  parentTaskId: string
}

const INSTANT_FIELDS: (keyof TaskFormValues)[] = ['priority', 'dueDate', 'categoryId', 'tagIds']

type TaskFormProps = {
  mode: 'create' | 'edit'
  suggested: TaskFormValues
  current?: TaskFormValues
  categories: CategoryResponse[]
  tags: TagResponse[]
  // Candidates for "Subtarefa de". Subtasks are listed too, so picking one
  // shows the API's real 422.
  tasks: TaskResponse[]
  errors: FormErrors
  saving: boolean
  onSubmit: (values: TaskFormValues) => void
  onCancel: () => void
}

export function TaskForm({
  mode,
  suggested,
  current,
  categories,
  tags,
  tasks,
  errors,
  saving,
  onSubmit,
  onCancel,
}: TaskFormProps) {
  const { values, setValue, done } = useTypewriter(suggested, { from: current, instant: INSTANT_FIELDS })
  const selectedTags = values.tagIds ? values.tagIds.split(',') : []

  function toggleTag(id: string) {
    const next = selectedTags.includes(id)
      ? selectedTags.filter((tagId) => tagId !== id)
      : [...selectedTags, id]
    setValue('tagIds', next.join(','))
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    onSubmit(values)
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-3 rounded-lg border border-slate-200 p-4">
      <ErrorMessage message={errors.message} />
      <Field id="task-title" label="Título" error={errors.fields.title}>
        {(props) => (
          <input
            {...props}
            type="text"
            autoComplete="off"
            value={values.title}
            onChange={(event) => setValue('title', event.target.value)}
            className={inputClass}
          />
        )}
      </Field>
      <Field id="task-description" label="Descrição" error={errors.fields.description}>
        {(props) => (
          <textarea
            {...props}
            rows={2}
            value={values.description}
            onChange={(event) => setValue('description', event.target.value)}
            className={inputClass}
          />
        )}
      </Field>
      <div className="grid gap-3 sm:grid-cols-2">
        <Field id="task-priority" label="Prioridade" error={errors.fields.priority}>
          {(props) => (
            <select
              {...props}
              value={values.priority}
              onChange={(event) => setValue('priority', event.target.value)}
              className={inputClass}
            >
              <option value="">—</option>
              {(Object.keys(PRIORITY_LABELS) as TaskPriority[]).map((priority) => (
                <option key={priority} value={priority}>
                  {PRIORITY_LABELS[priority]}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field id="task-due-date" label="Prazo" error={errors.fields.dueDate}>
          {(props) => (
            <input
              {...props}
              type="datetime-local"
              value={values.dueDate}
              onChange={(event) => setValue('dueDate', event.target.value)}
              className={inputClass}
            />
          )}
        </Field>
      </div>
      <Field id="task-category" label="Categoria" error={errors.fields.categoryId}>
        {(props) => (
          <select
            {...props}
            value={values.categoryId}
            onChange={(event) => setValue('categoryId', event.target.value)}
            className={inputClass}
          >
            <option value="">Sem categoria</option>
            {categories.map((category) => (
              <option key={category.id} value={String(category.id)}>
                {category.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      {mode === 'create' && (
        <>
          {tags.length > 0 && (
            <fieldset className="flex flex-col gap-1">
              <legend className="text-sm font-medium text-slate-700">Tags</legend>
              <div className="flex flex-wrap gap-3">
                {tags.map((tag) => (
                  <label key={tag.id} className="flex items-center gap-1 text-sm text-slate-800">
                    <input
                      type="checkbox"
                      checked={selectedTags.includes(String(tag.id))}
                      onChange={() => toggleTag(String(tag.id))}
                    />
                    {tag.name}
                  </label>
                ))}
              </div>
            </fieldset>
          )}
          <Field id="task-parent" label="Subtarefa de" error={errors.fields.parentTaskId}>
            {(props) => (
              <select
                {...props}
                value={values.parentTaskId}
                onChange={(event) => setValue('parentTaskId', event.target.value)}
                className={inputClass}
              >
                <option value="">Nenhuma (tarefa principal)</option>
                {tasks.map((task) => (
                  <option key={task.id} value={String(task.id)}>
                    {task.parentTaskId === null ? task.title : `↳ ${task.title}`}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </>
      )}
      <FormActions ready={done} saving={saving} onCancel={onCancel} />
    </form>
  )
}
