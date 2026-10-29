package br.com.smartstock.smartstock.core.valueobjects;

import java.util.Objects;

public record Telefone(String valor) {

    private static final String REGEX = "^\\(\\d{2}\\)\\s?\\d{4,5}-\\d{4}$";

    public Telefone {
        Objects.requireNonNull(valor, "Telefone não pode ser nulo");
        valor = valor.trim();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("Telefone não pode ser vazio");
        }
        if (!valor.matches(REGEX)) {
            throw new IllegalArgumentException("Telefone inválido (formato esperado: (XX) XXXXX-XXXX): " + valor);
        }
    }

    public static Telefone of(String valor) {
        return new Telefone(valor);
    }
}
