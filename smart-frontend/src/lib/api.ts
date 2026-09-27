const BASE_URL = '/v1'

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

function getCsrfToken(): string | null {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : null
}

interface RequestOptions extends RequestInit {
  skipCsrf?: boolean
}

async function request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { skipCsrf = false, ...fetchOptions } = options

  const headers = new Headers(fetchOptions.headers)

  if (!headers.has('Content-Type') && fetchOptions.body) {
    headers.set('Content-Type', 'application/json')
  }

  if (
    !skipCsrf &&
    !['GET', 'HEAD', 'OPTIONS'].includes(fetchOptions.method?.toUpperCase() ?? 'GET')
  ) {
    const csrfToken = getCsrfToken()
    if (csrfToken) {
      headers.set('X-XSRF-TOKEN', csrfToken)
    }
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...fetchOptions,
    headers,
    credentials: 'include',
  })

  if (response.status === 401 && !skipCsrf) {
    const refreshed = await tryRefresh()
    if (refreshed) {
      return request<T>(endpoint, options)
    }
    window.location.href = '/login'
    throw new Error('Sessão expirada')
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({ mensagem: 'Erro desconhecido' }))
    const message =
      (error as { mensagem?: string }).mensagem ||
      (error as { message?: string }).message ||
      `Erro ${response.status}`
    throw new ApiError(message, response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json()
}

async function tryRefresh(): Promise<boolean> {
  try {
    const response = await fetch(`${BASE_URL}/token/refresh`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'X-XSRF-TOKEN': getCsrfToken() ?? '',
      },
    })
    return response.ok
  } catch {
    return false
  }
}

export const api = {
  get: <T>(endpoint: string) => request<T>(endpoint, { method: 'GET' }),

  post: <T>(endpoint: string, body?: unknown) =>
    request<T>(endpoint, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    }),

  put: <T>(endpoint: string, body?: unknown) =>
    request<T>(endpoint, {
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    }),

  delete: <T>(endpoint: string) => request<T>(endpoint, { method: 'DELETE' }),

  login: (email: string, senha: string) =>
    request<{ token: string }>('/token/admin', {
      method: 'POST',
      body: JSON.stringify({ email, senha }),
      skipCsrf: true,
    }),

  logout: () => request<void>('/token/logout', { method: 'POST' }),
}
