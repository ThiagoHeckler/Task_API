import { api } from '@/lib/api'
import type { CategoryResponse } from '@/lib/types'

export const CATEGORIES_KEY = ['categories'] as const

export type CategoryInput = { name: string; color: string }

export function listCategories() {
  return api<CategoryResponse[]>('GET', '/categories')
}

export function createCategory(input: CategoryInput) {
  return api<CategoryResponse>('POST', '/categories', input)
}

export function updateCategory(id: number, input: CategoryInput) {
  return api<CategoryResponse>('PUT', `/categories/${id}`, input)
}

export function deleteCategory(id: number) {
  return api<void>('DELETE', `/categories/${id}`)
}
