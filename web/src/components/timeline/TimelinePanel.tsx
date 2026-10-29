'use client'

import { useState } from 'react'
import { useTimeline } from '@/lib/timeline'
import { TimelineRow } from './TimelineRow'

type TimelinePanelProps = {
  waiting: boolean
}

export function TimelinePanel({ waiting }: TimelinePanelProps) {
  const timeline = useTimeline()
  // Rows the visitor opened or closed; the rest follow the default: only the
  // newest is expanded, so older ones fold away as new calls arrive.
  const [toggled, setToggled] = useState<Record<number, boolean>>({})
  const newestId = timeline[0]?.id

  return (
    <div className="min-h-full bg-slate-900 font-mono text-sm text-slate-100">
      {waiting && (
        <p className="border-b border-slate-800 px-4 py-2 text-slate-400 italic">
          aguardando você salvar…
        </p>
      )}
      {timeline.length === 0 && !waiting ? (
        <p className="px-4 py-8 text-center text-slate-400">As chamadas da API aparecem aqui</p>
      ) : (
        <ul>
          {timeline.map((entry) => {
            const expanded = toggled[entry.id] ?? entry.id === newestId
            return (
              <TimelineRow
                key={entry.id}
                entry={entry}
                expanded={expanded}
                onToggle={() => setToggled((prev) => ({ ...prev, [entry.id]: !expanded }))}
              />
            )
          })}
        </ul>
      )}
    </div>
  )
}
