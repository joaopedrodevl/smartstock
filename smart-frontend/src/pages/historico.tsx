import { useQuery } from '@tanstack/react-query'
import { History } from 'lucide-react'
import { useState } from 'react'
import { Layout } from '@/components/layout/layout'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { Loading } from '@/components/ui/loading'
import { PageHeader } from '@/components/ui/page-header'
import { Select } from '@/components/ui/select'
import { formatCurrency, formatDateTime } from '@/lib/utils'
import { lojasService } from '@/services/lojas'
import { vendasService } from '@/services/vendas'

export function HistoricoVendasPage() {
  const [selectedLoja, setSelectedLoja] = useState<string>('')

  const { data: lojas } = useQuery({
    queryKey: ['lojas'],
    queryFn: () => lojasService.list(0, 100),
  })

  const lojaId = selectedLoja || lojas?.content?.[0]?.publicId || ''

  const { data: vendas, isLoading } = useQuery({
    queryKey: ['vendas-historico', lojaId],
    queryFn: () => vendasService.list(lojaId),
    enabled: !!lojaId,
  })

  return (
    <Layout>
      <PageHeader
        title="Histórico de Vendas"
        description="Vendas realizadas recentemente"
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

      {isLoading ? (
        <Loading />
      ) : !vendas?.length ? (
        <EmptyState
          icon={<History size={40} />}
          title="Nenhuma venda registrada"
          description="As vendas realizadas aparecerão aqui."
        />
      ) : (
        <Card>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-text-secondary">
                    <th className="pb-2 font-medium">Data</th>
                    <th className="pb-2 font-medium">ID</th>
                    <th className="pb-2 font-medium">Status</th>
                    <th className="pb-2 font-medium">Pagamento</th>
                    <th className="pb-2 font-medium text-right">Valor</th>
                  </tr>
                </thead>
                <tbody>
                  {vendas.map((venda) => (
                    <tr key={venda.publicId} className="border-b border-border/50">
                      <td className="py-3 text-text-secondary">
                        {venda.criadoEm ? formatDateTime(venda.criadoEm) : '—'}
                      </td>
                      <td className="py-3 font-mono text-xs">{venda.publicId.slice(0, 8)}</td>
                      <td className="py-3">
                        <Badge
                          variant={
                            venda.status === 'finalizada'
                              ? 'success'
                              : venda.status === 'cancelada'
                                ? 'danger'
                                : 'warning'
                          }
                        >
                          {venda.status}
                        </Badge>
                      </td>
                      <td className="py-3 capitalize">{venda.formaPagamento || '—'}</td>
                      <td className="py-3 text-right font-medium">
                        {formatCurrency(venda.valorTotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}
    </Layout>
  )
}
