import { api } from '@/lib/api'
import type { Embarcado } from '@/types'

export const embarcadosService = {
  list: (lojaPublicId: string) =>
    api.get<Embarcado[]>(`/admin/embarcados?lojaPublicId=${lojaPublicId}`),

  getByPublicId: (publicId: string) => api.get<Embarcado>(`/admin/embarcados/${publicId}`),

  create: (data: {
    lojaPublicId: string
    tipo: 'VENDA' | 'CADASTRO'
    descricao: string
  }) => api.post<Embarcado>('/admin/embarcados', data),

  delete: (publicId: string) => api.delete<void>(`/admin/embarcados/${publicId}`),

  getStatus: (lojaPublicId: string) =>
    api.get<Record<string, boolean>>(`/admin/embarcados/status?lojaPublicId=${lojaPublicId}`),
}
