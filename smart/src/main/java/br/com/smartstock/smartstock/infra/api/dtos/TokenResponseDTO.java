package br.com.smartstock.smartstock.infra.api.dtos;

public record TokenResponseDTO(
        String token,
        String refreshToken
) {
}
