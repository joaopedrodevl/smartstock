package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.HistoricoVenda;

import java.util.List;

public interface IHistoricoVendaRepository {
    List<HistoricoVenda> findByLojaId(Long lojaId);
    Long save(HistoricoVenda historico);
    List<HistoricoVenda> saveAll(List<HistoricoVenda> historicos);
}
