import type { TaskFilter } from './api'

const FILTERS: { value: TaskFilter; label: string }[] = [
  { value: 'all', label: 'Todas' },
  { value: 'root', label: 'Principais' },
  { value: 'PENDING', label: 'Pendentes' },
  { value: 'IN_PROGRESS', label: 'Em andamento' },
  { value: 'COMPLETED', label: 'Concluídas' },
]

type TaskFiltersProps = {
  value: TaskFilter
  onChange: (filter: TaskFilter) => void
}

export function TaskFilters({ value, onChange }: TaskFiltersProps) {
  return (
    <div role="group" aria-label="Filtrar tarefas" className="flex flex-wrap gap-1">
      {FILTERS.map((filter) => (
        <button
          key={filter.value}
          type="button"
          aria-pressed={value === filter.value}
          onClick={() => onChange(filter.value)}
          className={`rounded-full px-3 py-1 text-sm ${
            value === filter.value
              ? 'bg-indigo-600 text-white'
              : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
          }`}
        >
          {filter.label}
        </button>
      ))}
    </div>
  )
}
