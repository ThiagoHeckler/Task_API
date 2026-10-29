import { TopBar } from '@/components/TopBar'
import { DemoApp } from './DemoApp'

export default function Page() {
  return (
    <div className="flex h-dvh flex-col">
      <TopBar />
      <DemoApp />
    </div>
  )
}
