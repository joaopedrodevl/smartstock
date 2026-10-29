package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.TagRfid;
import br.com.smartstock.smartstock.core.ports.ITagRfidRepository;
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
public class TagRfidRepository implements ITagRfidRepository {

    private final JdbcTemplate jdbcTemplate;

    public TagRfidRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<TagRfid> ROW_MAPPER = (rs, rowNum) -> {
        TagRfid t = new TagRfid();
        t.setId(rs.getLong("id"));
        t.setPublicId(UUID.fromString(rs.getString("public_id")));
        t.setLojaId(rs.getLong("loja_id"));
        t.setProdutoId(rs.getLong("produto_id"));
        t.setCodigoRfid(rs.getString("codigo_rfid"));
        t.setAtivo(rs.getBoolean("ativo"));
        t.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        t.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());
        return t;
    };

    @Override
    public Optional<TagRfid> findByCodigoRfid(String codigoRfid) {
        String sql = "SELECT * FROM tags_rfid WHERE codigo_rfid = ? AND ativo = TRUE";
        return jdbcTemplate.query(sql, ROW_MAPPER, codigoRfid).stream().findFirst();
    }

    @Override
    public Optional<TagRfid> findByPublicId(UUID publicId) {
        String sql = "SELECT * FROM tags_rfid WHERE public_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, publicId).stream().findFirst();
    }

    @Override
    public List<TagRfid> findByProdutoId(Long produtoId) {
        String sql = "SELECT * FROM tags_rfid WHERE produto_id = ? AND ativo = TRUE";
        return jdbcTemplate.query(sql, ROW_MAPPER, produtoId);
    }

    @Override
    public List<TagRfid> findAtivosByProdutoId(Long produtoId) {
        String sql = "SELECT * FROM tags_rfid WHERE produto_id = ? AND ativo = TRUE ORDER BY criado_em DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, produtoId);
    }

    @Override
    public List<TagRfid> findByLojaId(Long lojaId) {
        String sql = "SELECT * FROM tags_rfid WHERE loja_id = ? AND ativo = TRUE ORDER BY criado_em DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, lojaId);
    }

    @Override
    public Long save(TagRfid tag) {
        String sql = """
            INSERT INTO tags_rfid (public_id, loja_id, produto_id, codigo_rfid, ativo)
            VALUES (?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, tag.getPublicId());
            ps.setLong(2, tag.getLojaId());
            ps.setLong(3, tag.getProdutoId());
            ps.setString(4, tag.getCodigoRfid());
            ps.setBoolean(5, tag.getAtivo() != null ? tag.getAtivo() : true);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void desvincular(String codigoRfid) {
        jdbcTemplate.update("UPDATE tags_rfid SET ativo = FALSE WHERE codigo_rfid = ?", codigoRfid);
    }

    @Override
    public void desvincularTodos(Long produtoId) {
        jdbcTemplate.update("UPDATE tags_rfid SET ativo = FALSE WHERE produto_id = ?", produtoId);
    }

    @Override
    public boolean existsByCodigoRfid(String codigoRfid) {
        String sql = "SELECT COUNT(*) FROM tags_rfid WHERE codigo_rfid = ? AND ativo = TRUE";
        return jdbcTemplate.queryForObject(sql, Integer.class, codigoRfid) > 0;
    }
}
