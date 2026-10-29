import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Package, Plus, Radio } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Layout } from '@/components/layout/layout'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { EmptyState } from '@/components/ui/empty-state'
import { Input } from '@/components/ui/input'
import { Loading } from '@/components/ui/loading'
import { Modal } from '@/components/ui/modal'
import { PageHeader } from '@/components/ui/page-header'
import { Select } from '@/components/ui/select'
import { formatCurrency, formatDate, maskBarcode, maskCurrency } from '@/lib/utils'
import { embarcadosService } from '@/services/embarcados'
import { lojasService } from '@/services/lojas'
import { produtosService } from '@/services/produtos'
import type { Produto } from '@/types'

export function ProdutosPage() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingProduto, setEditingProduto] = useState<Produto | null>(null)
  const [selectedLoja, setSelectedLoja] = useState<string>('')
  const [rfidModal, setRfidModal] = useState<Produto | null>(null)

  const { data: lojas } = useQuery({
    queryKey: ['lojas'],
    queryFn: () => lojasService.list(0, 100),
  })

  const lojaId = selectedLoja || lojas?.content?.[0]?.publicId || ''

  const { data: produtos, isLoading } = useQuery({
    queryKey: ['produtos', lojaId],
    queryFn: () => produtosService.list(lojaId),
    enabled: !!lojaId,
  })

  function handleEdit(produto: Produto) {
    setEditingProduto(produto)
    setModalOpen(true)
  }

  function handleNew() {
    setEditingProduto(null)
    setModalOpen(true)
  }

  return (
    <Layout>
      <PageHeader
        title="Produtos"
        description="Catálogo de produtos com vínculo RFID"
        actions={
          <div className="flex gap-2">
            {lojas && lojas.content.length > 1 && (
              <Select
                options={lojas.content.map((l) => ({ value: l.publicId, label: l.nomeFantasia }))}
                value={lojaId}
                onChange={(e) => setSelectedLoja(e.target.value)}
                className="w-48"
              />
            )}
            <Button onClick={handleNew} size="sm" disabled={!lojaId}>
              <Plus size={16} />
              Novo Produto
            </Button>
          </div>
        }
      />

      {isLoading ? (
        <Loading />
      ) : !produtos?.length ? (
        <EmptyState
          icon={<Package size={40} />}
          title="Nenhum produto cadastrado"
          description="Adicione produtos ao seu catálogo para começar a vender."
        />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-left text-text-secondary">
                <th className="pb-2 font-medium">Produto</th>
                <th className="pb-2 font-medium">Preço</th>
                <th className="pb-2 font-medium">Estoque</th>
                <th className="pb-2 font-medium">Validade</th>
                <th className="pb-2 font-medium">RFID</th>
                <th className="pb-2 font-medium text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {produtos.map((produto) => (
                <tr key={produto.publicId} className="border-b border-border/50">
                  <td className="py-3">
                    <div>
                      <p className="font-medium text-text">{produto.nome}</p>
                      {produto.codigoBarras && (
                        <p className="text-xs text-text-secondary font-mono">
                          {produto.codigoBarras}
                        </p>
                      )}
                    </div>
                  </td>
                  <td className="py-3 font-medium">{formatCurrency(produto.preco)}</td>
                  <td className="py-3">
                    <span
                      className={`font-medium ${produto.estoque <= 5 ? 'text-error' : 'text-text'}`}
                    >
                      {produto.estoque}
                    </span>
                    {produto.estoque <= 5 && (
                      <Badge variant="warning" className="ml-2">
                        Baixo
                      </Badge>
                    )}
                  </td>
                  <td className="py-3 text-text-secondary">
                    {produto.dataValidade ? formatDate(produto.dataValidade) : '—'}
                  </td>
                  <td className="py-3">
                    {produto.tagRfid ? (
                      <Badge variant="success">
                        <Radio size={12} className="mr-1" />
                        {produto.tagRfid.codigoRfid.slice(0, 8)}…
                      </Badge>
                    ) : (
                      <Button variant="ghost" size="sm" onClick={() => setRfidModal(produto)}>
                        <Radio size={14} />
                        {produto.tagsVinculadas}/{produto.estoque}
                      </Button>
                    )}
                  </td>
                  <td className="py-3 text-right">
                    <Button variant="ghost" size="sm" onClick={() => handleEdit(produto)}>
                      Editar
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ProdutoModal
        open={modalOpen}
        onClose={() => {
          setModalOpen(false)
          setEditingProduto(null)
        }}
        lojaId={lojaId}
        produto={editingProduto}
      />

      {rfidModal && (
        <RfidModal open={!!rfidModal} onClose={() => setRfidModal(null)} produto={rfidModal} lojaId={lojaId} />
      )}
    </Layout>
  )
}

function ProdutoModal({
  open,
  onClose,
  lojaId,
  produto,
}: {
  open: boolean
  onClose: () => void
  lojaId: string
  produto: Produto | null
}) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    nome: '',
    descricao: '',
    codigoBarras: '',
    preco: '',
    quantidade: '',
    dataValidade: '',
  })

  useEffect(() => {
    if (open) {
      if (produto) {
        setForm({
          nome: produto.nome,
          descricao: produto.descricao ?? '',
          codigoBarras: produto.codigoBarras ?? '',
          preco: (produto.preco / 100).toFixed(2).replace('.', ','),
          quantidade: String(produto.estoque),
          dataValidade: produto.dataValidade ?? '',
        })
      } else {
        setForm({
          nome: '',
          descricao: '',
          codigoBarras: '',
          preco: '',
          quantidade: '',
          dataValidade: '',
        })
      }
    }
  }, [open, produto])

  const createMutation = useMutation({
    mutationFn: () => {
      const precoCentavos = Math.round(Number(form.preco.replace(',', '.')) * 100)
      return produtosService.create({
        lojaPublicId: lojaId,
        nome: form.nome,
        descricao: form.descricao || undefined,
        codigoBarras: form.codigoBarras?.replace(/\D/g, '') || undefined,
        preco: precoCentavos,
        quantidade: Number(form.quantidade),
        dataValidade: form.dataValidade || undefined,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['produtos', lojaId] })
      onClose()
    },
  })

  const updateMutation = useMutation({
    mutationFn: () => {
      const precoCentavos = Math.round(Number(form.preco.replace(',', '.')) * 100)
      return produtosService.update(produto!.publicId, {
        nome: form.nome,
        descricao: form.descricao || undefined,
        codigoBarras: form.codigoBarras?.replace(/\D/g, '') || undefined,
        preco: precoCentavos,
        quantidade: Number(form.quantidade),
        dataValidade: form.dataValidade || undefined,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['produtos', lojaId] })
      onClose()
    },
  })

  const mutation = produto ? updateMutation : createMutation

  const inativarMutation = useMutation({
    mutationFn: () => produtosService.inativar(produto!.publicId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['produtos', lojaId] })
      onClose()
    },
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    mutation.mutate()
  }

  return (
    <Modal open={open} onClose={onClose} title={produto ? 'Editar Produto' : 'Novo Produto'}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <Input
          label="Nome"
          value={form.nome}
          onChange={(e) => setForm({ ...form, nome: e.target.value })}
          placeholder="Ex: Coca-Cola 2L"
          required
        />
        <Input
          label="Descrição"
          value={form.descricao}
          onChange={(e) => setForm({ ...form, descricao: e.target.value })}
          placeholder="Ex: Refrigerante Coca-Cola 2 litros"
        />
        <Input
          label="Código de Barras"
          value={form.codigoBarras}
          onChange={(e) => setForm({ ...form, codigoBarras: maskBarcode(e.target.value) })}
          placeholder="0000000000000"
        />
        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Preço (R$)"
            value={form.preco}
            onChange={(e) => setForm({ ...form, preco: maskCurrency(e.target.value) })}
            placeholder="0,00"
            required
          />
          <Input
            label="Quantidade"
            type="number"
            min="0"
            value={form.quantidade}
            onChange={(e) => setForm({ ...form, quantidade: e.target.value })}
            placeholder="0"
            required
          />
        </div>
        <Input
          label="Data de Validade"
          type="date"
          value={form.dataValidade}
          onChange={(e) => setForm({ ...form, dataValidade: e.target.value })}
        />

        {mutation.isError && <p className="text-sm text-error">{mutation.error.message}</p>}

        <div className="flex justify-between mt-2">
          {produto && (
            <Button
              type="button"
              variant="danger"
              onClick={() => {
                if (confirm('Tem certeza que deseja inativar este produto?')) {
                  inativarMutation.mutate()
                }
              }}
              loading={inativarMutation.isPending}
            >
              Inativar
            </Button>
          )}
          <div className="flex gap-2 ml-auto">
            <Button type="button" variant="secondary" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" loading={mutation.isPending}>
              {produto ? 'Salvar' : 'Criar'}
            </Button>
          </div>
        </div>
      </form>
    </Modal>
  )
}

