'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { CATEGORIES_KEY, listCategories } from '@/features/categories/api'
import { defaultDueDate, nextPriority, TASK_PRESETS } from '@/features/presets'
import { ErrorMessage, ItemActions, NO_ERRORS, SectionHeader, toFormErrors } from '@/features/system/ui'
import { listTags, TAGS_KEY } from '@/features/tags/api'
import { formatApiDate, toApiDateTime, toInputDateTime } from '@/lib/dates'
import type { TaskPriority, TaskResponse, TaskStatus } from '@/lib/types'
import {
  createTask,
  deleteTask,
  listTasks,
  TASKS_KEY,
  tasksKey,
  updateTask,
  type CreateTaskInput,
  type TaskFilter,
  type UpdateTaskInput,
} from './api'
import { TaskFilters } from './TaskFilters'
import { TaskForm, type TaskFormValues } from './TaskForm'
import { PRIORITY_LABELS, STATUS_LABELS } from './labels'

type FormState = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; task: TaskResponse }

function toFormValues(task: TaskResponse): TaskFormValues {
  return {
    title: task.title,
    description: task.description ?? '',
    priority: task.priority,
    dueDate: toInputDateTime(task.dueDate),
    categoryId: task.category ? String(task.category.id) : '',
    tagIds: task.tags.map((tag) => tag.id).join(','),
    parentTaskId: task.parentTaskId === null ? '' : String(task.parentTaskId),
  }
}

// Empty fields are left out, so the API applies its own defaults (an empty
// priority would not even parse: the API answers 400 "Malformed JSON").
function toCreateInput(values: TaskFormValues): CreateTaskInput {
  const input: CreateTaskInput = { title: values.title }
  if (values.priority) input.priority = values.priority as TaskPriority
  if (values.description) input.description = values.description
  if (values.dueDate) input.dueDate = toApiDateTime(values.dueDate)
  if (values.categoryId) input.categoryId = Number(values.categoryId)
  if (values.tagIds) input.tagIds = values.tagIds.split(',').map(Number)
  if (values.parentTaskId) input.parentTaskId = Number(values.parentTaskId)
  return input
}

// Partial PUT: only what the edit changed goes in the body.
function toUpdateInput(values: TaskFormValues, original: TaskFormValues): UpdateTaskInput {
  const input: UpdateTaskInput = {}
  if (values.title !== original.title) input.title = values.title
  if (values.description !== original.description) input.description = values.description
  if (values.priority && values.priority !== original.priority) {
    input.priority = values.priority as TaskPriority
  }
  if (values.dueDate && values.dueDate !== original.dueDate) {
    input.dueDate = toApiDateTime(values.dueDate)
  }
  if (values.categoryId && values.categoryId !== original.categoryId) {
    input.categoryId = Number(values.categoryId)
  }
  return input
}

// Subtasks go under their parent when it is in the same list; otherwise
// (e.g. a status filter that skips the parent) they show at the top level.
function groupByParent(tasks: TaskResponse[]) {
  const ids = new Set(tasks.map((task) => task.id))
  const children = new Map<number, TaskResponse[]>()
  const roots: TaskResponse[] = []
  for (const task of tasks) {
    if (task.parentTaskId !== null && ids.has(task.parentTaskId)) {
      children.set(task.parentTaskId, [...(children.get(task.parentTaskId) ?? []), task])
    } else {
      roots.push(task)
    }
  }
  return { roots, children }
}

type TasksPageProps = {
  onWaitingChange: (waiting: boolean) => void
}

