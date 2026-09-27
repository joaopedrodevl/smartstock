package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.Admin;
import br.com.smartstock.smartstock.core.ports.IAdminRepository;
import br.com.smartstock.smartstock.core.valueobjects.Email;
import br.com.smartstock.smartstock.core.valueobjects.Nome;
import br.com.smartstock.smartstock.core.valueobjects.Telefone;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdminRepository implements IAdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Admin> ROW_MAPPER = (rs, rowNum) -> {
        Admin a = new Admin();
        a.setId(rs.getLong("id"));
        a.setPublicId(UUID.fromString(rs.getString("public_id")));
        a.setNome(new Nome(rs.getString("nome")));
        a.setTelefone(new Telefone(rs.getString("telefone")));
        a.setEmail(new Email(rs.getString("email")));
        a.setSenhaHash(rs.getString("senha_hash"));
        a.setRole(rs.getString("role"));
        a.setLojaId(rs.getObject("loja_id") != null ? rs.getLong("loja_id") : null);
        a.setAtivo(rs.getBoolean("ativo"));

        Timestamp ultimoLogin = rs.getTimestamp("ultimo_login");
        a.setUltimoLogin(ultimoLogin != null ? ultimoLogin.toLocalDateTime() : null);

        a.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        a.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());

        return a;
    };

    public Optional<Admin> findById(Long id) {
        String sql = "SELECT * FROM administradores WHERE id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    public Optional<Admin> findByPublicId(UUID publicId) {
        String sql = "SELECT * FROM administradores WHERE public_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, publicId).stream().findFirst();
    }

    public Optional<Admin> findByEmail(String email) {
        String sql = "SELECT * FROM administradores WHERE email = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, email).stream().findFirst();
    }

    public List<Admin> findAll() {
        return jdbcTemplate.query("SELECT * FROM administradores", ROW_MAPPER);
    }

    public void save(Admin admin) {
        String sql = """
            INSERT INTO administradores (public_id, nome, telefone, email, senha_hash, role, loja_id, ativo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, admin.getPublicId());
            ps.setString(2, admin.getNome().valor());
            ps.setString(3, admin.getTelefone().valor());
            ps.setString(4, admin.getEmail().valor());
            ps.setString(5, admin.getSenhaHash());
            ps.setString(6, admin.getRole());
            if (admin.getLojaId() != null) {
                ps.setLong(7, admin.getLojaId());
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
            }
            ps.setBoolean(8, admin.getAtivo() != null ? admin.getAtivo() : true);
            return ps;
        });
    }

    public Optional<Admin> findByLojaIdAndRole(Long lojaId, String role) {
        String sql = "SELECT * FROM administradores WHERE loja_id = ? AND role = ? AND ativo = true";
        return jdbcTemplate.query(sql, ROW_MAPPER, lojaId, role).stream().findFirst();
    }

    public void update(Admin admin) {
        String sql = """
            UPDATE administradores
            SET nome = ?, telefone = ?, email = ?, ativo = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, admin.getNome().valor(), admin.getTelefone().valor(),
                admin.getEmail().valor(), admin.getAtivo(), admin.getId());
    }

    public void updateSenha(Long id, String novoSenhaHash) {
        String sql = "UPDATE administradores SET senha_hash = ? WHERE id = ?";
        jdbcTemplate.update(sql, novoSenhaHash, id);
    }

    public void atualizarUltimoLogin(Long id) {
        String sql = "UPDATE administradores SET ultimo_login = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM administradores WHERE id = ?", id);
    }
}
