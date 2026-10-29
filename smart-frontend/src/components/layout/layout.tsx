import type { ReactNode } from 'react'
import { useAuth } from '@/hooks/use-auth'
import { Sidebar } from './sidebar'

export function Layout({ children }: { children: ReactNode }) {
  const { role } = useAuth()
  const isVendaAdmin = role === 'ROLE_VENDA_ADMIN'

  return (
    <div className="min-h-screen bg-base">
      {!isVendaAdmin && <Sidebar />}
      <main className={isVendaAdmin ? '' : 'pl-60'}>
        <div className="mx-auto max-w-7xl px-6 py-8">{children}</div>
      </main>
    </div>
  )
}
