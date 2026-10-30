import type { TimelineEntry } from '@/lib/timeline'
import { isLoginSuccess } from './isLoginSuccess'

const METHOD_STYLES: Record<TimelineEntry['method'], string> = {
  POST: 'bg-green-600',
  GET: 'bg-blue-600',
  PUT: 'bg-orange-500',
  DELETE: 'bg-red-600',
}

function statusStyle(status: number | null): string {
  if (status === null) return 'text-slate-400'
  if (status >= 200 && status < 300) return 'text-green-400'
  if (status >= 400 && status < 500) return 'text-yellow-300'
  return 'text-red-400'
}

function toJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}

type TimelineRowProps = {
  entry: TimelineEntry
  expanded: boolean
  onToggle: () => void
}

export function TimelineRow({ entry, expanded, onToggle }: TimelineRowProps) {
  return (
    <li className="border-b border-slate-800">
      <button
        type="button"
        aria-expanded={expanded}
        onClick={onToggle}
        className="flex w-full items-center gap-3 px-4 py-2 text-left hover:bg-slate-800/60"
      >
        <span
          className={`w-16 shrink-0 rounded px-1.5 py-0.5 text-center text-xs font-bold text-white ${METHOD_STYLES[entry.method]}`}
        >
          {entry.method}
        </span>
        <span className="min-w-0 flex-1 truncate">{entry.path}</span>
        <span className={`shrink-0 font-bold ${statusStyle(entry.status)}`}>
          {entry.status ?? '…'}
        </span>
        <span className="w-16 shrink-0 text-right text-xs text-slate-400">
          {entry.durationMs === null ? '' : `${entry.durationMs} ms`}
        </span>
      </button>
      {expanded && (
        <div className="space-y-3 px-4 pb-4 text-xs">
          <div>
            <p className="mb-1 text-slate-400">Request</p>
            <pre className="overflow-x-auto rounded bg-slate-950 p-3">
              {toJson(entry.requestHeaders)}
            </pre>
            {entry.requestBody !== null && (
              <pre className="mt-2 overflow-x-auto rounded bg-slate-950 p-3">
                {toJson(entry.requestBody)}
              </pre>
            )}
          </div>
          <div>
            <p className="mb-1 text-slate-400">Response</p>
            {isLoginSuccess(entry) && (
              <p className="mb-1 font-bold text-indigo-300">Token JWT gerado</p>
            )}
            <pre
              className={`overflow-x-auto rounded bg-slate-950 p-3 ${
                isLoginSuccess(entry) ? 'ring-2 ring-indigo-400' : ''
              }`}
            >
              {entry.status === null ? '…' : toJson(entry.responseBody)}
            </pre>
          </div>
        </div>
      )}
    </li>
  )
}
