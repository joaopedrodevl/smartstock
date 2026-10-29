import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, Cpu, Plus, Search, Store, Trash2 } from 'lucide-react'
import { useMemo, useState } from 'react'
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
import { embarcadosService } from '@/services/embarcados'
import { lojasService } from '@/services/lojas'
import type { Loja } from '@/types'

function StatusDot({ online }: { online: boolean }) {
  return (
    <span
      className={`inline-block h-2.5 w-2.5 rounded-full shrink-0 ${
        online ? 'bg-green-500 shadow-[0_0_6px_rgba(34,197,94,0.5)]' : 'bg-red-400'
      }`}
      title={online ? 'Online' : 'Offline'}
    />
  )
}

export function EmbarcadosPage() {
  const queryClient = useQueryClient()
  const [addModal, setAddModal] = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState<{ publicId: string; descricao: string } | null>(
    null,
  )
  const [search, setSearch] = useState('')

  const { data: lojas, isLoading: lojasLoading } = useQuery({
    queryKey: ['lojas'],
    queryFn: () => lojasService.list(0, 100),
  })

  const filteredLojas = useMemo(() => {
    if (!lojas?.content) return []
    if (!search.trim()) return lojas.content
    const term = search.toLowerCase()
    return lojas.content.filter(
      (loja) =>
        loja.nomeFantasia.toLowerCase().includes(term) ||
        loja.razaoSocial.toLowerCase().includes(term),
    )
  }, [lojas?.content, search])

  const deleteMutation = useMutation({
    mutationFn: embarcadosService.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['embarcados'] })
      queryClient.invalidateQueries({ queryKey: ['embarcados-status'] })
      setConfirmDelete(null)
    },
  })

  if (lojasLoading) {
    return (
      <Layout>
        <PageHeader title="Embarcados" description="Dispositivos ESP32 conectados ao sistema" />
        <Loading />
      </Layout>
    )
  }

  if (!lojas?.content?.length) {
    return (
      <Layout>
        <PageHeader title="Embarcados" description="Dispositivos ESP32 conectados ao sistema" />
        <EmptyState
          icon={<Store size={40} />}
          title="Nenhuma loja cadastrada"
          description="Cadastre uma loja primeiro para gerenciar dispositivos."
        />
      </Layout>
    )
  }

  return (
    <Layout>
      <PageHeader
        title="Embarcados"
        description="Dispositivos ESP32 conectados ao sistema"
      />

      <div className="mb-4">
        <div className="relative max-w-sm">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary/60" />
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
            <LojaCard
              key={loja.publicId}
              loja={loja}
              onAdd={() => setAddModal(loja.publicId)}
              onDelete={(publicId, descricao) => setConfirmDelete({ publicId, descricao })}
            />
          ))}
        </div>
      )}

      {addModal && (
        <EmbarcadoModal
          open={!!addModal}
          onClose={() => setAddModal(null)}
          lojaId={addModal}
        />
      )}

      <ConfirmDeleteModal
        open={!!confirmDelete}
        deviceName={confirmDelete?.descricao ?? ''}
        onConfirm={() => {
          if (confirmDelete) deleteMutation.mutate(confirmDelete.publicId)
        }}
        onCancel={() => setConfirmDelete(null)}
        loading={deleteMutation.isPending}
      />
    </Layout>
  )
}

