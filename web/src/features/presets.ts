import { toInputDateTime } from '@/lib/dates'
import type { TaskPriority } from '@/lib/types'

// Sample data the typewriter fills in. Names already in use get a numeric
// suffix ("Estudos 2"), so the demo never hits the 409 on its own.

export type CategoryPreset = { name: string; color: string }

export const CATEGORY_PRESETS: CategoryPreset[] = [
  { name: 'Estudos', color: '#4f46e5' },
  { name: 'Trabalho', color: '#0ea5e9' },
  { name: 'Pessoal', color: '#22c55e' },
]

export const TAG_PRESETS: { name: string }[] = [
  { name: 'urgente' },
  { name: 'leitura' },
  { name: 'backend' },
]

// Colors an edit cycles through, so "Editar" always suggests a visible change.
export const CATEGORY_COLORS = ['#4f46e5', '#0ea5e9', '#22c55e', '#f59e0b', '#ef4444']

export function nextPreset<P extends { name: string }>(presets: P[], usedNames: string[]): P {
  const used = new Set(usedNames)
  for (let round = 1; ; round++) {
    for (const preset of presets) {
      const name = round === 1 ? preset.name : `${preset.name} ${round}`
      if (!used.has(name)) return { ...preset, name }
    }
  }
}

export function nextColor(current: string): string {
  const index = CATEGORY_COLORS.indexOf(current.toLowerCase())
  return CATEGORY_COLORS[(index + 1) % CATEGORY_COLORS.length]
}

// Suggested rename on edit; editing again takes the suffix back off.
function toggleSuffix(name: string, suffix: string): string {
  return name.endsWith(suffix) ? name.slice(0, -suffix.length) : `${name}${suffix}`
}

export function editedTagName(name: string): string {
  return toggleSuffix(name, '-editada')
}

export function editedUserName(name: string): string {
  return toggleSuffix(name, ' (editado)')
}

export const TASK_PRESETS: { title: string; description: string; priority: TaskPriority }[] = [
  {
    title: 'Estudar Spring Security',
    description: 'Configurar o filtro JWT e testar as rotas protegidas',
    priority: 'HIGH',
  },
  { title: 'Revisar o README', description: 'Explicar como rodar a demo localmente', priority: 'MEDIUM' },
  { title: 'Gravar vídeo da demo', description: 'Mostrar a linha do tempo das chamadas', priority: 'LOW' },
]

const PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH']

export function nextPriority(current: TaskPriority): TaskPriority {
  return PRIORITIES[(PRIORITIES.indexOf(current) + 1) % PRIORITIES.length]
}

// A week from now at 18:00 (São Paulo), as a datetime-local value.
export function defaultDueDate(): string {
  const inAWeek = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
  return `${toInputDateTime(inAWeek.toISOString().slice(0, 19)).slice(0, 10)}T18:00`
}
