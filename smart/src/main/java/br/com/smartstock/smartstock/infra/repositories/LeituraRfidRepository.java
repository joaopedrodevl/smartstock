package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.LeituraRfid;
import br.com.smartstock.smartstock.core.enums.StatusLeituraRfid;
import br.com.smartstock.smartstock.core.ports.ILeituraRfidRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;
import java.util.UUID;

@Repository
public class LeituraRfidRepository implements ILeituraRfidRepository {

    private final JdbcTemplate jdbcTemplate;

    public LeituraRfidRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<LeituraRfid> ROW_MAPPER = (rs, rowNum) -> {
        LeituraRfid l = new LeituraRfid();
        l.setId(rs.getLong("id"));
        l.setPublicId(UUID.fromString(rs.getString("public_id")));
        l.setLojaId(rs.getLong("loja_id"));
        l.setEmbarcadoKey(rs.getString("embarcado_key"));
        l.setStatus(StatusLeituraRfid.valueOf(rs.getString("status")));
        l.setCodigoRfid(rs.getString("codigo_rfid"));
        l.setProdutoId(rs.getLong("produto_id") > 0 ? rs.getLong("produto_id") : null);
        l.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        l.setExpiraEm(rs.getTimestamp("expira_em").toLocalDateTime());
        return l;
    };

    @Override
    public Optional<LeituraRfid> findByPublicId(UUID publicId) {
        String sql = "SELECT * FROM leituras_rfid WHERE public_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, publicId).stream().findFirst();
    }

    @Override
    public Optional<LeituraRfid> findPendenteByEmbarcadoKey(String embarcadoKey) {
        String sql = """
            SELECT * FROM leituras_rfid
            WHERE embarcado_key = ? AND status = 'PENDENTE' AND expira_em > NOW()
            ORDER BY criado_em ASC LIMIT 1
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, embarcadoKey).stream().findFirst();
    }

    @Override
    public Long save(LeituraRfid leitura) {
        String sql = """
            INSERT INTO leituras_rfid (public_id, loja_id, embarcado_key, status, codigo_rfid, produto_id, expira_em)
            VALUES (?, ?, ?, ?::status_leitura_rfid_enum, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, leitura.getPublicId());
            ps.setLong(2, leitura.getLojaId());
            ps.setString(3, leitura.getEmbarcadoKey());
            ps.setString(4, leitura.getStatus().name());
            ps.setString(5, leitura.getCodigoRfid());
            if (leitura.getProdutoId() != null) {
                ps.setLong(6, leitura.getProdutoId());
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(leitura.getExpiraEm()));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void updateStatus(UUID publicId, StatusLeituraRfid status, String codigoRfid) {
        String sql = """
            UPDATE leituras_rfid
            SET status = ?::status_leitura_rfid_enum, codigo_rfid = ?
            WHERE public_id = ?
            """;
        jdbcTemplate.update(sql, status.name(), codigoRfid, publicId);
    }
}
