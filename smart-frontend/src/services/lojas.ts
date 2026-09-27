import { api } from '@/lib/api'
import type { Loja, PaginatedResponse } from '@/types'

export const lojasService = {
  list: (page = 0, size = 10) =>
    api.get<PaginatedResponse<Loja>>(`/admin/lojas?page=${page}&size=${size}`),

  getByPublicId: (publicId: string) => api.get<Loja>(`/admin/lojas/${publicId}`),

  create: (data: {
    nomeFantasia: string
    razaoSocial: string
    cnpj: string
    telefone: string
    ehMatriz: boolean
    endereco: {
      cep: string
      logradouro: string
      numero: string
      bairro: string
      cidade: string
      estado: string
      referencia?: string
    }
  }) => api.post<Loja>('/admin/lojas', data),

  update: (
    publicId: string,
    data: {
      nomeFantasia: string
      razaoSocial: string
      cnpj: string
      telefone: string
      ehMatriz: boolean
      endereco: {
        cep: string
        logradouro: string
        numero: string
        bairro: string
        cidade: string
        estado: string
        referencia?: string
      }
    },
  ) => api.put<Loja>(`/admin/lojas/${publicId}`, data),

  delete: (publicId: string) => api.delete<void>(`/admin/lojas/${publicId}`),

  getVendaAdmin: (lojaPublicId: string) =>
    api.get<VendedorLoja>(`/admin/venda-admins?lojaPublicId=${lojaPublicId}`),

  createVendaAdmin: (data: {
    lojaPublicId: string
    nome: string
    telefone: string
    email: string
    senha: string
  }) => api.post<VendedorLoja>('/admin/venda-admins', data),
}

export interface VendedorLoja {
  publicId: string
  nome: string
  telefone: string
  email: string
  ativo: boolean
}
