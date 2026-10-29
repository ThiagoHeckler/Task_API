import { useSyncExternalStore } from 'react'

export type TimelineEntry = {
  id: number
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  path: string
  requestHeaders: Record<string, string>
  requestBody: unknown
  // null while the request is in flight; 0 on network failure.
  status: number | null
  responseBody: unknown
  durationMs: number | null
  startedAt: number
}

// Module-level store: every write replaces the array, so useSyncExternalStore
// sees a new snapshot and re-renders only when something changed.
let entries: TimelineEntry[] = []
let nextId = 1
const listeners = new Set<() => void>()

function emit() {
  listeners.forEach((listener) => listener())
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function getTimeline(): TimelineEntry[] {
  return entries
}

export function addEntry(entry: Omit<TimelineEntry, 'id'>): number {
  const id = nextId++
  entries = [{ ...entry, id }, ...entries]
  emit()
  return id
}

export function updateEntry(id: number, patch: Partial<TimelineEntry>): void {
  entries = entries.map((entry) => (entry.id === id ? { ...entry, ...patch, id } : entry))
  emit()
}

export function clearTimeline(): void {
  entries = []
  emit()
}

export function useTimeline(): TimelineEntry[] {
  return useSyncExternalStore(subscribe, getTimeline, getTimeline)
}
