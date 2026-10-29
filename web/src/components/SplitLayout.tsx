'use client'

import { useState, useSyncExternalStore, type ReactNode } from 'react'
import { useTimeline } from '@/lib/timeline'

const WIDE_QUERY = '(min-width: 768px)'

function subscribeToWidth(onChange: () => void) {
  const query = window.matchMedia(WIDE_QUERY)
  query.addEventListener('change', onChange)
  return () => query.removeEventListener('change', onChange)
}

// The static export is prerendered without a window: assume the wide layout
// there, and the browser corrects it on hydration.
function useIsWide(): boolean {
  return useSyncExternalStore(
    subscribeToWidth,
    () => window.matchMedia(WIDE_QUERY).matches,
    () => true,
  )
}

type Tab = 'system' | 'api'

type SplitLayoutProps = {
  system: ReactNode
  api: ReactNode
}

export function SplitLayout({ system, api }: SplitLayoutProps) {
  const isWide = useIsWide()
  const [tab, setTab] = useState<Tab>('system')
  const timeline = useTimeline()
  // Highest timeline id the visitor has seen on the API tab.
  const [seenId, setSeenId] = useState(0)

  const newestId = timeline[0]?.id ?? 0
  const apiVisible = isWide || tab === 'api'
  if (apiVisible && seenId !== newestId) setSeenId(newestId)
  const unseen = apiVisible ? 0 : timeline.filter((entry) => entry.id > seenId).length

  if (isWide) {
    return (
      <div className="grid min-h-0 flex-1 grid-cols-2">
        <section className="min-h-0 overflow-y-auto">{system}</section>
        <section className="min-h-0 overflow-y-auto">{api}</section>
      </div>
    )
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div role="tablist" className="flex border-b border-slate-200 bg-white">
        <TabButton id="system" active={tab === 'system'} onSelect={setTab}>
          Sistema
        </TabButton>
        <TabButton id="api" active={tab === 'api'} onSelect={setTab}>
          API
          {unseen > 0 && (
            <>
              <span aria-hidden className="ml-2 rounded-full bg-indigo-600 px-2 text-xs text-white">
                {unseen}
              </span>
              <span className="sr-only">, {unseen} novas</span>
            </>
          )}
        </TabButton>
      </div>
      {/* Both panels stay mounted so the system side keeps what was typed. */}
      <section
        role="tabpanel"
        id="panel-system"
        aria-labelledby="tab-system"
        hidden={tab !== 'system'}
        className="min-h-0 flex-1 overflow-y-auto"
      >
        {system}
      </section>
      <section
        role="tabpanel"
        id="panel-api"
        aria-labelledby="tab-api"
        hidden={tab !== 'api'}
        className="min-h-0 flex-1 overflow-y-auto"
      >
        {api}
      </section>
    </div>
  )
}

type TabButtonProps = {
  id: Tab
  active: boolean
  onSelect: (tab: Tab) => void
  children: ReactNode
}

function TabButton({ id, active, onSelect, children }: TabButtonProps) {
  return (
    <button
      type="button"
      role="tab"
      id={`tab-${id}`}
      aria-selected={active}
      aria-controls={`panel-${id}`}
      onClick={() => onSelect(id)}
      className={`flex-1 py-2 text-sm font-medium ${
        active ? 'border-b-2 border-indigo-600 text-indigo-600' : 'text-slate-600'
      }`}
    >
      {children}
    </button>
  )
}
