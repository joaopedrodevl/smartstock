package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.RefreshToken;
import br.com.smartstock.smartstock.core.ports.IRefreshTokenRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RefreshTokenRepository implements IRefreshTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long administradorId, String tokenHash, LocalDateTime expiraEm,
                       String userAgent, String ip) {
        String sql = """
            INSERT INTO refresh_tokens (administrador_id, token_hash, expira_em, user_agent, ip)
            VALUES (?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql, administradorId, tokenHash, expiraEm, userAgent, ip);
    }

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        String sql = "SELECT * FROM refresh_tokens WHERE token_hash = ? AND revogado = FALSE";
        List<RefreshToken> result = jdbcTemplate.query(sql, ROW_MAPPER, tokenHash);
        return result.stream().findFirst();
    }

    public void revoke(String tokenHash) {
        jdbcTemplate.update("UPDATE refresh_tokens SET revogado = TRUE WHERE token_hash = ?", tokenHash);
    }

    private static final RowMapper<RefreshToken> ROW_MAPPER = (rs, rowNum) -> {
        RefreshToken rt = new RefreshToken();
        rt.setId(rs.getLong("id"));
        rt.setAdministradorId(rs.getLong("administrador_id"));
        rt.setTokenHash(rs.getString("token_hash"));
        rt.setExpiraEm(rs.getTimestamp("expira_em").toLocalDateTime());
        rt.setRevogado(rs.getBoolean("revogado"));
        return rt;
    };
}
