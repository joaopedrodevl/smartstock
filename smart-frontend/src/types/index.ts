export interface Admin {
  publicId: string
  nome: string
  telefone: string
  email: string
  role: string
  ativo: boolean
}

export interface Endereco {
  publicId: string
  cep: string
  logradouro: string
  numero: string
  bairro: string
  cidade: string
  estado: string
  referencia?: string
}

export interface Loja {
  publicId: string
  administradorId: string
  enderecoId: string
  endereco?: Endereco
  cnpj: string
  nomeFantasia: string
  razaoSocial: string
  telefone: string
  ehMatriz: boolean
  ativo: boolean
}

export interface Produto {
  publicId: string
  lojaId: string
  codigoBarras?: string
  nome: string
  descricao?: string
  preco: number // em centavos
  estoque: number
  tagsVinculadas: number
  ativo: boolean
  dataValidade?: string
  tagRfid?: TagRfid
}

export interface TagRfid {
  publicId: string
  lojaId: string
  produtoId: string
  codigoRfid: string
  ativo: boolean
}

export interface Embarcado {
  publicId: string
  lojaId: string
  chavePublica: string
  tipo: 'VENDA' | 'CADASTRO'
  descricao: string
  ativo: boolean
}

export type FormaPagamento = 'dinheiro' | 'pix' | 'debito' | 'credito' | 'outro'
export type StatusVenda = 'aberta' | 'finalizada' | 'cancelada'

export interface Venda {
  publicId: string
  lojaPublicId?: string
  valorTotal: number // em centavos
  formaPagamento?: FormaPagamento
  status: StatusVenda
  criadoEm?: string
  itens?: ItemVenda[]
}

export interface ItemVenda {
  codigoRfid: string
  nomeProduto: string
  precoUnitario: number // em centavos
}

export interface PagamentoResponse {
  vendaPublicId: string
  status: string
  formaPagamento: string
  valorTotal: number
  processadoEm: string
}

export interface Cliente {
  existe?: boolean
  cpf: string
  telefone?: string
  nome?: string
}

export interface LeituraRfid {
  publicId: string
  status: 'PENDENTE' | 'LIDO' | 'EXPIRADO' | 'CANCELADO'
  codigoRfid?: string
  produtoId?: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface LoginRequest {
  email: string
  senha: string
}
