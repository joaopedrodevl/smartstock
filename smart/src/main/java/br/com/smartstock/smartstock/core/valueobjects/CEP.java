package br.com.smartstock.smartstock.core.valueobjects;

import java.util.Objects;

public record CEP(String valor) {

    private static final String REGEX = "^\\d{8}$";

    public CEP {
        Objects.requireNonNull(valor, "CEP não pode ser nulo");
        String somenteDigitos = valor.replaceAll("[^0-9]", "");
        if (somenteDigitos.isBlank()) {
            throw new IllegalArgumentException("CEP não pode ser vazio");
        }
        if (!somenteDigitos.matches(REGEX)) {
            throw new IllegalArgumentException("CEP deve ter 8 dígitos: " + valor);
        }
    }

    public static CEP of(String valor) {
        return new CEP(valor);
    }
}
