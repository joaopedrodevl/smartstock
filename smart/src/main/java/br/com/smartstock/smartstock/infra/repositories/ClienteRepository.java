package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.Cliente;
import br.com.smartstock.smartstock.core.ports.IClienteRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class ClienteRepository implements IClienteRepository {

    private final JdbcTemplate jdbcTemplate;

    public ClienteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Cliente> ROW_MAPPER = (rs, rowNum) -> {
        Cliente c = new Cliente();
        c.setId(rs.getLong("id"));
        c.setCpf(rs.getString("cpf"));
        c.setTelefone(rs.getString("telefone"));
        c.setNome(rs.getString("nome"));

        Timestamp criadoEm = rs.getTimestamp("criado_em");
        c.setCriadoEm(criadoEm != null ? criadoEm.toLocalDateTime() : null);

        Timestamp atualizadoEm = rs.getTimestamp("atualizado_em");
        c.setAtualizadoEm(atualizadoEm != null ? atualizadoEm.toLocalDateTime() : null);

        return c;
    };

    @Override
    public Optional<Cliente> findByCpf(String cpf) {
        String sql = "SELECT * FROM clientes WHERE cpf = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, cpf).stream().findFirst();
    }

    @Override
    public Long save(Cliente cliente) {
        String sql = """
            INSERT INTO clientes (cpf, telefone, nome)
            VALUES (?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, cliente.getCpf());
            ps.setString(2, cliente.getTelefone());
            ps.setString(3, cliente.getNome());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public void update(Cliente cliente) {
        String sql = """
            UPDATE clientes
            SET telefone = ?, nome = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, cliente.getTelefone(), cliente.getNome(), cliente.getId());
    }
}
