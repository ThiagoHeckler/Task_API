export function TopBar() {
  return (
    <header className="flex items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-3">
      <h1 className="text-lg font-semibold text-slate-900">Task API</h1>
      <nav className="flex items-center gap-3 text-sm">
        <a
          href="https://github.com/ThiagoHeckler/Task_API"
          target="_blank"
          rel="noreferrer"
          className="text-slate-600 hover:text-slate-900"
        >
          GitHub
        </a>
        <a
          href="/api/swagger-ui/index.html"
          className="rounded-md bg-indigo-600 px-3 py-1.5 font-medium text-white hover:bg-indigo-700"
        >
          Ver somente API
        </a>
      </nav>
    </header>
  )
}