export function TasksPage({ onWaitingChange }: TasksPageProps) {
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState<TaskFilter>('all')
  const tasks = useQuery({ queryKey: tasksKey(filter), queryFn: () => listTasks(filter) })
  const [form, setForm] = useState<FormState>({ kind: 'closed' })
  // Bumped on every open so the form remounts and the typewriter starts over.
  const [formKey, setFormKey] = useState(0)

  // The selects need categories and tags: loaded when a form opens (cached
  // if the visitor already opened those sections).
  const formOpen = form.kind !== 'closed'
  const categories = useQuery({ queryKey: CATEGORIES_KEY, queryFn: listCategories, enabled: formOpen })
  const tags = useQuery({ queryKey: TAGS_KEY, queryFn: listTags, enabled: formOpen })

  // Every mutation is followed by a real GET of the list (visible in the timeline).
  const refreshList = () => queryClient.invalidateQueries({ queryKey: TASKS_KEY })

  const save = useMutation({
    mutationFn: (values: TaskFormValues) =>
      form.kind === 'edit'
        ? updateTask(form.task.id, toUpdateInput(values, toFormValues(form.task)))
        : createTask(toCreateInput(values)),
    onSuccess: () => {
      setForm({ kind: 'closed' })
      return refreshList()
    },
  })

  const changeStatus = useMutation({
    mutationFn: ({ id, status }: { id: number; status: TaskStatus }) => updateTask(id, { status }),
    onSuccess: refreshList,
  })

  const remove = useMutation({
    mutationFn: (id: number) => deleteTask(id),
    onSuccess: refreshList,
  })

  const waiting = formOpen && !save.isPending
  useEffect(() => {
    onWaitingChange(waiting)
  }, [waiting, onWaitingChange])
  useEffect(() => () => onWaitingChange(false), [onWaitingChange])

  function open(next: FormState) {
    save.reset()
    setForm(next)
    setFormKey((key) => key + 1)
  }

  const list = tasks.data ?? []
  const { roots, children } = groupByParent(list)
  const formReady = categories.isSuccess && tags.isSuccess

  function suggestedForCreate(): TaskFormValues {
    const preset = TASK_PRESETS[list.length % TASK_PRESETS.length]
    const firstTag = tags.data?.[0]
    return {
      title: preset.title,
      description: preset.description,
      priority: preset.priority,
      dueDate: defaultDueDate(),
      categoryId: categories.data?.[0] ? String(categories.data[0].id) : '',
      tagIds: firstTag ? String(firstTag.id) : '',
      parentTaskId: '',
    }
  }

  function renderTask(task: TaskResponse) {
    const subtasks = children.get(task.id) ?? []
    const completed = task.status === 'COMPLETED'
    // Pendente: Iniciar or Concluir; Em andamento: Concluir; Concluída: Reabrir.
    const statusActions: { label: string; next: TaskStatus }[] = completed
      ? [{ label: 'Reabrir', next: 'PENDING' }]
      : [
          ...(task.status === 'PENDING' ? [{ label: 'Iniciar', next: 'IN_PROGRESS' as const }] : []),
          { label: 'Concluir', next: 'COMPLETED' },
        ]
    return (
      <li key={task.id} className="py-2">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div className="flex min-w-0 flex-col gap-1">
            <span className={`font-medium ${completed ? 'text-slate-500 line-through' : 'text-slate-900'}`}>
              {task.title}
            </span>
            <span className="flex flex-wrap items-center gap-2 text-xs text-slate-600">
              <span className="rounded bg-slate-100 px-1.5 py-0.5">{STATUS_LABELS[task.status]}</span>
              <span className="rounded bg-slate-100 px-1.5 py-0.5">
                Prioridade {PRIORITY_LABELS[task.priority].toLowerCase()}
              </span>
              {task.dueDate && <span>Prazo {formatApiDate(task.dueDate)}</span>}
              {task.completedAt && <span>Concluída em {formatApiDate(task.completedAt)}</span>}
              {task.category && (
                <span className="flex items-center gap-1">
                  <span
                    aria-hidden
                    className="h-2 w-2 rounded-full"
                    style={{ backgroundColor: task.category.color }}
                  />
                  {task.category.name}
                </span>
              )}
              {task.tags.map((tag) => (
                <span key={tag.id} className="rounded-full bg-indigo-50 px-2 text-indigo-700">
                  {tag.name}
                </span>
              ))}
            </span>
          </div>
          <div className="flex flex-wrap items-center gap-1 text-sm">
            {statusActions.map((action) => (
              <button
                key={action.next}
                type="button"
                aria-label={`${action.label} ${task.title}`}
                disabled={changeStatus.isPending}
                onClick={() => changeStatus.mutate({ id: task.id, status: action.next })}
                className="px-2 py-1 text-green-700 hover:text-green-900 disabled:opacity-50"
              >
                {action.label}
              </button>
            ))}
            <ItemActions
              label={task.title}
              warning="As subtarefas são excluídas junto"
              onEdit={() => open({ kind: 'edit', task })}
              onDelete={() => remove.mutate(task.id)}
              deleting={remove.isPending}
            />
          </div>
        </div>
        {subtasks.length > 0 && (
          <ul className="mt-2 ml-5 border-l border-slate-200 pl-3">{subtasks.map(renderTask)}</ul>
        )}
      </li>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <SectionHeader
        title="Tarefas"
        actionLabel="Nova tarefa"
        onAction={() => open({ kind: 'create' })}
        actionDisabled={tasks.isPending}
      />

      {formOpen && !formReady && !categories.isError && !tags.isError && (
        <p className="text-slate-500">Carregando…</p>
      )}
      {formOpen && (categories.isError || tags.isError) && (
        <ErrorMessage message={toFormErrors(categories.error ?? tags.error).message} />
      )}
      {form.kind !== 'closed' && formReady && (
        <TaskForm
          key={formKey}
          mode={form.kind}
          suggested={
            form.kind === 'edit'
              ? {
                  ...toFormValues(form.task),
                  priority: nextPriority(form.task.priority),
                }
              : suggestedForCreate()
          }
          current={form.kind === 'edit' ? toFormValues(form.task) : undefined}
          categories={categories.data}
          tags={tags.data}
          tasks={list}
          errors={save.isError ? toFormErrors(save.error) : NO_ERRORS}
          saving={save.isPending}
          onSubmit={(values) => save.mutate(values)}
          onCancel={() => setForm({ kind: 'closed' })}
        />
      )}

      <TaskFilters value={filter} onChange={setFilter} />

      {tasks.isPending && <p className="text-slate-500">Carregando…</p>}
      {tasks.isError && <ErrorMessage message={toFormErrors(tasks.error).message} />}
      {changeStatus.isError && <ErrorMessage message={toFormErrors(changeStatus.error).message} />}
      {remove.isError && <ErrorMessage message={toFormErrors(remove.error).message} />}
      {tasks.isSuccess && list.length === 0 && (
        <p className="text-slate-500">
          {filter === 'all' ? 'Nenhuma tarefa ainda' : 'Nenhuma tarefa neste filtro'}
        </p>
      )}

      <ul className="flex flex-col divide-y divide-slate-200">{roots.map(renderTask)}</ul>
    </div>
  )
}
