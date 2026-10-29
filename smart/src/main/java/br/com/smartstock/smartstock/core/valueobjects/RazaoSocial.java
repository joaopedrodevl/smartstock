package br.com.smartstock.smartstock.core.valueobjects;

import java.util.Objects;

public record RazaoSocial(String valor) {

    public RazaoSocial {
        Objects.requireNonNull(valor, "Razão Social não pode ser nula");
        valor = valor.trim();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("Razão Social não pode ser vazia");
        }
        if (valor.length() < 2) {
            throw new IllegalArgumentException("Razão Social deve ter no mínimo 2 caracteres");
        }
        if (valor.length() > 255) {
            throw new IllegalArgumentException("Razão Social deve ter no máximo 255 caracteres");
        }
    }

    public static RazaoSocial of(String valor) {
        return new RazaoSocial(valor);
    }
}
