package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.application.exceptions.ResourceNotFoundException;
import br.com.smartstock.smartstock.core.entities.HistoricoVenda;
import br.com.smartstock.smartstock.core.entities.Produto;
import br.com.smartstock.smartstock.core.entities.TagRfid;
import br.com.smartstock.smartstock.core.ports.IHistoricoVendaRepository;
import br.com.smartstock.smartstock.core.ports.IProdutoRepository;
import br.com.smartstock.smartstock.core.ports.ITagRfidRepository;
import br.com.smartstock.smartstock.core.ports.ILojaRepository;
import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;
import br.com.smartstock.smartstock.infra.api.dtos.venda.RegistrarVendaRequest;
import br.com.smartstock.smartstock.infra.api.dtos.venda.VendaResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VendaService {

    private static final Logger log = LoggerFactory.getLogger(VendaService.class);

    private final ILojaRepository lojaRepository;
    private final ITagRfidRepository tagRfidRepository;
    private final IProdutoRepository produtoRepository;
    private final IHistoricoVendaRepository historicoVendaRepository;

    public VendaService(ILojaRepository lojaRepository, ITagRfidRepository tagRfidRepository,
                        IProdutoRepository produtoRepository, IHistoricoVendaRepository historicoVendaRepository) {
        this.lojaRepository = lojaRepository;
        this.tagRfidRepository = tagRfidRepository;
        this.produtoRepository = produtoRepository;
        this.historicoVendaRepository = historicoVendaRepository;
    }

    @Transactional
    public VendaResponseDTO registrarVenda(RegistrarVendaRequest dto) {
        log.info("Registrando venda - lojaPublicId: {}, itens: {}", dto.lojaPublicId(), dto.codigosRfid().size());

        var loja = lojaRepository.findByPublicId(dto.lojaPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Loja", dto.lojaPublicId().toString()));

        List<HistoricoVenda> historicos = new ArrayList<>();
        List<VendaResponseDTO.ItemVendaResponseDTO> itensResponse = new ArrayList<>();
        Dinheiro valorTotal = Dinheiro.deCentavos(0);

        for (String codigoRfid : dto.codigosRfid()) {
            TagRfid tag = tagRfidRepository.findByCodigoRfid(codigoRfid)
                    .orElseThrow(() -> new IllegalArgumentException("Tag RFID nao encontrada ou ja vendida: " + codigoRfid));

            Produto produto = produtoRepository.findById(tag.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto associado ao RFID nao encontrado: " + codigoRfid));

            if (!produto.getAtivo()) {
                throw new IllegalArgumentException("Produto inativo: " + produto.getNome());
            }

            HistoricoVenda historico = HistoricoVenda.criar(
                    loja.getId(),
                    produto.getId(),
                    codigoRfid,
                    produto.getPreco()
            );
            historicos.add(historico);

            itensResponse.add(new VendaResponseDTO.ItemVendaResponseDTO(
                    codigoRfid,
                    produto.getNome(),
                    produto.getPreco()
            ));

            valorTotal = valorTotal.somar(produto.getPreco());

            tagRfidRepository.desvincular(codigoRfid);

            produto.decrementarEstoque(1);
            produtoRepository.update(produto);

            log.debug("Item vendido - produto: {}, preco: {}, rfid: {}", produto.getNome(), produto.getPreco(), codigoRfid);
        }

        historicoVendaRepository.saveAll(historicos);

        log.info("Venda registrada com sucesso - valorTotal: {}, itens: {}", valorTotal, historicos.size());

        return new VendaResponseDTO(
                UUID.randomUUID(),
                loja.getPublicId(),
                valorTotal,
                "finalizada",
                null,
                historicos.isEmpty() ? null : historicos.get(0).getVendidoEm(),
                itensResponse
        );
    }

    public List<VendaResponseDTO> listarPorLoja(UUID lojaPublicId) {
        log.info("Listando historico de vendas - lojaPublicId: {}", lojaPublicId);

        var loja = lojaRepository.findByPublicId(lojaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Loja", lojaPublicId.toString()));

        List<HistoricoVenda> historicos = historicoVendaRepository.findByLojaId(loja.getId());

        return historicos.stream()
                .map(h -> {
                    String nomeProduto = produtoRepository.findById(h.getProdutoId())
                            .map(Produto::getNome)
                            .orElse("Produto removido");

                    return new VendaResponseDTO(
                            h.getPublicId(),
                            loja.getPublicId(),
                            h.getPrecoUnitario(),
                            "finalizada",
                            null,
                            h.getVendidoEm(),
                            List.of(new VendaResponseDTO.ItemVendaResponseDTO(
                                    h.getCodigoRfid(),
                                    nomeProduto,
                                    h.getPrecoUnitario()
                            ))
                    );
                })
                .toList();
    }
}
