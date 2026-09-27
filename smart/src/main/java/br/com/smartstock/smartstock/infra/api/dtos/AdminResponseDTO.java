package br.com.smartstock.smartstock.infra.api.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminResponseDTO(
        UUID publicId,
        String nome,
        String telefone,
        String email,
        Boolean ativo,
        LocalDateTime ultimoLogin,
        LocalDateTime criadoEm
) {
    public static AdminResponseDTO from(br.com.smartstock.smartstock.core.entities.Admin admin) {
        return new AdminResponseDTO(
                admin.getPublicId(),
                admin.getNome().valor(),
                admin.getTelefone().valor(),
                admin.getEmail().valor(),
                admin.getAtivo(),
                admin.getUltimoLogin(),
                admin.getCriadoEm()
        );
    }
}
