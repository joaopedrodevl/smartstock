package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.Venda;
import br.com.smartstock.smartstock.core.enums.FormaPagamento;
import br.com.smartstock.smartstock.core.enums.StatusVenda;
import br.com.smartstock.smartstock.core.ports.IVendaRepository;
import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VendaRepository implements IVendaRepository {

    private final JdbcTemplate jdbcTemplate;

    public VendaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Venda> ROW_MAPPER = (rs, rowNum) -> {
        Venda v = new Venda();
        v.setId(rs.getLong("id"));
        v.setPublicId(rs.getObject("public_id", UUID.class));
        v.setLojaId(rs.getLong("loja_id"));
        v.setClienteId(rs.getLong("cliente_id"));
        v.setValorTotal(Dinheiro.deCentavos(rs.getLong("valor_total")));
        v.setFormaPagamento(FormaPagamento.valueOf(rs.getString("forma_pagamento")));
        v.setStatus(StatusVenda.valueOf(rs.getString("status")));
        v.setObservacao(rs.getString("observacao"));

        Timestamp finalizadaEm = rs.getTimestamp("finalizada_em");
        v.setFinalizadaEm(finalizadaEm != null ? finalizadaEm.toLocalDateTime() : null);

        Timestamp criadoEm = rs.getTimestamp("criado_em");
        v.setCriadoEm(criadoEm != null ? criadoEm.toLocalDateTime() : null);

        Timestamp atualizadoEm = rs.getTimestamp("atualizado_em");
        v.setAtualizadoEm(atualizadoEm != null ? atualizadoEm.toLocalDateTime() : null);

        return v;
    };

    @Override
    public Optional<Venda> findById(Long id) {
        String sql = "SELECT * FROM vendas WHERE id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    @Override
    public Optional<Venda> findByPublicId(UUID publicId) {
        String sql = "SELECT * FROM vendas WHERE public_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, publicId).stream().findFirst();
    }

    @Override
    public Long save(Venda venda) {
        String sql = """
            INSERT INTO vendas (public_id, loja_id, cliente_id, valor_total, forma_pagamento, status, observacao)
            VALUES (?, ?, ?, ?, ?::forma_pagamento_enum, ?::status_venda_enum, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, venda.getPublicId());
            ps.setLong(2, venda.getLojaId());
            ps.setLong(3, venda.getClienteId());
            ps.setLong(4, venda.getValorTotal().getCentavos());
            ps.setString(5, venda.getFormaPagamento().name());
            ps.setString(6, venda.getStatus().name());
            ps.setString(7, venda.getObservacao());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void update(Venda venda) {
        String sql = """
            UPDATE vendas
            SET valor_total = ?, forma_pagamento = ?::forma_pagamento_enum,
                status = ?::status_venda_enum, finalizada_em = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, venda.getValorTotal().getCentavos(), venda.getFormaPagamento().name(),
                venda.getStatus().name(), venda.getFinalizadaEm(), venda.getId());
    }

    @Override
    public List<Venda> findByLojaId(Long lojaId) {
        String sql = "SELECT * FROM vendas WHERE loja_id = ? ORDER BY criado_em DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, lojaId);
    }

    @Override
    public List<Venda> findByClienteId(Long clienteId) {
        String sql = "SELECT * FROM vendas WHERE cliente_id = ? ORDER BY criado_em DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, clienteId);
    }
}
