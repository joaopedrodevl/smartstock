package br.com.smartstock.smartstock.infra.api.dtos.venda;

import java.time.LocalDateTime;

public record ConsultarClienteResponse(
        boolean existe,
        String cpf,
        String telefone,
        String nome,
        LocalDateTime criadoEm
) {
    public static ConsultarClienteResponse naoEncontrado(String cpf) {
        return new ConsultarClienteResponse(false, cpf, null, null, null);
    }

    public static ConsultarClienteResponse encontrado(String cpf, String telefone, String nome, LocalDateTime criadoEm) {
        return new ConsultarClienteResponse(true, cpf, telefone, nome, criadoEm);
    }
}
