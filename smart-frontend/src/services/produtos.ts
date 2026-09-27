import { api } from '@/lib/api'
import type { Produto, TagRfid } from '@/types'

export const produtosService = {
  list: (lojaPublicId: string) =>
    api.get<Produto[]>(`/admin/produtos?lojaPublicId=${lojaPublicId}`),

  getByPublicId: (publicId: string) => api.get<Produto>(`/admin/produtos/${publicId}`),

  create: (data: {
    lojaPublicId: string
    nome: string
    descricao?: string
    codigoBarras?: string
    preco: number // em centavos
    quantidade: number
    dataValidade?: string
  }) => api.post<Produto>('/admin/produtos', data),

  update: (publicId: string, data: {
    nome: string
    descricao?: string
    codigoBarras?: string
    preco: number // em centavos
    quantidade: number
    dataValidade?: string
  }) => api.put<Produto>(`/admin/produtos/${publicId}`, data),

  inativar: (publicId: string) =>
    api.delete<Produto>(`/admin/produtos/${publicId}`),

  desvincularRfid: (produtoPublicId: string, codigoRfid: string) =>
    api.delete<Produto>(`/admin/produtos/rfid/desvincular?produtoPublicId=${produtoPublicId}&codigoRfid=${codigoRfid}`),

  desvincularTodosRfid: (produtoPublicId: string) =>
    api.delete<Produto>(`/admin/produtos/rfid/desvincular-todos?produtoPublicId=${produtoPublicId}`),

  listarTagsRfid: (produtoPublicId: string) =>
    api.get<Array<{ publicId: string; codigoRfid: string; criadoEm: string }>>(
      `/admin/produtos/rfid/tags/${produtoPublicId}`
    ),

  solicitarLeituraRfid: (produtoPublicId: string, embarcadoKey: string) =>
    api.post<{ leituraPublicId: string }>(`/admin/produtos/rfid/solicitar-leitura`, {
      produtoPublicId,
      embarcadoKey,
    }),

  getLeituraStatus: (leituraPublicId: string) =>
    api.get<{ status: string; codigoRfid?: string }>(
      `/admin/produtos/rfid/leitura/${leituraPublicId}`,
    ),

  vincularRfid: (produtoPublicId: string, codigoRfid: string) =>
    api.post<TagRfid>(`/admin/produtos/rfid/vincular?produtoPublicId=${produtoPublicId}&codigoRfid=${codigoRfid}`),

  cancelarLeitura: (leituraPublicId: string) =>
    api.post<void>('/admin/produtos/rfid/cancelar', { leituraPublicId }),
}
