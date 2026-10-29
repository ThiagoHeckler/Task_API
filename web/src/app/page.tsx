import { SplitLayout } from '@/components/SplitLayout'
import { TimelinePanel } from '@/components/timeline/TimelinePanel'
import { TopBar } from '@/components/TopBar'

export default function Page() {
  return (
    <div className="flex h-dvh flex-col">
      <TopBar />
      <SplitLayout
        system={
          <main className="flex h-full items-center justify-center p-6 text-slate-500">
            Sistema de tarefas
          </main>
        }
        api={<TimelinePanel waiting={false} />}
      />
    </div>
  )
}
