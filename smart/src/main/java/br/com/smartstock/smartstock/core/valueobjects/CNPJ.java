package br.com.smartstock.smartstock.core.valueobjects;

import java.util.Objects;

public record CNPJ(String valor) {

    private static final String REGEX_ALFANUMERICO = "^[A-Z0-9]{12}[0-9]{2}$";

    public CNPJ {
        Objects.requireNonNull(valor, "CNPJ não pode ser nulo");
        String normalizado = valor.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (!normalizado.matches(REGEX_ALFANUMERICO)) {
            throw new IllegalArgumentException("CNPJ deve ter 14 caracteres alfanuméricos: " + valor);
        }
        if (!isValido(normalizado)) {
            throw new IllegalArgumentException("CNPJ inválido: " + valor);
        }
        valor = normalizado;
    }

    public static CNPJ of(String valor) {
        return new CNPJ(valor);
    }

    private static boolean isValido(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }

        int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int digito1 = calcularDigitoVerificador(cnpj.substring(0, 12), peso1);
        int digito2 = calcularDigitoVerificador(cnpj.substring(0, 13), peso2);

        return cnpj.substring(12).equals(String.valueOf(digito1) + digito2);
    }

    private static int calcularDigitoVerificador(String base, int[] peso) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += valorCaractere(base.charAt(i)) * peso[i];
        }
        int resto = soma % 11;
        return (resto < 2) ? 0 : 11 - resto;
    }

    private static int valorCaractere(char c) {
        if (Character.isDigit(c)) {
            return c - '0';
        }
        return c - 'A' + 17;
    }
}
