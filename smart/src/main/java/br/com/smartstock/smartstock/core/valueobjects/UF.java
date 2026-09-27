package br.com.smartstock.smartstock.core.valueobjects;

import java.util.Objects;

public record UF(String valor) {

    private static final String REGEX = "^[A-Z]{2}$";

    public UF {
        Objects.requireNonNull(valor, "UF não pode ser nula");
        valor = valor.trim().toUpperCase();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("UF não pode ser vazia");
        }
        if (!valor.matches(REGEX)) {
            throw new IllegalArgumentException("UF deve ter 2 letras maiúsculas: " + valor);
        }
    }

    public static UF of(String valor) {
        return new UF(valor);
    }
}