function LojaCard({
  loja,
  onAdd,
  onDelete,
}: {
  loja: Loja
  onAdd: () => void
  onDelete: (publicId: string, descricao: string) => void
}) {
  const { data: embarcados } = useQuery({
    queryKey: ['embarcados', loja.publicId],
    queryFn: () => embarcadosService.list(loja.publicId),
  })

  const { data: statusMap } = useQuery({
    queryKey: ['embarcados-status', loja.publicId],
    queryFn: () => embarcadosService.getStatus(loja.publicId),
    refetchInterval: 5000,
  })

  const devices = embarcados ?? []
  const maxDevices = 2
  const onlineCount = devices.filter(
    (e) => statusMap?.[e.chavePublica] ?? false,
  ).length

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-primary/10">
            <Store size={18} className="text-primary" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold font-display text-text truncate">{loja.nomeFantasia}</h3>
            <p className="text-xs text-text-secondary truncate">{loja.razaoSocial}</p>
          </div>
        </div>

        <div className="flex items-center gap-4 border-t border-border pt-3 text-xs text-text-secondary">
          <span>
            Online:{' '}
            <span className={`font-medium ${onlineCount > 0 ? 'text-green-600' : 'text-text-secondary'}`}>
              {onlineCount}
            </span>
          </span>
          <span>
            Total:{' '}
            <span className="font-medium text-text">{devices.length}</span>/{maxDevices}
          </span>
        </div>

        {devices.length > 0 && (
          <div className="flex flex-col gap-2">
            {devices.map((device) => {
              const isOnline = statusMap?.[device.chavePublica] ?? false
              return (
                <div
                  key={device.publicId}
                  className="flex items-center justify-between rounded-md border border-border/60 px-3 py-2"
                >
                  <div className="flex items-center gap-2 min-w-0">
                    <StatusDot online={isOnline} />
                    <span className="text-sm font-medium text-text truncate">
                      {device.descricao || 'Sem descrição'}
                    </span>
                  </div>
                  <div className="flex items-center gap-1.5 shrink-0">
                    <Badge variant={isOnline ? 'success' : 'default'}>
                      {isOnline ? 'Online' : 'Offline'}
                    </Badge>
                    <Badge variant={device.tipo === 'VENDA' ? 'info' : 'default'}>
                      {device.tipo}
                    </Badge>
                    <button
                      type="button"
                      className="rounded p-1 text-text-secondary hover:bg-red-50 hover:text-red-600 transition-colors"
                      title="Remover"
                      onClick={() => onDelete(device.publicId, device.descricao || 'Sem descrição')}
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {devices.length === 0 && (
          <p className="text-xs text-text-secondary text-center py-1">Nenhum dispositivo cadastrado</p>
        )}

        {devices.length < maxDevices && (
          <Button variant="secondary" size="sm" className="w-full" onClick={onAdd}>
            <Plus size={14} />
            Adicionar Device
          </Button>
        )}
      </CardContent>
    </Card>
  )
}

function EmbarcadoModal({
  open,
  onClose,
  lojaId,
}: {
  open: boolean
  onClose: () => void
  lojaId: string
}) {
  const queryClient = useQueryClient()
  const [tipo, setTipo] = useState<'VENDA' | 'CADASTRO'>('VENDA')
  const [descricao, setDescricao] = useState('')
  const [chaveGerada, setChaveGerada] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () =>
      embarcadosService.create({
        lojaPublicId: lojaId,
        tipo,
        descricao,
      }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['embarcados', lojaId] })
      queryClient.invalidateQueries({ queryKey: ['embarcados-status', lojaId] })
      setChaveGerada(data.chavePublica)
    },
  })

  function handleClose() {
    setChaveGerada(null)
    setTipo('VENDA')
    setDescricao('')
    onClose()
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    mutation.mutate()
  }

  if (chaveGerada) {
    return (
      <Modal open={open} onClose={handleClose} title="Dispositivo Cadastrado">
        <div className="flex flex-col items-center gap-4 py-2">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-green-50">
            <Cpu size={28} className="text-green-600" />
          </div>
          <p className="text-center text-sm text-text">
            Dispositivo cadastrado com sucesso! Programe o ESP32 com a chave abaixo:
          </p>
          <div className="w-full rounded-md border border-border bg-surface p-3">
            <p className="text-xs text-text-secondary mb-1">Chave Pública (UUID)</p>
            <p className="font-mono text-sm text-text break-all select-all">{chaveGerada}</p>
          </div>
          <p className="text-xs text-text-secondary text-center">
            Copie essa chave e coloque no código do ESP32. Ela não será exibida novamente.
          </p>
          <Button className="w-full" onClick={handleClose}>
            Fechar
          </Button>
        </div>
      </Modal>
    )
  }

  return (
    <Modal open={open} onClose={handleClose} title="Novo Dispositivo">
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <Select
          label="Tipo"
          value={tipo}
          onChange={(e) => setTipo(e.target.value as 'VENDA' | 'CADASTRO')}
          options={[
            { value: 'VENDA', label: 'Venda — Leitura de produtos para vendas' },
            { value: 'CADASTRO', label: 'Cadastro — Leitura para registrar produtos' },
          ]}
        />
        <Input
          label="Descrição"
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          placeholder="Ex: Device entrada principal"
          required
        />

        {mutation.isError && <p className="text-sm text-error">{mutation.error.message}</p>}

        <div className="flex justify-end gap-2 mt-2">
          <Button type="button" variant="secondary" onClick={handleClose}>
            Cancelar
          </Button>
          <Button type="submit" loading={mutation.isPending}>
            Cadastrar
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function ConfirmDeleteModal({
  open,
  deviceName,
  onConfirm,
  onCancel,
  loading,
}: {
  open: boolean
  deviceName: string
  onConfirm: () => void
  onCancel: () => void
  loading: boolean
}) {
  return (
    <Modal open={open} onClose={onCancel} title="Confirmar remoção">
      <div className="flex flex-col items-center gap-4 py-2">
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-50">
          <AlertTriangle size={28} className="text-red-600" />
        </div>
        <p className="text-center text-sm text-text">
          Deseja realmente remover o dispositivo{' '}
          <span className="font-semibold">{deviceName}</span>?
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
            <Trash2 size={14} />
            Remover
          </Button>
        </div>
      </div>
    </Modal>
  )
}
