package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.Embarcado;
import br.com.smartstock.smartstock.core.enums.TipoEmbarcado;
import br.com.smartstock.smartstock.core.ports.IEmbarcadoRepository;
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
public class EmbarcadoRepository implements IEmbarcadoRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmbarcadoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Embarcado> ROW_MAPPER = (rs, rowNum) -> {
        Embarcado e = new Embarcado();
        e.setId(rs.getLong("id"));
        e.setPublicId(UUID.fromString(rs.getString("public_id")));
        e.setLojaId(rs.getLong("loja_id"));
        e.setChavePublica(rs.getString("chave_publica"));
        e.setHmacSecret(rs.getString("hmac_secret"));
        e.setTipo(TipoEmbarcado.valueOf(rs.getString("tipo")));
        e.setDescricao(rs.getString("descricao"));
        e.setAtivo(rs.getBoolean("ativo"));
        e.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        e.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());
        return e;
    };

    @Override
    public Optional<Embarcado> findByChavePublica(String chavePublica) {
        String sql = "SELECT * FROM embarcados WHERE chave_publica = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, chavePublica).stream().findFirst();
    }

    @Override
    public Optional<Embarcado> findByPublicId(UUID publicId) {
        String sql = "SELECT * FROM embarcados WHERE public_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, publicId).stream().findFirst();
    }

    @Override
    public List<Embarcado> findByLojaId(Long lojaId) {
        String sql = "SELECT * FROM embarcados WHERE loja_id = ? ORDER BY criado_em DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, lojaId);
    }

    @Override
    public Long save(Embarcado embarcado) {
        String sql = """
            INSERT INTO embarcados (public_id, loja_id, chave_publica, hmac_secret, tipo, descricao, ativo)
            VALUES (?, ?, ?, ?, ?::tipo_embarcado_enum, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, embarcado.getPublicId());
            ps.setLong(2, embarcado.getLojaId());
            ps.setString(3, embarcado.getChavePublica());
            ps.setString(4, embarcado.getHmacSecret());
            ps.setString(5, embarcado.getTipo().name());
            ps.setString(6, embarcado.getDescricao());
            ps.setBoolean(7, embarcado.getAtivo() != null ? embarcado.getAtivo() : true);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void update(Embarcado embarcado) {
        String sql = """
            UPDATE embarcados
            SET descricao = ?, ativo = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, embarcado.getDescricao(), embarcado.getAtivo(), embarcado.getId());
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        jdbcTemplate.update("DELETE FROM embarcados WHERE public_id = ?", publicId);
    }

    @Override
    public boolean existsByLojaIdAndTipo(Long lojaId, TipoEmbarcado tipo) {
        String sql = "SELECT COUNT(*) FROM embarcados WHERE loja_id = ? AND tipo = ?::tipo_embarcado_enum";
        return jdbcTemplate.queryForObject(sql, Integer.class, lojaId, tipo.name()) > 0;
    }
}
