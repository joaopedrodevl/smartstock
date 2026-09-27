package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.Produto;
import br.com.smartstock.smartstock.core.ports.IProdutoRepository;
import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProdutoRepository implements IProdutoRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProdutoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Produto> ROW_MAPPER = (rs, rowNum) -> {
        Produto p = new Produto();
        p.setId(rs.getLong("id"));
        p.setPublicId(UUID.fromString(rs.getString("public_id")));
        p.setLojaId(rs.getLong("loja_id"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNome(rs.getString("nome"));
        p.setDescricao(rs.getString("descricao"));
        p.setPreco(Dinheiro.deCentavos(rs.getLong("preco")));
        p.setEstoque(rs.getInt("estoque"));
        p.setAtivo(rs.getBoolean("ativo"));
        java.sql.Date dataValidade = rs.getDate("data_validade");
        p.setDataValidade(dataValidade != null ? dataValidade.toLocalDate() : null);
        p.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        p.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());
        return p;
    };

    @Override
    public Optional<Produto> findById(Long id) {
        String sql = "SELECT * FROM produtos WHERE id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    @Override
    public Optional<Produto> findByPublicId(UUID publicId) {
        String sql = "SELECT * FROM produtos WHERE public_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, publicId).stream().findFirst();
    }

    @Override
    public List<Produto> findByLojaId(Long lojaId) {
        String sql = "SELECT * FROM produtos WHERE loja_id = ? AND ativo = TRUE ORDER BY criado_em DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, lojaId);
    }

    @Override
    public Long save(Produto produto) {
        String sql = """
            INSERT INTO produtos (public_id, loja_id, codigo_barras, nome, descricao, preco, estoque, ativo, data_validade)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, produto.getPublicId());
            ps.setLong(2, produto.getLojaId());
            ps.setString(3, produto.getCodigoBarras());
            ps.setString(4, produto.getNome());
            ps.setString(5, produto.getDescricao());
            ps.setLong(6, produto.getPreco().getCentavos());
            ps.setInt(7, produto.getEstoque() != null ? produto.getEstoque() : 0);
            ps.setBoolean(8, produto.getAtivo() != null ? produto.getAtivo() : true);
            java.sql.Date dv = produto.getDataValidade() != null ? java.sql.Date.valueOf(produto.getDataValidade()) : null;
            ps.setDate(9, dv);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void update(Produto produto) {
        String sql = """
            UPDATE produtos
            SET nome = ?, descricao = ?, preco = ?, estoque = ?, ativo = ?, data_validade = ?
            WHERE id = ?
            """;
        java.sql.Date dv = produto.getDataValidade() != null ? java.sql.Date.valueOf(produto.getDataValidade()) : null;
        jdbcTemplate.update(sql, produto.getNome(), produto.getDescricao(),
                produto.getPreco().getCentavos(), produto.getEstoque(), produto.getAtivo(), dv, produto.getId());
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        jdbcTemplate.update("DELETE FROM produtos WHERE public_id = ?", publicId);
    }
}
