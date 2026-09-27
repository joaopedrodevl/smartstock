package br.com.smartstock.smartstock.core.valueobjects;

import java.util.Objects;

public record Email(String valor) {

    private static final String REGEX = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

    public Email {
        Objects.requireNonNull(valor, "Email não pode ser nulo");
        valor = valor.trim();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("Email não pode ser vazio");
        }
        if (valor.length() > 255) {
            throw new IllegalArgumentException("Email deve ter no máximo 255 caracteres");
        }
        if (!valor.matches(REGEX)) {
            throw new IllegalArgumentException("Email inválido: " + valor);
        }
    }

    public static Email of(String valor) {
        return new Email(valor);
    }
}
