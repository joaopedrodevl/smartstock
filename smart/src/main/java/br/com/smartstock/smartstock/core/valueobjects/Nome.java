package br.com.smartstock.smartstock.core.valueobjects;

import java.util.Objects;

public record Nome(String valor) {

    public Nome {
        Objects.requireNonNull(valor, "Nome não pode ser nulo");
        valor = valor.trim();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("Nome não pode ser vazio");
        }
        if (valor.length() < 2) {
            throw new IllegalArgumentException("Nome deve ter no mínimo 2 caracteres");
        }
        if (valor.length() > 255) {
            throw new IllegalArgumentException("Nome deve ter no máximo 255 caracteres");
        }
    }

    public static Nome of(String valor) {
        return new Nome(valor);
    }
}