function RfidModal({
  open,
  onClose,
  produto,
  lojaId,
}: {
  open: boolean
  onClose: () => void
  produto: Produto
  lojaId: string
}) {
  const queryClient = useQueryClient()
  const [leituraId, setLeituraId] = useState<string | null>(null)
  const [rfidCode, setRfidCode] = useState('')

  const { data: embarcados } = useQuery({
    queryKey: ['embarcados', lojaId],
    queryFn: () => embarcadosService.list(lojaId),
    enabled: !!lojaId,
  })

  const { data: tags, refetch: refetchTags } = useQuery({
    queryKey: ['tagsRfid', produto.publicId],
    queryFn: () => produtosService.listarTagsRfid(produto.publicId),
  })

  const cadastroKey = embarcados?.find((e) => e.tipo === 'CADASTRO')?.chavePublica
  const limiteAtingido = tags !== undefined && tags.length >= produto.estoque

  const solicitarMutation = useMutation({
    mutationFn: () => {
      if (!cadastroKey) throw new Error('Nenhum embarcado CADASTRO conectado')
      return produtosService.solicitarLeituraRfid(produto.publicId, cadastroKey)
    },
    onSuccess: (data) => {
      setLeituraId(data.leituraPublicId)
    },
  })

  const { data: leituraStatus } = useQuery({
    queryKey: ['leituraRfid', leituraId],
    queryFn: () => produtosService.getLeituraStatus(leituraId!),
    enabled: !!leituraId,
    refetchInterval: 1000,
  })

  useEffect(() => {
    if (leituraStatus?.status === 'LIDO' && leituraStatus?.codigoRfid) {
      setRfidCode(leituraStatus.codigoRfid)
      setLeituraId(null)
    } else if (leituraStatus?.status === 'EXPIRADO' || leituraStatus?.status === 'CANCELADO') {
      setLeituraId(null)
    }
  }, [leituraStatus])

  const vincularMutation = useMutation({
    mutationFn: () => produtosService.vincularRfid(produto.publicId, rfidCode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['produtos'] })
      refetchTags()
      setRfidCode('')
    },
  })

  const desvincularMutation = useMutation({
    mutationFn: (codigoRfid: string) => produtosService.desvincularRfid(produto.publicId, codigoRfid),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['produtos'] })
      refetchTags()
    },
  })

  const desvincularTodosMutation = useMutation({
    mutationFn: () => produtosService.desvincularTodosRfid(produto.publicId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['produtos'] })
      refetchTags()
    },
  })

  function handleClose() {
    setLeituraId(null)
    setRfidCode('')
    onClose()
  }

  return (
    <Modal open={open} onClose={handleClose} title="Gerenciar RFID">
      <div className="flex flex-col gap-4">
        <p className="text-sm text-text-secondary">
          Produto: <span className="font-medium text-text">{produto.nome}</span>
        </p>

        {tags && tags.length > 0 && (
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-text">
                Tags Vinculadas ({tags.length})
              </p>
              {tags.length > 1 && (
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => {
                    if (confirm('Tem certeza que deseja desvincular todas as tags?')) {
                      desvincularTodosMutation.mutate()
                    }
                  }}
                  loading={desvincularTodosMutation.isPending}
                >
                  Desvincular Todas
                </Button>
              )}
            </div>
            <div className="flex flex-col gap-1">
              {tags.map((tag) => (
                <div key={tag.publicId} className="flex items-center justify-between rounded bg-bg-secondary px-3 py-2">
                  <span className="font-mono text-sm text-text">{tag.codigoRfid}</span>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      if (confirm(`Desvincular tag ${tag.codigoRfid}?`)) {
                        desvincularMutation.mutate(tag.codigoRfid)
                      }
                    }}
                    loading={desvincularMutation.isPending}
                  >
                    Desvincular
                  </Button>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="border-t border-border pt-4">
          <p className="text-sm font-medium text-text mb-2">Vincular Nova Tag</p>
          {limiteAtingido ? (
            <p className="text-sm text-text-secondary">
              Limite de tags atingido ({tags.length}/{produto.estoque})
            </p>
          ) : !leituraId ? (
            <div className="flex flex-col gap-3">
              <p className="text-sm text-text-secondary">
                Aproxime a tag RFID do leitor para vincular ao produto.
              </p>
              <div className="flex gap-2">
                <Button
                  onClick={() => solicitarMutation.mutate()}
                  loading={solicitarMutation.isPending}
                  disabled={!cadastroKey || limiteAtingido}
                >
                  <Radio size={16} />
                  Solicitar Leitura
                </Button>
                <div className="flex-1">
                  <Input
                    placeholder="Ou digite o código RFID"
                    value={rfidCode}
                    onChange={(e) => setRfidCode(e.target.value)}
                  />
                </div>
              </div>
              {rfidCode && (
                <Button
                  onClick={() => vincularMutation.mutate()}
                  loading={vincularMutation.isPending}
                >
                  Vincular
                </Button>
              )}
            </div>
          ) : (
            <div className="flex flex-col items-center gap-4 py-4">
              <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary border-t-transparent" />
              <p className="text-sm text-text-secondary">Aguardando leitura da tag RFID...</p>
              <p className="text-xs text-text-secondary">Timeout: 30 segundos</p>
              <Button variant="ghost" size="sm" onClick={handleClose}>
                Cancelar
              </Button>
            </div>
          )}
        </div>

        {solicitarMutation.isError && (
          <p className="text-sm text-error">{solicitarMutation.error.message}</p>
        )}
        {vincularMutation.isError && (
          <p className="text-sm text-error">{vincularMutation.error.message}</p>
        )}
        {desvincularMutation.isError && (
          <p className="text-sm text-error">{desvincularMutation.error.message}</p>
        )}
        {desvincularTodosMutation.isError && (
          <p className="text-sm text-error">{desvincularTodosMutation.error.message}</p>
        )}
      </div>
    </Modal>
  )
}
