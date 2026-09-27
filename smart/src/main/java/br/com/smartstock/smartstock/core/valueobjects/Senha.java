package br.com.smartstock.smartstock.core.valueobjects;

import java.util.Objects;

public record Senha(String valor) {

    public Senha {
        Objects.requireNonNull(valor, "Senha não pode ser nula");
        if (valor.isBlank()) {
            throw new IllegalArgumentException("Senha não pode ser vazia");
        }
        if (valor.length() < 8) {
            throw new IllegalArgumentException("Senha deve ter no mínimo 8 caracteres");
        }
        if (valor.length() > 128) {
            throw new IllegalArgumentException("Senha deve ter no máximo 128 caracteres");
        }
    }

    public static Senha of(String valor) {
        return new Senha(valor);
    }
}
