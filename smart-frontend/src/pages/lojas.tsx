import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, Pencil, Plus, Power, Search, Store, Trash2, UserCog } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/layout/layout'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { Input } from '@/components/ui/input'
import { Loading } from '@/components/ui/loading'
import { Modal } from '@/components/ui/modal'
import { PageHeader } from '@/components/ui/page-header'
import { Select } from '@/components/ui/select'
import { ApiError } from '@/lib/api'
import { formatCnpj, formatPhone, maskCep, maskCnpj, maskPhone } from '@/lib/utils'
import { lojasService, type VendedorLoja } from '@/services/lojas'
import type { Loja } from '@/types'

export function LojasPage() {
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [editingLoja, setEditingLoja] = useState<Loja | null>(null)
  const [confirmDelete, setConfirmDelete] = useState<Loja | null>(null)
  const [vendedorModal, setVendedorModal] = useState<Loja | null>(null)
  const [search, setSearch] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['lojas'],
    queryFn: () => lojasService.list(0, 100),
  })

  const filteredLojas = useMemo(() => {
    if (!data?.content) return []
    if (!search.trim()) return data.content
    const term = search.toLowerCase()
    return data.content.filter(
      (loja) =>
        loja.nomeFantasia.toLowerCase().includes(term) ||
        loja.razaoSocial.toLowerCase().includes(term),
    )
  }, [data?.content, search])

  const deleteMutation = useMutation({
    mutationFn: lojasService.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lojas'] })
      setConfirmDelete(null)
    },
  })

  function handleEdit(loja: Loja) {
    setEditingLoja(loja)
    setModalOpen(true)
  }

  function handleNew() {
    setEditingLoja(null)
    setModalOpen(true)
  }

  return (
    <Layout>
      <PageHeader
        title="Lojas"
        description="Gerencie suas lojas e endereços"
        actions={
          <Button onClick={handleNew} size="sm">
            <Plus size={16} />
            Nova Loja
          </Button>
        }
      />

      {isLoading ? (
        <Loading />
      ) : !data?.content?.length ? (
        <EmptyState
          icon={<Store size={40} />}
          title="Nenhuma loja cadastrada"
          description="Crie sua primeira loja para começar a gerenciar produtos e dispositivos."
          action={
            <Button onClick={handleNew} size="sm">
              <Plus size={16} />
              Criar Loja
            </Button>
          }
        />
      ) : (
        <>
          <div className="mb-4">
            <div className="relative max-w-sm">
              <Search
                size={16}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary/60"
              />
              <input
                type="text"
                placeholder="Buscar loja..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="h-10 w-full rounded-md border border-border bg-white pl-9 pr-3 text-sm text-text placeholder:text-text-secondary/60 focus:outline-2 focus:outline-primary/30 focus:border-primary"
              />
            </div>
          </div>

          {filteredLojas.length === 0 ? (
            <EmptyState
              icon={<Store size={40} />}
              title="Nenhuma loja encontrada"
              description="Nenhuma loja corresponde ao filtro informado."
            />
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {filteredLojas.map((loja) => (
                <Card key={loja.publicId}>
                  <CardContent className="flex flex-col gap-3">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="font-semibold font-display text-text">
                          {loja.nomeFantasia}
                        </h3>
                        <p className="text-xs text-text-secondary mt-0.5">{loja.razaoSocial}</p>
                      </div>
                      {loja.ehMatriz && <Badge variant="info">Matriz</Badge>}
                    </div>

                    <div className="text-sm text-text-secondary space-y-1">
                      <p>CNPJ: {formatCnpj(loja.cnpj)}</p>
                      <p>Tel: {formatPhone(loja.telefone)}</p>
                      {loja.endereco && (
                        <p>
                          {loja.endereco.logradouro}, {loja.endereco.numero} —{' '}
                          {loja.endereco.cidade}/{loja.endereco.estado}
                        </p>
                      )}
                    </div>

                    <div className="flex flex-wrap gap-2 mt-2">
                      <Button
                        variant="secondary"
                        size="sm"
                        className="border-blue-200 text-blue-700 hover:bg-blue-50 hover:text-blue-800"
                        onClick={() => handleEdit(loja)}
                      >
                        <Pencil size={14} />
                        Editar
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        className="border-purple-200 text-purple-700 hover:bg-purple-50 hover:text-purple-800"
                        onClick={() => setVendedorModal(loja)}
                      >
                        <UserCog size={14} />
                        Vendedor
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        className="border-red-200 text-red-700 hover:bg-red-50 hover:text-red-800"
                        onClick={() => setConfirmDelete(loja)}
                      >
                        <Trash2 size={14} />
                        Desativar
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </>
      )}

      <LojaModal open={modalOpen} onClose={() => setModalOpen(false)} loja={editingLoja} />

      {vendedorModal && (
        <VendedorModal loja={vendedorModal} onClose={() => setVendedorModal(null)} />
      )}

      <ConfirmDeleteModal
        open={!!confirmDelete}
        lojaNome={confirmDelete?.nomeFantasia ?? ''}
        onConfirm={() => {
          if (confirmDelete) deleteMutation.mutate(confirmDelete.publicId)
        }}
        onCancel={() => setConfirmDelete(null)}
        loading={deleteMutation.isPending}
      />
    </Layout>
  )
}

function ConfirmDeleteModal({
  open,
  lojaNome,
  onConfirm,
  onCancel,
  loading,
}: {
  open: boolean
  lojaNome: string
  onConfirm: () => void
  onCancel: () => void
  loading: boolean
}) {
  return (
    <Modal open={open} onClose={onCancel} title="Confirmar desativação">
      <div className="flex flex-col items-center gap-4 py-2">
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
          <AlertTriangle size={28} className="text-red-600" />
        </div>
        <p className="text-center text-sm text-text">
          Deseja realmente desativar a loja <span className="font-semibold">{lojaNome}</span>?
        </p>
        <p className="text-xs text-text-secondary text-center">
          A loja não será removida, apenas desativada do sistema.
        </p>
        <div className="flex w-full gap-2 mt-2">
          <Button type="button" variant="secondary" className="flex-1" onClick={onCancel}>
            Cancelar
          </Button>
          <Button
            type="button"
            variant="danger"
            className="flex-1"
            loading={loading}
            onClick={onConfirm}
          >
            <Power size={14} />
            Desativar
          </Button>
        </div>
      </div>
    </Modal>
  )
}

function LojaModal({
  open,
  onClose,
  loja,
}: {
  open: boolean
  onClose: () => void
  loja: Loja | null
}) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    nomeFantasia: '',
    razaoSocial: '',
    cnpj: '',
    telefone: '',
    ehMatriz: false,
    cep: '',
    logradouro: '',
    numero: '',
    bairro: '',
    cidade: '',
    estado: '',
  })

  useEffect(() => {
    if (open) {
      setForm({
        nomeFantasia: loja?.nomeFantasia ?? '',
        razaoSocial: loja?.razaoSocial ?? '',
        cnpj: loja?.cnpj ?? '',
        telefone: loja?.telefone ?? '',
        ehMatriz: loja?.ehMatriz ?? false,
        cep: loja?.endereco?.cep ?? '',
        logradouro: loja?.endereco?.logradouro ?? '',
        numero: loja?.endereco?.numero ?? '',
        bairro: loja?.endereco?.bairro ?? '',
        cidade: loja?.endereco?.cidade ?? '',
        estado: loja?.endereco?.estado ?? '',
      })
    }
  }, [open, loja])

  const mutation = useMutation({
    mutationFn: (data: typeof form) => {
      const payload = {
        nomeFantasia: data.nomeFantasia,
        razaoSocial: data.razaoSocial,
        cnpj: data.cnpj.replace(/[^A-Za-z0-9]/g, ''),
        telefone: data.telefone,
        ehMatriz: data.ehMatriz,
        endereco: {
          cep: data.cep.replace(/\D/g, ''),
          logradouro: data.logradouro,
          numero: data.numero,
          bairro: data.bairro,
          cidade: data.cidade,
          estado: data.estado,
        },
      }
      if (loja) return lojasService.update(loja.publicId, payload)
      return lojasService.create(payload)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lojas'] })
      onClose()
    },
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    mutation.mutate(form)
  }

  return (
    <Modal open={open} onClose={onClose} title={loja ? 'Editar Loja' : 'Nova Loja'}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <Input
          label="Nome Fantasia"
          value={form.nomeFantasia}
          onChange={(e) => setForm({ ...form, nomeFantasia: e.target.value })}
          placeholder="Ex: SmartStore Centro"
          required
        />
        <Input
          label="Razão Social"
          value={form.razaoSocial}
          onChange={(e) => setForm({ ...form, razaoSocial: e.target.value })}
          placeholder="Ex: SmartStore LTDA"
          required
        />
        <div className="grid grid-cols-2 gap-3">
          <Input
            label="CNPJ"
            value={form.cnpj}
            onChange={(e) => setForm({ ...form, cnpj: maskCnpj(e.target.value) })}
            placeholder="00.000.000/0000-00"
            required
          />
          <Input
            label="Telefone"
            value={form.telefone}
            onChange={(e) => setForm({ ...form, telefone: maskPhone(e.target.value) })}
            placeholder="(00) 00000-0000"
            required
          />
        </div>
        <Input
          label="CEP"
          value={form.cep}
          onChange={(e) => setForm({ ...form, cep: maskCep(e.target.value) })}
          placeholder="00000-000"
          required
        />
        <Input
          label="Logradouro"
          value={form.logradouro}
          onChange={(e) => setForm({ ...form, logradouro: e.target.value })}
          placeholder="Ex: Rua das Flores"
          required
        />
        <div className="grid grid-cols-3 gap-3">
          <Input
            label="Número"
            value={form.numero}
            onChange={(e) => setForm({ ...form, numero: e.target.value })}
            placeholder="Ex: 123"
            required
          />
          <Input
            label="Bairro"
            value={form.bairro}
            onChange={(e) => setForm({ ...form, bairro: e.target.value })}
            placeholder="Ex: Centro"
            required
          />
          <Input
            label="Cidade"
            value={form.cidade}
            onChange={(e) => setForm({ ...form, cidade: e.target.value })}
            placeholder="Ex: João Pessoa"
            required
          />
        </div>
        <Select
          label="Estado (UF)"
          value={form.estado}
          onChange={(e) => setForm({ ...form, estado: e.target.value })}
          options={[
            { value: 'AC', label: 'Acre' },
            { value: 'AL', label: 'Alagoas' },
            { value: 'AP', label: 'Amapá' },
            { value: 'AM', label: 'Amazonas' },
            { value: 'BA', label: 'Bahia' },
            { value: 'CE', label: 'Ceará' },
            { value: 'DF', label: 'Distrito Federal' },
            { value: 'ES', label: 'Espírito Santo' },
            { value: 'GO', label: 'Goiás' },
            { value: 'MA', label: 'Maranhão' },
            { value: 'MT', label: 'Mato Grosso' },
            { value: 'MS', label: 'Mato Grosso do Sul' },
            { value: 'MG', label: 'Minas Gerais' },
            { value: 'PA', label: 'Pará' },
            { value: 'PB', label: 'Paraíba' },
            { value: 'PR', label: 'Paraná' },
            { value: 'PE', label: 'Pernambuco' },
            { value: 'PI', label: 'Piauí' },
            { value: 'RJ', label: 'Rio de Janeiro' },
            { value: 'RN', label: 'Rio Grande do Norte' },
            { value: 'RS', label: 'Rio Grande do Sul' },
            { value: 'RO', label: 'Rondônia' },
            { value: 'RR', label: 'Roraima' },
            { value: 'SC', label: 'Santa Catarina' },
            { value: 'SP', label: 'São Paulo' },
            { value: 'SE', label: 'Sergipe' },
            { value: 'TO', label: 'Tocantins' },
          ]}
          required
        />

        {mutation.isError && <p className="text-sm text-error">{mutation.error.message}</p>}

        <div className="flex justify-end gap-2 mt-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" loading={mutation.isPending}>
            {loja ? 'Salvar' : 'Criar'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function VendedorModal({ loja, onClose }: { loja: Loja; onClose: () => void }) {
  const [vendedor, setVendedor] = useState<VendedorLoja | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [form, setForm] = useState({ nome: '', telefone: '', email: '', senha: '' })

  useEffect(() => {
    async function carregar() {
      try {
        const data = await lojasService.getVendaAdmin(loja.publicId)
        setVendedor(data)
      } catch (error) {
        if (!(error instanceof ApiError && error.status === 404)) {
          setVendedor(null)
        }
      } finally {
        setCarregando(false)
      }
    }
    carregar()
  }, [loja.publicId])

  const criarMutation = useMutation({
    mutationFn: () =>
      lojasService.createVendaAdmin({
        lojaPublicId: loja.publicId,
        nome: form.nome,
        telefone: form.telefone,
        email: form.email,
        senha: form.senha,
      }),
    onSuccess: (data) => {
      setVendedor(data)
      setForm({ nome: '', telefone: '', email: '', senha: '' })
    },
  })

  return (
    <Modal open onClose={onClose} title={`Vendedor — ${loja.nomeFantasia}`}>
      <div className="flex flex-col gap-4">
        {carregando ? (
          <Loading />
        ) : vendedor ? (
          <div className="rounded-md border border-border p-3 flex flex-col gap-1">
            <p className="text-sm">
              <span className="text-text-secondary">Vendedor cadastrado:</span>{' '}
              <span className="font-medium">{vendedor.email}</span>
            </p>
            <p className="text-sm">
              <span className="text-text-secondary">Nome:</span>{' '}
              <span className="font-medium">{vendedor.nome}</span>
            </p>
            <p className="text-sm">
              <span className="text-text-secondary">Telefone:</span>{' '}
              <span className="font-medium">{formatPhone(vendedor.telefone)}</span>
            </p>
            <p className="text-xs text-text-secondary mt-1">
              Ele tem acesso apenas ao Terminal de Vendas desta loja.
            </p>
          </div>
        ) : (
          <form
            onSubmit={(e) => {
              e.preventDefault()
              criarMutation.mutate()
            }}
            className="flex flex-col gap-3"
          >
            <p className="text-sm text-text-secondary">
              Crie um vendedor exclusivo desta loja. Ele terá acesso apenas ao Terminal de Vendas.
            </p>
            <Input
              label="Nome"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              placeholder="Ex: João Vendedor"
              required
            />
            <Input
              label="Telefone"
              value={form.telefone}
              onChange={(e) => setForm({ ...form, telefone: maskPhone(e.target.value) })}
              placeholder="(00) 00000-0000"
              required
            />
            <Input
              label="Email"
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              placeholder="vendedor@email.com"
              required
            />
            <Input
              label="Senha"
              type="password"
              value={form.senha}
              onChange={(e) => setForm({ ...form, senha: e.target.value })}
              placeholder="Mínimo 8 caracteres"
              minLength={8}
              required
            />

            {criarMutation.isError && (
              <p className="text-sm text-error">{criarMutation.error.message}</p>
            )}

            <div className="flex gap-2 mt-2">
              <Button type="button" variant="secondary" className="flex-1" onClick={onClose}>
                Cancelar
              </Button>
              <Button type="submit" className="flex-1" loading={criarMutation.isPending}>
                Criar Vendedor
              </Button>
            </div>
          </form>
        )}
      </div>
    </Modal>
  )
}
