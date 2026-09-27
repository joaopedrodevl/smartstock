import { api } from '@/lib/api'
import type { Cliente, Embarcado, FormaPagamento, PagamentoResponse, Produto, Venda } from '@/types'

export interface LeituraRfid {
  leituraPublicId: string
  status: 'PENDENTE' | 'LIDO' | 'EXPIRADO' | 'CANCELADO'
  codigoRfid?: string
  expiraEm?: string
}

export const vendasService = {
  list: (lojaPublicId: string) =>
    api.get<Venda[]>(`/vendas/historico?lojaPublicId=${lojaPublicId}`),

  minhaLoja: () => api.get<{ publicId: string; nomeFantasia: string }>('/vendas/minha-loja'),

  meusEmbarcados: () => api.get<Embarcado[]>('/vendas/embarcados'),

  create: (data: {
    lojaPublicId: string
    cpfCliente: string
    telefoneCliente?: string
    codigosRfid: string[]
  }) => api.post<Venda>('/vendas/criar', data),

  processarPagamento: (vendaPublicId: string, formaPagamento: FormaPagamento) =>
    api.post<PagamentoResponse>('/vendas/pagamento', { vendaPublicId, formaPagamento }),

  consultarCliente: (cpf: string) => api.post<Cliente>('/vendas/cliente/consultar', { cpf }),

  buscarProduto: (codigoRfid: string) =>
    api.get<Produto>(`/vendas/produto/buscar?codigoRfid=${codigoRfid}`),

  solicitarLeituraRfid: (embarcadoKey: string) =>
    api.post<LeituraRfid>('/vendas/rfid/solicitar-leitura', { embarcadoKey }),

  getLeituraStatus: (leituraPublicId: string) =>
    api.get<LeituraRfid>(`/vendas/rfid/leitura/${leituraPublicId}`),

  cancelarLeitura: (leituraPublicId: string) =>
    api.post<void>('/vendas/rfid/cancelar', { leituraPublicId }),
}
