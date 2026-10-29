'use client'

import { useState } from 'react'
import { CategoriesPage } from '@/features/categories/CategoriesPage'
import { TagsPage } from '@/features/tags/TagsPage'

const SECTIONS = [
  { id: 'tasks', label: 'Tarefas' },
  { id: 'categories', label: 'Categorias' },
  { id: 'tags', label: 'Tags' },
  { id: 'account', label: 'Minha conta' },
] as const

type Section = (typeof SECTIONS)[number]['id']

type SystemShellProps = {
  onWaitingChange: (waiting: boolean) => void
}

export function SystemShell({ onWaitingChange }: SystemShellProps) {
  const [section, setSection] = useState<Section>('tasks')

  return (
    <div className="flex h-full flex-col md:flex-row">
      <nav
        aria-label="Seções"
        className="flex shrink-0 gap-1 overflow-x-auto border-b border-slate-200 p-2 md:w-44 md:flex-col md:border-r md:border-b-0"
      >
        {SECTIONS.map(({ id, label }) => (
          <button
            key={id}
            type="button"
            aria-current={section === id ? 'page' : undefined}
            onClick={() => setSection(id)}
            className={`rounded-md px-3 py-2 text-left text-sm whitespace-nowrap ${
              section === id
                ? 'bg-indigo-50 font-medium text-indigo-700'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            {label}
          </button>
        ))}
      </nav>
      {/* Only the active section is mounted: opening it is what loads its list. */}
      <main className="min-w-0 flex-1 p-4">
        {section === 'categories' && <CategoriesPage onWaitingChange={onWaitingChange} />}
        {section === 'tags' && <TagsPage onWaitingChange={onWaitingChange} />}
        {(section === 'tasks' || section === 'account') && (
          <p className="text-slate-500">Em construção.</p>
        )}
      </main>
    </div>
  )
}
