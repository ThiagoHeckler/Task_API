import type { TimelineEntry } from '@/lib/timeline'

export function isLoginSuccess(entry: TimelineEntry): boolean {
  return entry.method === 'POST' && entry.path === '/api/auth/login' && entry.status === 200
}
