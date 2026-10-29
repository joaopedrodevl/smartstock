import { createContext, type ReactNode, useCallback, useContext, useEffect, useState } from 'react'
import { api } from '@/lib/api'

interface AuthContextType {
  isAuthenticated: boolean
  isLoading: boolean
  role: string | null
  login: (email: string, senha: string) => Promise<void>
  logout: () => Promise<void>
}

function decodeScope(token: string): string | null {
  try {
    const payload = atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
    return (JSON.parse(payload) as { scope?: string }).scope ?? null
  } catch {
    return null
  }
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [role, setRole] = useState<string | null>(null)

  useEffect(() => {
    async function checkAuth() {
      try {
        const data = await api.post<{ token: string }>('/token/refresh')
        setRole(decodeScope(data.token))
        setIsAuthenticated(true)
      } catch {
        setIsAuthenticated(false)
        setRole(null)
      } finally {
        setIsLoading(false)
      }
    }
    checkAuth()
  }, [])

  const login = useCallback(async (email: string, senha: string) => {
    const data = await api.login(email, senha)
    setRole(decodeScope(data.token))
    setIsAuthenticated(true)
  }, [])

  const logout = useCallback(async () => {
    try {
      await api.logout()
    } finally {
      setIsAuthenticated(false)
      setRole(null)
    }
  }, [])

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading, role, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
