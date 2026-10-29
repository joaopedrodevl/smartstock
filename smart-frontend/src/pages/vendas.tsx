import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, Check, CreditCard, Plus, Radio, ShoppingCart, Trash2, X } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Layout } from '@/components/layout/layout'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Modal } from '@/components/ui/modal'
import { PageHeader } from '@/components/ui/page-header'
import { Select } from '@/components/ui/select'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api'
import { formatCurrency, isValidCpf, isValidPhone, maskCpf, maskPhone } from '@/lib/utils'
import { embarcadosService } from '@/services/embarcados'
import { lojasService } from '@/services/lojas'
import { vendasService } from '@/services/vendas'
import type { Cliente, FormaPagamento, Produto } from '@/types'

type VendaStatus = 'INICIANDO' | 'CLIENTE' | 'PRODUTOS' | 'PAGAMENTO'

type ResultadoLeitura =
  | 'AGUARDANDO'
  | 'TEMPO_ESGOTADO'
  | 'NAO_ENCONTRADO'
  | 'TAG_DUPLICADA'
  | 'ESTOQUE_ESGOTADO'
  | 'ERRO'

interface CartItem {
  produto: Produto
  rfids: string[]
}

export function VendasPage() {
  const queryClient = useQueryClient()
  const { role } = useAuth()
  const isVendaAdmin = role === 'ROLE_VENDA_ADMIN'
  const [status, setStatus] = useState<VendaStatus>('INICIANDO')
  const [selectedLoja, setSelectedLoja] = useState<string>('')
  const [cart, setCart] = useState<CartItem[]>([])
  const [clienteCpf, setClienteCpf] = useState('')
  const [clienteTelefone, setClienteTelefone] = useState('')
  const [cliente, setCliente] = useState<Cliente | null>(null)
  const [clienteNovo, setClienteNovo] = useState(false)
  const [pagamentoModal, setPagamentoModal] = useState(false)
  const [cancelarModal, setCancelarModal] = useState(false)
  const [leituraModal, setLeituraModal] = useState(false)
  const [leituraId, setLeituraId] = useState<string | null>(null)
  const [leituraExpiraEm, setLeituraExpiraEm] = useState<number | null>(null)
  const [segundosRestantes, setSegundosRestantes] = useState(30)
  const [leituraResultado, setLeituraResultado] = useState<ResultadoLeitura>('AGUARDANDO')
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const cpfConsultadoRef = useRef('')

  const { data: lojas } = useQuery({
    queryKey: ['lojas'],
    queryFn: () => lojasService.list(0, 100),
    enabled: !isVendaAdmin,
  })

  const { data: minhaLoja } = useQuery({
    queryKey: ['minha-loja'],
    queryFn: () => vendasService.minhaLoja(),
    enabled: isVendaAdmin,
  })

  const lojaId = isVendaAdmin
    ? (minhaLoja?.publicId ?? '')
    : selectedLoja || lojas?.content?.[0]?.publicId || ''

  const { data: embarcadosAdmin } = useQuery({
    queryKey: ['embarcados', lojaId],
    queryFn: () => embarcadosService.list(lojaId),
    enabled: !!lojaId && !isVendaAdmin,
  })

  const { data: meusEmbarcados } = useQuery({
    queryKey: ['meus-embarcados'],
    queryFn: () => vendasService.meusEmbarcados(),
    enabled: isVendaAdmin,
  })

  const embarcados = isVendaAdmin ? meusEmbarcados : embarcadosAdmin

  const embarcadoVenda = embarcados?.find((e) => e.tipo === 'VENDA')

  const clienteMutation = useMutation({
    mutationFn: vendasService.consultarCliente,
    onSuccess: (data) => {
      setCliente(data)
      setClienteNovo(!data.existe)
    },
    onError: () => {
      setCliente(null)
      setClienteNovo(true)
    },
  })

  const leituraMutation = useMutation({
    mutationFn: vendasService.solicitarLeituraRfid,
    onSuccess: (data) => {
      setLeituraId(data.leituraPublicId)
      setLeituraExpiraEm(data.expiraEm ? new Date(data.expiraEm).getTime() : Date.now() + 30_000)
      setLeituraResultado('AGUARDANDO')
    },
  })

  const leituraStatusQuery = useQuery({
    queryKey: ['leitura-rfid', leituraId],
    queryFn: () => vendasService.getLeituraStatus(leituraId!),
    enabled: !!leituraId,
    refetchInterval: 1000,
  })

  const buscarProdutoMutation = useMutation({
    mutationFn: vendasService.buscarProduto,
    onSuccess: (data, codigoRfid) => {
      setLeituraId(null)
      setLeituraExpiraEm(null)
      const resultado = addToCart(data, codigoRfid)
      if (resultado === 'ok') {
        setLeituraModal(false)
      } else {
        setLeituraResultado(
          resultado === 'duplicada' ? 'TAG_DUPLICADA' : 'ESTOQUE_ESGOTADO',
        )
      }
    },
    onError: (error) => {
      setLeituraId(null)
      setLeituraExpiraEm(null)
      setLeituraResultado(
        error instanceof ApiError && error.status === 404 ? 'NAO_ENCONTRADO' : 'ERRO',
      )
    },
  })

  const criarVendaMutation = useMutation({
    mutationFn: async (formaPagamento: FormaPagamento) => {
      const venda = await vendasService.create({
        lojaPublicId: lojaId,
        cpfCliente: cliente?.cpf || clienteCpf.replace(/\D/g, ''),
        telefoneCliente: clienteNovo ? clienteTelefone : undefined,
        codigosRfid: cart.flatMap((item) => item.rfids),
      })
      return vendasService.processarPagamento(venda.publicId, formaPagamento)
    },
    onSuccess: () => {
      resetarVenda()
      queryClient.invalidateQueries({ queryKey: ['vendas-historico'] })
    },
  })

  useEffect(() => {
    if (leituraStatusQuery.data?.status === 'LIDO' && leituraStatusQuery.data?.codigoRfid) {
      buscarProdutoMutation.mutate(leituraStatusQuery.data.codigoRfid)
    } else if (leituraStatusQuery.data?.status === 'EXPIRADO' || leituraStatusQuery.data?.status === 'CANCELADO') {
      setLeituraId(null)
      setLeituraExpiraEm(null)
      setLeituraResultado('TEMPO_ESGOTADO')
    }
  }, [leituraStatusQuery.data])

  useEffect(() => {
    if (!leituraExpiraEm) return
    const tick = () => {
      const restante = Math.ceil((leituraExpiraEm - Date.now()) / 1000)
      setSegundosRestantes(Math.max(0, restante))
    }
    tick()
    const timer = setInterval(tick, 500)
    return () => clearInterval(timer)
  }, [leituraExpiraEm])

  useEffect(() => {
    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current)
      }
    }
  }, [])

  const resetarVenda = useCallback(() => {
    setStatus('INICIANDO')
    setCart([])
    setClienteCpf('')
    setClienteTelefone('')
    setCliente(null)
    setClienteNovo(false)
    setPagamentoModal(false)
    setCancelarModal(false)
    setLeituraModal(false)
    setLeituraId(null)
    setLeituraExpiraEm(null)
    setLeituraResultado('AGUARDANDO')
    cpfConsultadoRef.current = ''
  }, [])

  const iniciarVenda = useCallback(() => {
    setStatus('CLIENTE')
  }, [])

  const handleCpfChange = useCallback(
    (value: string) => {
      const masked = maskCpf(value)
      setClienteCpf(masked)
      const digits = masked.replace(/\D/g, '')
      if (digits === cpfConsultadoRef.current) return
      const completo = digits.length === 11 && isValidCpf(digits)
      cpfConsultadoRef.current = completo ? digits : ''
      setCliente(null)
      setClienteNovo(false)
      if (completo) {
        clienteMutation.mutate(digits)
      }
    },
    [clienteMutation],
  )

  const avancarParaProdutos = useCallback(() => {
    setStatus('PRODUTOS')
  }, [])

  const adicionarProduto = useCallback(() => {
    if (!embarcadoVenda) return
    setLeituraId(null)
    setLeituraExpiraEm(null)
    setLeituraResultado('AGUARDANDO')
    setLeituraModal(true)
    leituraMutation.mutate(embarcadoVenda.chavePublica)
  }, [embarcadoVenda, leituraMutation])

  const cancelarLeitura = useCallback(() => {
    if (leituraId) {
      vendasService.cancelarLeitura(leituraId)
    }
    setLeituraId(null)
    setLeituraExpiraEm(null)
    setLeituraModal(false)
  }, [leituraId])

  function addToCart(produto: Produto, codigoRfid: string): 'ok' | 'duplicada' | 'estoque' {
    const existing = cart.find((item) => item.produto.publicId === produto.publicId)
    if (existing) {
      if (existing.rfids.includes(codigoRfid)) return 'duplicada'
      if (existing.rfids.length >= produto.estoque) return 'estoque'
    } else if (produto.estoque < 1) {
      return 'estoque'
    }
    setCart((prev) => {
      const existing = prev.find((item) => item.produto.publicId === produto.publicId)
      if (existing) {
        return prev.map((item) =>
          item.produto.publicId === produto.publicId
            ? { ...item, rfids: [...item.rfids, codigoRfid] }
            : item,
        )
      }
      return [...prev, { produto, rfids: [codigoRfid] }]
    })
    return 'ok'
  }

  function removeFromCart(produtoId: string) {
    setCart((prev) => prev.filter((item) => item.produto.publicId !== produtoId))
  }

  const total = cart.reduce((acc, item) => acc + item.produto.preco * item.rfids.length, 0)

  return (
    <Layout>
      <PageHeader
        title="Terminal de Vendas"
        description="Registre vendas escaneando RFID ou adicionando produtos"
        actions={
          lojas && lojas.content.length > 1 ? (
            <Select
              options={lojas.content.map((l) => ({ value: l.publicId, label: l.nomeFantasia }))}
              value={selectedLoja}
              onChange={(e) => setSelectedLoja(e.target.value)}
              className="w-48"
            />
          ) : undefined
        }
      />

      {/* Tela de Início */}
      {status === 'INICIANDO' && (
        <div className="flex items-center justify-center min-h-[60vh]">
          <Card className="w-full max-w-md">
            <CardContent className="flex flex-col items-center gap-6 py-12">
              <div className="flex h-20 w-20 items-center justify-center rounded-full bg-primary/10">
                <ShoppingCart size={40} className="text-primary" />
              </div>
              <div className="text-center">
                <h2 className="text-xl font-semibold font-display text-text">Iniciar Nova Venda</h2>
                <p className="text-sm text-text-secondary mt-1">
                  Clique abaixo para iniciar uma nova venda
                </p>
              </div>
              <Button size="lg" className="w-full" onClick={iniciarVenda}>
                <ShoppingCart size={20} />
                Iniciar Venda
              </Button>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tela de Cliente */}
      {status === 'CLIENTE' && (
        <div className="flex items-center justify-center min-h-[60vh]">
          <Card className="w-full max-w-md">
            <CardTitle>Dados do Cliente</CardTitle>
            <CardContent className="mt-4 flex flex-col gap-4">
              <div>
                <label htmlFor="cliente-cpf" className="text-sm font-medium text-text mb-1 block">
                  CPF
                </label>
                <Input
                  id="cliente-cpf"
                  placeholder="000.000.000-00"
                  inputMode="numeric"
                  value={clienteCpf}
                  onChange={(e) => handleCpfChange(e.target.value)}
                  error={
                    clienteCpf.replace(/\D/g, '').length === 11 && !isValidCpf(clienteCpf)
                      ? 'CPF inválido. Verifique os dígitos.'
                      : undefined
                  }
                />
                {clienteMutation.isPending && (
                  <p className="text-xs text-text-secondary">Consultando cliente...</p>
                )}
              </div>

              {cliente && !clienteNovo && (
                <div className="rounded-md border border-border p-3">
                  <p className="text-sm">
                    <span className="text-text-secondary">Cliente encontrado</span>
                  </p>
                  <p className="text-sm">
                    <span className="text-text-secondary">CPF:</span>{' '}
                    <span className="font-mono">{cliente.cpf}</span>
                  </p>
                  <p className="text-sm">
                    <span className="text-text-secondary">Telefone:</span>{' '}
                    <span className="font-medium">{cliente.telefone || '—'}</span>
                  </p>
                </div>
              )}

              {clienteNovo && (
                <div className="rounded-md border border-yellow-200 bg-yellow-50 p-3">
                  <p className="text-sm text-yellow-800 mb-2">
                    Cliente não encontrado. Será cadastrado ao finalizar a venda.
                  </p>
                  <label htmlFor="cliente-telefone" className="text-sm font-medium text-text mb-1 block">
                    Telefone *
                  </label>
                  <Input
                    id="cliente-telefone"
                    placeholder="(00) 00000-0000"
                    inputMode="numeric"
                    value={clienteTelefone}
                    onChange={(e) => setClienteTelefone(maskPhone(e.target.value))}
                    error={
                      clienteTelefone.replace(/\D/g, '').length >= 10 &&
                      !isValidPhone(clienteTelefone)
                        ? 'Telefone inválido. Use DDD válido e número completo.'
                        : undefined
                    }
                  />
                </div>
              )}

              <div className="flex gap-2 mt-2">
                <Button variant="secondary" className="flex-1" onClick={resetarVenda}>
                  Voltar
                </Button>
                <Button
                  className="flex-1"
                  onClick={avancarParaProdutos}
                  disabled={
                    !clienteCpf ||
                    !isValidCpf(clienteCpf) ||
                    clienteMutation.isPending ||
                    (clienteNovo && !isValidPhone(clienteTelefone))
                  }
                >
                  Continuar
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tela de Produtos */}
      {status === 'PRODUTOS' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 flex flex-col gap-4">
            {/* Botão Adicionar Produto */}
            <Card>
              <CardContent className="py-4">
                <Button
                  className="w-full"
                  size="lg"
                  onClick={adicionarProduto}
                  disabled={!embarcadoVenda}
                >
                  <Plus size={20} />
                  Adicionar Produto
                </Button>
                {!embarcadoVenda && (
                  <p className="text-xs text-text-secondary text-center mt-2">
                    Nenhum embarcado VENDA conectado
                  </p>
                )}
              </CardContent>
            </Card>

            {/* Carrinho */}
            <Card>
              <CardTitle>Itens da Venda</CardTitle>
              <CardContent className="mt-3">
                {cart.length === 0 ? (
                  <p className="text-sm text-text-secondary py-6 text-center">
                    Nenhum item adicionado. Clique em "Adicionar Produto" para começar.
                  </p>
                ) : (
                  <div className="flex flex-col gap-2">
                    {cart.map((item) => (
                      <div
                        key={item.produto.publicId}
                        className="flex items-center justify-between rounded-md border border-border/50 px-3 py-2"
                      >
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium truncate">{item.produto.nome}</p>
                          <p className="text-xs text-text-secondary">
                            {formatCurrency(item.produto.preco)} un.
                          </p>
                        </div>
                        <div className="flex items-center gap-2 shrink-0">
                          <span className="w-6 text-center text-sm font-medium">
                            {item.rfids.length}
                          </span>
                          <span className="w-20 text-right text-sm font-medium">
                            {formatCurrency(item.produto.preco * item.rfids.length)}
                          </span>
                          <button
                            type="button"
                            onClick={() => removeFromCart(item.produto.publicId)}
                            className="ml-2 text-text-secondary hover:text-error"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Resumo da venda */}
          <div className="flex flex-col gap-4">
            <Card className="sticky top-8">
              <CardTitle>Resumo</CardTitle>
              <CardContent className="mt-3 flex flex-col gap-3">
                {cliente && (
                  <div className="flex justify-between text-sm">
                    <span className="text-text-secondary">Cliente</span>
                    <span className="font-medium">{cliente.nome || cliente.cpf}</span>
                  </div>
                )}
                <div className="flex justify-between text-sm">
                  <span className="text-text-secondary">Itens</span>
                  <span className="font-medium">{cart.length}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-text-secondary">Quantidade total</span>
                  <span className="font-medium">
                    {cart.reduce((acc, item) => acc + item.rfids.length, 0)}
                  </span>
                </div>
                <div className="border-t border-border pt-3 flex justify-between">
                  <span className="font-semibold font-display">Total</span>
                  <span className="text-xl font-bold font-display text-primary">
                    {formatCurrency(total)}
                  </span>
                </div>

                <Button
                  variant="secondary"
                  className="w-full mt-2"
                  onClick={() => setCancelarModal(true)}
                  disabled={cart.length === 0}
                >
                  <X size={16} />
                  Cancelar Venda
                </Button>
                <Button
                  className="w-full"
                  disabled={cart.length === 0}
                  onClick={() => setPagamentoModal(true)}
                >
                  <CreditCard size={16} />
                  Finalizar Venda
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {/* Modal de Leitura RFID */}
      <LeituraRfidModal
        open={leituraModal}
        onClose={cancelarLeitura}
        onRetry={adicionarProduto}
        resultado={leituraResultado}
        segundosRestantes={segundosRestantes}
        conectando={leituraMutation.isPending && !leituraExpiraEm}
        erroSolicitacao={leituraMutation.isError ? leituraMutation.error.message : null}
      />

      {/* Modal de Pagamento */}
      <PagamentoModal
        open={pagamentoModal}
        onClose={() => setPagamentoModal(false)}
        total={total}
        onPay={(forma) => criarVendaMutation.mutate(forma)}
        loading={criarVendaMutation.isPending}
      />

      {/* Modal de Cancelar Venda */}
      <CancelarVendaModal
        open={cancelarModal}
        onCancel={() => setCancelarModal(false)}
        onConfirm={resetarVenda}
      />
    </Layout>
  )
}

function LeituraRfidModal({
  open,
  onClose,
  onRetry,
  resultado,
  segundosRestantes,
  conectando,
  erroSolicitacao,
}: {
  open: boolean
  onClose: () => void
  onRetry: () => void
  resultado: ResultadoLeitura
  segundosRestantes: number
  conectando: boolean
  erroSolicitacao: string | null
}) {
  return (
    <Modal open={open} onClose={onClose} title="Adicionar Produto">
      <div className="flex flex-col items-center gap-4 py-2">
        {resultado === 'AGUARDANDO' && !erroSolicitacao && (
          <>
            <div className="relative flex h-20 w-20 items-center justify-center">
              <span className="absolute h-16 w-16 animate-ping rounded-full bg-primary/20" />
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
                <Radio size={28} className="text-primary animate-pulse" />
              </div>
            </div>
            <p className="text-sm font-medium text-text text-center">
              Aproxime o produto ao leitor
            </p>
            {conectando ? (
              <p className="text-xs text-text-secondary">Conectando ao leitor...</p>
            ) : (
              <>
                <p className="text-xs text-text-secondary -mt-2">
                  Aguardando leitura da tag RFID...
                </p>
                <span className="font-mono text-2xl font-bold text-primary tabular-nums">
                  {String(segundosRestantes).padStart(2, '0')}s
                </span>
              </>
            )}
            <Button variant="secondary" size="sm" onClick={onClose}>
              <X size={14} />
              Cancelar
            </Button>
          </>
        )}

        {resultado === 'AGUARDANDO' && erroSolicitacao && (
          <>
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
              <AlertTriangle size={28} className="text-red-600" />
            </div>
            <p className="text-sm text-error text-center">{erroSolicitacao}</p>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={onClose}>
                Fechar
              </Button>
              <Button onClick={onRetry}>
                <Radio size={16} />
                Tentar novamente
              </Button>
            </div>
          </>
        )}

        {resultado === 'TEMPO_ESGOTADO' && (
          <>
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
              <AlertTriangle size={28} className="text-red-600" />
            </div>
            <p className="text-sm font-medium text-text">Tempo esgotado</p>
            <p className="text-xs text-text-secondary -mt-2 text-center">
              Nenhuma tag foi lida em 30 segundos.
            </p>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={onClose}>
                Fechar
              </Button>
              <Button onClick={onRetry}>
                <Radio size={16} />
                Tentar novamente
              </Button>
            </div>
          </>
        )}

        {resultado === 'NAO_ENCONTRADO' && (
          <>
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-yellow-50">
              <AlertTriangle size={28} className="text-yellow-600" />
            </div>
            <p className="text-sm font-medium text-text">Produto não encontrado</p>
            <p className="text-xs text-text-secondary -mt-2 text-center">
              A tag lida não está vinculada a nenhum produto.
            </p>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={onClose}>
                Fechar
              </Button>
              <Button onClick={onRetry}>
                <Radio size={16} />
                Ler outra tag
              </Button>
            </div>
          </>
        )}

        {resultado === 'TAG_DUPLICADA' && (
          <>
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-yellow-50">
              <AlertTriangle size={28} className="text-yellow-600" />
            </div>
            <p className="text-sm font-medium text-text">Tag já adicionada</p>
            <p className="text-xs text-text-secondary -mt-2 text-center">
              Esta tag já está no carrinho. Aproxime outro produto.
            </p>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={onClose}>
                Fechar
              </Button>
              <Button onClick={onRetry}>
                <Radio size={16} />
                Ler outra tag
              </Button>
            </div>
          </>
        )}

        {resultado === 'ESTOQUE_ESGOTADO' && (
          <>
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-yellow-50">
              <AlertTriangle size={28} className="text-yellow-600" />
            </div>
            <p className="text-sm font-medium text-text">Estoque insuficiente</p>
            <p className="text-xs text-text-secondary -mt-2 text-center">
              Não há mais unidades deste produto em estoque.
            </p>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={onClose}>
                Fechar
              </Button>
              <Button onClick={onRetry}>
                <Radio size={16} />
                Ler outra tag
              </Button>
            </div>
          </>
        )}

        {resultado === 'ERRO' && (
          <>
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
              <AlertTriangle size={28} className="text-red-600" />
            </div>
            <p className="text-sm font-medium text-text">Erro ao processar leitura</p>
            <p className="text-xs text-text-secondary -mt-2 text-center">
              Tente novamente em instantes.
            </p>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={onClose}>
                Fechar
              </Button>
              <Button onClick={onRetry}>
                <Radio size={16} />
                Tentar novamente
              </Button>
            </div>
          </>
        )}
      </div>
    </Modal>
  )
}

function PagamentoModal({
  open,
  onClose,
  total,
  onPay,
  loading,
}: {
  open: boolean
  onClose: () => void
  total: number
  onPay: (forma: FormaPagamento) => void
  loading: boolean
}) {
  const [forma, setForma] = useState<FormaPagamento>('pix')
  const [dadosCartao, setDadosCartao] = useState({
    numero: '',
    nome: '',
    validade: '',
    cvv: '',
  })

  const formas: { value: FormaPagamento; label: string; icon: string }[] = [
    { value: 'pix', label: 'PIX', icon: '🔑' },
    { value: 'dinheiro', label: 'Dinheiro', icon: '💵' },
    { value: 'debito', label: 'Cartão de Débito', icon: '💳' },
    { value: 'credito', label: 'Cartão de Crédito', icon: '💳' },
    { value: 'outro', label: 'Outro', icon: '📋' },
  ]

  function handleConfirmar() {
    onPay(forma)
  }

  return (
    <Modal open={open} onClose={onClose} title="Pagamento">
      <div className="flex flex-col gap-4">
        <div className="text-center py-2">
          <p className="text-sm text-text-secondary">Valor a pagar</p>
          <p className="text-3xl font-bold font-display text-primary">{formatCurrency(total)}</p>
        </div>

        <div className="flex flex-col gap-2">
          {formas.map((f) => (
            <button
              key={f.value}
              onClick={() => setForma(f.value)}
              className={`flex items-center gap-3 rounded-md border px-4 py-3 text-sm font-medium transition-colors ${
                forma === f.value
                  ? 'border-primary bg-primary/5 text-primary'
                  : 'border-border hover:bg-surface'
              }`}
            >
              <span>{f.icon}</span>
              {f.label}
            </button>
          ))}
        </div>

        {/* QR Code PIX Mockado */}
        {forma === 'pix' && (
          <div className="flex flex-col items-center gap-3 py-4 border-t border-border">
            <p className="text-sm text-text-secondary">Escaneie o QR Code abaixo</p>
            <div className="w-48 h-48 bg-white border-2 border-border rounded-lg flex items-center justify-center">
              <div className="text-center">
                <div className="w-40 h-40 bg-gray-100 rounded flex items-center justify-center">
                  <svg viewBox="0 0 100 100" className="w-32 h-32">
                    <rect x="5" y="5" width="25" height="25" fill="black" />
                    <rect x="70" y="5" width="25" height="25" fill="black" />
                    <rect x="5" y="70" width="25" height="25" fill="black" />
                    <rect x="35" y="5" width="8" height="8" fill="black" />
                    <rect x="35" y="18" width="8" height="8" fill="black" />
                    <rect x="35" y="35" width="8" height="8" fill="black" />
                    <rect x="5" y="35" width="8" height="8" fill="black" />
                    <rect x="18" y="35" width="8" height="8" fill="black" />
                    <rect x="50" y="5" width="8" height="8" fill="black" />
                    <rect x="50" y="18" width="8" height="8" fill="black" />
                    <rect x="50" y="35" width="8" height="8" fill="black" />
                    <rect x="70" y="35" width="8" height="8" fill="black" />
                    <rect x="83" y="35" width="8" height="8" fill="black" />
                    <rect x="35" y="50" width="8" height="8" fill="black" />
                    <rect x="50" y="50" width="8" height="8" fill="black" />
                    <rect x="65" y="50" width="8" height="8" fill="black" />
                    <rect x="80" y="50" width="8" height="8" fill="black" />
                    <rect x="5" y="50" width="8" height="8" fill="black" />
                    <rect x="70" y="70" width="25" height="25" fill="black" />
                  </svg>
                </div>
              </div>
            </div>
            <p className="text-xs text-text-secondary">Chave PIX: smartstock@email.com</p>
          </div>
        )}

        {/* Dados do Cartão Mockado */}
        {(forma === 'credito' || forma === 'debito') && (
          <div className="flex flex-col gap-3 py-4 border-t border-border">
            <p className="text-sm text-text-secondary">Dados do Cartão (simulado)</p>
            <Input
              placeholder="Número do cartão"
              value={dadosCartao.numero}
              onChange={(e) => setDadosCartao({ ...dadosCartao, numero: e.target.value })}
            />
            <Input
              placeholder="Nome no cartão"
              value={dadosCartao.nome}
              onChange={(e) => setDadosCartao({ ...dadosCartao, nome: e.target.value })}
            />
            <div className="flex gap-2">
              <Input
                placeholder="Validade"
                value={dadosCartao.validade}
                onChange={(e) => setDadosCartao({ ...dadosCartao, validade: e.target.value })}
              />
              <Input
                placeholder="CVV"
                value={dadosCartao.cvv}
                onChange={(e) => setDadosCartao({ ...dadosCartao, cvv: e.target.value })}
              />
            </div>
          </div>
        )}

        <Button className="w-full" onClick={handleConfirmar} loading={loading}>
          <Check size={16} />
          Confirmar Pagamento
        </Button>
      </div>
    </Modal>
  )
}

function CancelarVendaModal({
  open,
  onCancel,
  onConfirm,
}: {
  open: boolean
  onCancel: () => void
  onConfirm: () => void
}) {
  return (
    <Modal open={open} onClose={onCancel} title="Cancelar Venda">
      <div className="flex flex-col items-center gap-4 py-2">
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
          <AlertTriangle size={28} className="text-red-600" />
        </div>
        <p className="text-center text-sm text-text">
          Deseja realmente cancelar esta venda? Todos os itens serão removidos.
        </p>
        <div className="flex w-full gap-2 mt-2">
          <Button variant="secondary" className="flex-1" onClick={onCancel}>
            Voltar
          </Button>
          <Button variant="danger" className="flex-1" onClick={onConfirm}>
            <Trash2 size={14} />
            Cancelar Venda
          </Button>
        </div>
      </div>
    </Modal>
  )
}
