package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.Loja;
import br.com.smartstock.smartstock.core.ports.ILojaRepository;
import br.com.smartstock.smartstock.core.valueobjects.CNPJ;
import br.com.smartstock.smartstock.core.valueobjects.Nome;
import br.com.smartstock.smartstock.core.valueobjects.RazaoSocial;
import br.com.smartstock.smartstock.core.valueobjects.Telefone;
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
public class LojaRepository implements ILojaRepository {

    private final JdbcTemplate jdbcTemplate;

    public LojaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Loja> ROW_MAPPER = (rs, rowNum) -> {
        Loja l = new Loja();
        l.setId(rs.getLong("id"));
        l.setPublicId(UUID.fromString(rs.getString("public_id")));
        l.setAdministradorId(rs.getLong("administrador_id"));
        l.setEnderecoId(rs.getLong("endereco_id"));
        l.setCnpj(new CNPJ(rs.getString("cnpj")));
        l.setNomeFantasia(new Nome(rs.getString("nome_fantasia")));
        l.setRazaoSocial(new RazaoSocial(rs.getString("razao_social")));
        l.setTelefone(new Telefone(rs.getString("telefone")));
        l.setEhMatriz(rs.getBoolean("eh_matriz"));
        l.setAtivo(rs.getBoolean("ativo"));
        l.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        l.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());
        return l;
    };

    public Long save(Loja loja) {
        String sql = """
            INSERT INTO lojas (public_id, administrador_id, endereco_id, cnpj, nome_fantasia, razao_social, telefone, eh_matriz, ativo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, loja.getPublicId());
            ps.setLong(2, loja.getAdministradorId());
            ps.setLong(3, loja.getEnderecoId());
            ps.setString(4, loja.getCnpj().valor());
            ps.setString(5, loja.getNomeFantasia().valor());
            ps.setString(6, loja.getRazaoSocial().valor());
            ps.setString(7, loja.getTelefone().valor());
            ps.setBoolean(8, loja.getEhMatriz() != null ? loja.getEhMatriz() : false);
            ps.setBoolean(9, loja.getAtivo() != null ? loja.getAtivo() : true);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public Optional<Loja> findById(Long id) {
        String sql = "SELECT * FROM lojas WHERE id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    public Optional<Loja> findByPublicId(UUID publicId) {
        String sql = "SELECT * FROM lojas WHERE public_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, publicId).stream().findFirst();
    }

    public List<Loja> findByAdministradorId(Long administradorId) {
        String sql = "SELECT * FROM lojas WHERE administrador_id = ? ORDER BY criado_em DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, administradorId);
    }

    public List<Loja> findByAdministradorId(Long administradorId, int page, int size) {
        String sql = "SELECT * FROM lojas WHERE administrador_id = ? ORDER BY criado_em DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, administradorId, size, page * size);
    }

    public long countByAdministradorId(Long administradorId) {
        String sql = "SELECT COUNT(*) FROM lojas WHERE administrador_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, administradorId);
    }

    public boolean existsByCnpj(String cnpj) {
        String sql = "SELECT COUNT(*) FROM lojas WHERE cnpj = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, cnpj) > 0;
    }

    public boolean existsByCnpjAndIdNot(String cnpj, Long id) {
        String sql = "SELECT COUNT(*) FROM lojas WHERE cnpj = ? AND id != ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, cnpj, id) > 0;
    }

    public void update(Loja loja) {
        String sql = """
            UPDATE lojas
            SET cnpj = ?, nome_fantasia = ?, razao_social = ?, telefone = ?, eh_matriz = ?, ativo = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, loja.getCnpj().valor(), loja.getNomeFantasia().valor(),
                loja.getRazaoSocial().valor(), loja.getTelefone().valor(),
                loja.getEhMatriz(), loja.getAtivo(), loja.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM lojas WHERE id = ?", id);
    }
}
