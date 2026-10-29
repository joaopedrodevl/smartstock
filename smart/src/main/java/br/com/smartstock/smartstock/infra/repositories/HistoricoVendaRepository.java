package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.HistoricoVenda;
import br.com.smartstock.smartstock.core.ports.IHistoricoVendaRepository;
import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

@Repository
public class HistoricoVendaRepository implements IHistoricoVendaRepository {

    private final JdbcTemplate jdbcTemplate;

    public HistoricoVendaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<HistoricoVenda> ROW_MAPPER = (rs, rowNum) -> {
        HistoricoVenda h = new HistoricoVenda();
        h.setId(rs.getLong("id"));
        h.setPublicId(UUID.fromString(rs.getString("public_id")));
        h.setLojaId(rs.getLong("loja_id"));
        h.setProdutoId(rs.getLong("produto_id"));
        h.setCodigoRfid(rs.getString("codigo_rfid"));
        h.setPrecoUnitario(Dinheiro.deCentavos(rs.getLong("preco_unitario")));
        h.setVendidoEm(rs.getTimestamp("vendido_em").toLocalDateTime());
        return h;
    };

    @Override
    public List<HistoricoVenda> findByLojaId(Long lojaId) {
        String sql = "SELECT * FROM historico_vendas WHERE loja_id = ? ORDER BY vendido_em DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, lojaId);
    }

    @Override
    public Long save(HistoricoVenda historico) {
        String sql = """
            INSERT INTO historico_vendas (public_id, loja_id, produto_id, codigo_rfid, preco_unitario, vendido_em)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, historico.getPublicId());
            ps.setLong(2, historico.getLojaId());
            ps.setLong(3, historico.getProdutoId());
            ps.setString(4, historico.getCodigoRfid());
            ps.setLong(5, historico.getPrecoUnitario().getCentavos());
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(historico.getVendidoEm()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public List<HistoricoVenda> saveAll(List<HistoricoVenda> historicos) {
        for (HistoricoVenda h : historicos) {
            save(h);
        }
        return historicos;
    }
}
