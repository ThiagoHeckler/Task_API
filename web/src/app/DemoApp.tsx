'use client'

import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { SplitLayout } from '@/components/SplitLayout'
import { TimelinePanel } from '@/components/timeline/TimelinePanel'
import { Onboarding } from '@/features/onboarding/Onboarding'
import { SystemShell } from '@/features/system/SystemShell'
import { configureApi } from '@/lib/api'
import { clearToken, useToken } from '@/lib/session'

const SESSION_EXPIRED = 'Sua sessão expirou. Crie uma nova conta de demonstração.'

export function DemoApp() {
  const queryClient = useQueryClient()
  const token = useToken()
  // True while a form was filled in and the visitor has not saved it yet.
  const [waiting, setWaiting] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)

  // Expired token or account already deleted: back to signup, nothing cached.
  // The timeline is kept so the 401 stays visible.
  useEffect(() => {
    configureApi({
      onUnauthorized: () => {
        clearToken()
        queryClient.clear()
        setNotice(SESSION_EXPIRED)
      },
    })
  }, [queryClient])

  const system = token ? (
    <SystemShell onWaitingChange={setWaiting} />
  ) : (
    <Onboarding onLoggedIn={() => setNotice(null)} onWaitingChange={setWaiting} notice={notice} />
  )

  return <SplitLayout system={system} api={<TimelinePanel waiting={waiting} />} />
}
