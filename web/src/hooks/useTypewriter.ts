import { useCallback, useEffect, useState } from 'react'

type TypewriterOptions<F> = {
  charDelayMs?: number
  // Fields filled in one step when their turn comes (select, date inputs).
  instant?: (keyof F)[]
  // Starting values (edit forms): fields already equal to the target are
  // skipped; a field that is not a prefix of its target is cleared and retyped.
  from?: F
}

type Typewriter<F> = {
  values: F
  setValue: (field: keyof F, value: string) => void
  done: boolean
  typing: boolean
  restart: (next: F) => void
}

function emptyLike<F extends Record<string, string>>(target: F): F {
  return Object.fromEntries(Object.keys(target).map((key) => [key, ''])) as F
}

// Fills a form field by field, one character per tick, in key order. Any edit
// by the visitor stops it for good: what they typed is what gets sent.
export function useTypewriter<F extends Record<string, string>>(
  initialTarget: F,
  opts: TypewriterOptions<F> = {},
): Typewriter<F> {
  const charDelayMs = opts.charDelayMs ?? 25
  const [instant] = useState(() => new Set(opts.instant ?? []))
  const [target, setTarget] = useState(initialTarget)
  const [values, setValues] = useState(() => opts.from ?? emptyLike(initialTarget))
  const [stopped, setStopped] = useState(false)

  const nextField = (Object.keys(target) as (keyof F)[]).find((key) => values[key] !== target[key])
  const done = stopped || nextField === undefined

  useEffect(() => {
    if (done || nextField === undefined) return
    const timer = setTimeout(() => {
      setValues((prev) => {
        const full = target[nextField]
        const current = prev[nextField]
        let next: string
        if (instant.has(nextField)) next = full
        else if (!full.startsWith(current)) next = ''
        else next = full.slice(0, current.length + 1)
        return { ...prev, [nextField]: next }
      })
    }, charDelayMs)
    return () => clearTimeout(timer)
  }, [done, nextField, values, target, instant, charDelayMs])

  const setValue = useCallback((field: keyof F, value: string) => {
    setStopped(true)
    setValues((prev) => ({ ...prev, [field]: value }))
  }, [])

  const restart = useCallback((next: F) => {
    setTarget(next)
    setValues(emptyLike(next))
    setStopped(false)
  }, [])

  return { values, setValue, done, typing: !done, restart }
}
