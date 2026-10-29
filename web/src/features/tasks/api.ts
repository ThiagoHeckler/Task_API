import { api } from '@/lib/api'
import type { TaskPriority, TaskResponse, TaskStatus } from '@/lib/types'

export type TaskFilter = 'all' | 'root' | TaskStatus

// Prefix for invalidation: refreshes the list under every filter.
export const TASKS_KEY = ['tasks'] as const

export function tasksKey(filter: TaskFilter) {
  return [...TASKS_KEY, filter] as const
}

export type CreateTaskInput = {
  title: string
  description?: string
  priority: TaskPriority
  dueDate?: string
  categoryId?: number
  tagIds?: number[]
  parentTaskId?: number
}

// The API treats a missing field as "do not change".
export type UpdateTaskInput = Partial<{
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueDate: string
  categoryId: number
}>

function pathFor(filter: TaskFilter): string {
  if (filter === 'all') return '/tasks'
  if (filter === 'root') return '/tasks/root'
  return `/tasks?status=${filter}`
}

export function listTasks(filter: TaskFilter) {
  return api<TaskResponse[]>('GET', pathFor(filter))
}

export function createTask(input: CreateTaskInput) {
  return api<TaskResponse>('POST', '/tasks', input)
}

export function updateTask(id: number, input: UpdateTaskInput) {
  return api<TaskResponse>('PUT', `/tasks/${id}`, input)
}

export function deleteTask(id: number) {
  return api<void>('DELETE', `/tasks/${id}`)
}
