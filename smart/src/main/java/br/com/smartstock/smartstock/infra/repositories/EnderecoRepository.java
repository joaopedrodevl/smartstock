package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.Endereco;
import br.com.smartstock.smartstock.core.ports.IEnderecoRepository;
import br.com.smartstock.smartstock.core.valueobjects.CEP;
import br.com.smartstock.smartstock.core.valueobjects.UF;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EnderecoRepository implements IEnderecoRepository {

    private final JdbcTemplate jdbcTemplate;

    public EnderecoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Endereco> ROW_MAPPER = (rs, rowNum) -> {
        Endereco e = new Endereco();
        e.setId(rs.getLong("id"));
        e.setPublicId(UUID.fromString(rs.getString("public_id")));
        e.setCep(new CEP(rs.getString("cep")));
        e.setLogradouro(rs.getString("logradouro"));
        e.setNumero(rs.getString("numero"));
        e.setComplemento(rs.getString("complemento"));
        e.setBairro(rs.getString("bairro"));
        e.setCidade(rs.getString("cidade"));
        e.setEstado(new UF(rs.getString("estado")));
        e.setReferencia(rs.getString("referencia"));
        e.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        e.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());
        return e;
    };

    public Long save(Endereco endereco) {
        String sql = """
            INSERT INTO enderecos (public_id, cep, logradouro, numero, complemento, bairro, cidade, estado, referencia)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, endereco.getPublicId());
            ps.setString(2, endereco.getCep().valor());
            ps.setString(3, endereco.getLogradouro());
            ps.setString(4, endereco.getNumero());
            ps.setString(5, endereco.getComplemento());
            ps.setString(6, endereco.getBairro());
            ps.setString(7, endereco.getCidade());
            ps.setString(8, endereco.getEstado().valor());
            ps.setString(9, endereco.getReferencia());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public Optional<Endereco> findById(Long id) {
        String sql = "SELECT * FROM enderecos WHERE id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    public Optional<Endereco> findByPublicId(UUID publicId) {
        String sql = "SELECT * FROM enderecos WHERE public_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, publicId).stream().findFirst();
    }

    public void update(Endereco endereco) {
        String sql = """
            UPDATE enderecos
            SET cep = ?, logradouro = ?, numero = ?, complemento = ?, bairro = ?, cidade = ?, estado = ?, referencia = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, endereco.getCep().valor(), endereco.getLogradouro(), endereco.getNumero(),
                endereco.getComplemento(), endereco.getBairro(), endereco.getCidade(),
                endereco.getEstado().valor(), endereco.getReferencia(), endereco.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM enderecos WHERE id = ?", id);
    }
}
