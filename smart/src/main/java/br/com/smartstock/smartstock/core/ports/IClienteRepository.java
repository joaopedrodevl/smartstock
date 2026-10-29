package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.Cliente;

import java.util.Optional;

public interface IClienteRepository {
    Optional<Cliente> findByCpf(String cpf);
    Long save(Cliente cliente);
    void update(Cliente cliente);
}
