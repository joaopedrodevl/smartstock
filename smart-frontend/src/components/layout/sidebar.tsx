import { Cpu, History, LayoutDashboard, LogOut, Package, ShoppingCart, Store } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'
import { cn } from '@/lib/utils'

const navigation = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, vendaAdmin: false },
  { to: '/lojas', label: 'Lojas', icon: Store, vendaAdmin: false },
  { to: '/produtos', label: 'Produtos', icon: Package, vendaAdmin: false },
  { to: '/embarcados', label: 'Embarcados', icon: Cpu, vendaAdmin: false },
  { to: '/vendas', label: 'Vendas', icon: ShoppingCart, vendaAdmin: true },
  { to: '/vendas/historico', label: 'Histórico', icon: History, vendaAdmin: false },
]

export function Sidebar() {
  const { logout, role } = useAuth()
  const isVendaAdmin = role === 'ROLE_VENDA_ADMIN'

  const items = navigation.filter((item) => !isVendaAdmin || item.vendaAdmin)

  return (
    <aside className="fixed inset-y-0 left-0 z-30 flex w-60 flex-col border-r border-border bg-white">
      <div className="flex h-16 items-center gap-2 border-b border-border px-5">
        <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary">
          <span className="text-sm font-bold text-white font-display">S</span>
        </div>
        <span className="text-lg font-bold font-display text-text">SmartStock</span>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4">
        <ul className="flex flex-col gap-1">
          {items.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                end={item.to === '/' || item.to === '/vendas'}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-primary/10 text-primary'
                      : 'text-text-secondary hover:bg-surface hover:text-text',
                  )
                }
              >
                <item.icon size={18} />
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="border-t border-border p-3">
        <button
          onClick={() => logout()}
          className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-text-secondary hover:bg-surface hover:text-text transition-colors"
        >
          <LogOut size={18} />
          Sair
        </button>
      </div>
    </aside>
  )
}
