import { api } from '@/lib/api'
import type { TagResponse } from '@/lib/types'

export const TAGS_KEY = ['tags'] as const

export type TagInput = { name: string }

export function listTags() {
  return api<TagResponse[]>('GET', '/tags')
}

export function createTag(input: TagInput) {
  return api<TagResponse>('POST', '/tags', input)
}

export function updateTag(id: number, input: TagInput) {
  return api<TagResponse>('PUT', `/tags/${id}`, input)
}

export function deleteTag(id: number) {
  return api<void>('DELETE', `/tags/${id}`)
}
