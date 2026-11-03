type LockScreenProps = {
  message: string
}

// The daily signup cap resets at midnight in São Paulo (the API's Retry-After
// always points there), so the time is stated instead of counted down.
export function LockScreen({ message }: LockScreenProps) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 p-6 text-center">
      <p className="text-lg font-semibold text-slate-900">{message}</p>
      <p className="text-slate-600">Volta a funcionar à meia-noite (horário de Brasília).</p>
      <a
        href="/api/swagger-ui/index.html"
        className="rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
      >
        Ver somente API
      </a>
    </div>
  )
}
