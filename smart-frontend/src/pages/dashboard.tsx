import { useQuery } from '@tanstack/react-query'
import { Cpu, Package, ShoppingCart, Store } from 'lucide-react'
import { useState } from 'react'
import { Layout } from '@/components/layout/layout'
import { Card, CardContent, CardTitle } from '@/components/ui/card'
import { Loading } from '@/components/ui/loading'
import { PageHeader } from '@/components/ui/page-header'
import { Select } from '@/components/ui/select'
import { api } from '@/lib/api'
import { formatCurrency } from '@/lib/utils'
import type { Loja, PaginatedResponse, Produto, Venda } from '@/types'

interface StatCardProps {
  icon: React.ReactNode
  label: string
  value: string | number
  sublabel?: string
}

function StatCard({ icon, label, value, sublabel }: StatCardProps) {
  return (
    <Card>
      <CardContent className="flex items-start gap-4">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
          {icon}
        </div>
        <div>
          <p className="text-sm text-text-secondary">{label}</p>
          <p className="text-2xl font-bold font-display text-text">{value}</p>
          {sublabel && <p className="text-xs text-text-secondary mt-0.5">{sublabel}</p>}
        </div>
      </CardContent>
    </Card>
  )
}

export function DashboardPage() {
  const [selectedLoja, setSelectedLoja] = useState('all')

  const { data: lojas, isLoading: lojasLoading } = useQuery({
    queryKey: ['lojas'],
    queryFn: () => api.get<PaginatedResponse<Loja>>('/admin/lojas?size=100'),
  })

  const lojaId = selectedLoja || lojas?.content?.[0]?.publicId || ''
  const isAll = selectedLoja === 'all'

  const { data: produtos, isLoading: produtosLoading } = useQuery({
    queryKey: ['produtos', lojaId],
    queryFn: () => api.get<Produto[]>(`/admin/produtos?lojaPublicId=${lojaId}`),
    enabled: !!lojaId && !isAll,
  })

  const { data: vendas, isLoading: vendasLoading } = useQuery({
    queryKey: ['vendas-historico', lojaId],
    queryFn: () => api.get<Venda[]>(`/vendas/historico?lojaPublicId=${lojaId}`),
    enabled: !!lojaId && !isAll,
  })

  const allLojas = lojas?.content ?? []

  const { data: allProdutos, isLoading: allProdutosLoading } = useQuery({
    queryKey: ['all-produtos', allLojas.map((l) => l.publicId).join(',')],
    queryFn: async () => {
      const results = await Promise.all(
        allLojas.map((l) => api.get<Produto[]>(`/admin/produtos?lojaPublicId=${l.publicId}`)),
      )
      return results.flat()
    },
    enabled: isAll && allLojas.length > 0,
  })

  const { data: allVendas, isLoading: allVendasLoading } = useQuery({
    queryKey: ['all-vendas', allLojas.map((l) => l.publicId).join(',')],
    queryFn: async () => {
      const results = await Promise.all(
        allLojas.map((l) => api.get<Venda[]>(`/vendas/historico?lojaPublicId=${l.publicId}`)),
      )
      return results.flat()
    },
    enabled: isAll && allLojas.length > 0,
  })

  const isLoading = lojasLoading || (isAll ? allProdutosLoading || allVendasLoading : produtosLoading || vendasLoading)

  const listaProdutos = isAll ? (allProdutos ?? []) : (produtos ?? [])
  const listaVendas = isAll ? (allVendas ?? []) : (vendas ?? [])

  const totalEstoque = listaProdutos.reduce((acc, p) => acc + p.estoque, 0)
  const finalizadas = listaVendas.filter((v) => v.status === 'finalizada')
  const totalVendasCount = finalizadas.length
  const valorTotalVendas = finalizadas.reduce((acc, v) => acc + v.valorTotal, 0)
  const recentVendas = listaVendas.slice(0, 5)

  const selectedLojaName = isAll
    ? 'Todas as Lojas'
    : lojas?.content?.find((l) => l.publicId === lojaId)?.nomeFantasia

  return (
    <Layout>
      <PageHeader
        title="Dashboard"
        description={selectedLojaName ? `Visão geral — ${selectedLojaName}` : undefined}
        actions={
          lojas && lojas.content.length > 0 ? (
            <Select
              options={[
                { value: 'all', label: 'Todas as Lojas' },
                ...lojas.content.map((l) => ({ value: l.publicId, label: l.nomeFantasia })),
              ]}
              value={selectedLoja}
              onChange={(e) => setSelectedLoja(e.target.value)}
              className="w-56"
            />
          ) : undefined
        }
      />

      {isLoading ? (
        <Loading />
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard icon={<Store size={20} />} label="Lojas" value={lojas?.totalElements ?? 0} />
            <StatCard
              icon={<Package size={20} />}
              label="Produtos"
              value={listaProdutos.length}
              sublabel={`${totalEstoque} em estoque`}
            />
            <StatCard
              icon={<ShoppingCart size={20} />}
              label="Vendas"
              value={totalVendasCount}
              sublabel={`${listaVendas.length} no histórico`}
            />
            <StatCard
              icon={<Cpu size={20} />}
              label="Receita"
              value={formatCurrency(valorTotalVendas)}
            />
          </div>

          <div className="mt-8">
            <Card>
              <CardTitle>Vendas Recentes</CardTitle>
              <CardContent className="mt-4">
                {recentVendas.length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-border text-left text-text-secondary">
                          <th className="pb-2 font-medium">ID</th>
                          <th className="pb-2 font-medium">Status</th>
                          <th className="pb-2 font-medium">Pagamento</th>
                          <th className="pb-2 font-medium text-right">Valor</th>
                        </tr>
                      </thead>
                      <tbody>
                        {recentVendas.map((venda) => (
                          <tr key={venda.publicId} className="border-b border-border/50">
                            <td className="py-2.5 font-mono text-xs">
                              {venda.publicId.slice(0, 8)}
                            </td>
                            <td className="py-2.5">
                              <span
                                className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                                  venda.status === 'finalizada'
                                    ? 'bg-green-50 text-success'
                                    : venda.status === 'cancelada'
                                      ? 'bg-primary-light text-primary'
                                      : 'bg-amber-50 text-accent'
                                }`}
                              >
                                {venda.status}
                              </span>
                            </td>
                            <td className="py-2.5 capitalize">{venda.formaPagamento}</td>
                            <td className="py-2.5 text-right font-medium">
                              {formatCurrency(venda.valorTotal)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="text-sm text-text-secondary py-4 text-center">
                    Nenhuma venda registrada ainda.
                  </p>
                )}
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </Layout>
  )
}
