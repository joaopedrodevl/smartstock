import type { ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Loading } from '@/components/ui/loading'
import { AuthProvider, useAuth } from '@/hooks/use-auth'
import { DashboardPage } from '@/pages/dashboard'
import { EmbarcadosPage } from '@/pages/embarcados'
import { HistoricoVendasPage } from '@/pages/historico'
import { LoginPage } from '@/pages/login'
import { LojasPage } from '@/pages/lojas'
import { ProdutosPage } from '@/pages/produtos'
import { VendasPage } from '@/pages/vendas'

function ProtectedRoute({
  children,
  allowVendaAdmin = false,
}: {
  children: ReactNode
  allowVendaAdmin?: boolean
}) {
  const { isAuthenticated, isLoading, role } = useAuth()

  if (isLoading) {
    return (
      <div className="min-h-screen bg-base flex items-center justify-center">
        <Loading />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (role === 'ROLE_VENDA_ADMIN' && !allowVendaAdmin) {
    return <Navigate to="/vendas" replace />
  }

  return <>{children}</>
}

function PublicRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="min-h-screen bg-base flex items-center justify-center">
        <Loading />
      </div>
    )
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route
            path="/login"
            element={
              <PublicRoute>
                <LoginPage />
              </PublicRoute>
            }
          />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/lojas"
            element={
              <ProtectedRoute>
                <LojasPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/produtos"
            element={
              <ProtectedRoute>
                <ProdutosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/embarcados"
            element={
              <ProtectedRoute>
                <EmbarcadosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vendas"
            element={
              <ProtectedRoute allowVendaAdmin>
                <VendasPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vendas/historico"
            element={
              <ProtectedRoute>
                <HistoricoVendasPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
