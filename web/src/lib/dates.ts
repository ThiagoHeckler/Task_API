const TIME_ZONE = 'America/Sao_Paulo'

const saoPauloFormat = new Intl.DateTimeFormat('pt-BR', {
  timeZone: TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

// The API returns LocalDateTime in UTC without an offset.
function saoPauloParts(apiValue: string): Record<string, string> {
  return Object.fromEntries(
    saoPauloFormat.formatToParts(new Date(`${apiValue}Z`)).map((p) => [p.type, p.value]),
  )
}

export function formatApiDate(value: string | null): string {
  if (value === null) return '—'
  const parts = saoPauloParts(value)
  return `${parts.day}/${parts.month}/${parts.year} ${parts.hour}:${parts.minute}`
}

// API value (UTC) to the São Paulo wall time an <input type="datetime-local"> shows.
export function toInputDateTime(value: string | null): string {
  if (value === null) return ''
  const parts = saoPauloParts(value)
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`
}

// Offset of São Paulo from UTC, in minutes, at the given instant.
function saoPauloOffsetMinutes(instant: Date): number {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-US', {
      timeZone: TIME_ZONE,
      timeZoneName: 'longOffset',
    })
      .formatToParts(instant)
      .map((p) => [p.type, p.value]),
  )
  // "GMT-03:00", or "GMT" when the offset is zero.
  const match = /GMT([+-])(\d{2}):(\d{2})/.exec(parts.timeZoneName)
  if (!match) return 0
  const minutes = Number(match[2]) * 60 + Number(match[3])
  return match[1] === '-' ? -minutes : minutes
}

// Value of <input type="datetime-local"> (São Paulo wall time) to the UTC
// LocalDateTime string the API expects.
export function toApiDateTime(local: string): string {
  const asUtc = new Date(`${local}Z`)
  const offset = saoPauloOffsetMinutes(asUtc)
  const utc = new Date(asUtc.getTime() - offset * 60_000)
  return utc.toISOString().slice(0, 19)
}
