package br.com.smartstock.smartstock.core.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * Value object para representar valores monetários em centavos.
 * Armazena como Long para evitar erros de arredondamento de BigDecimal/double.
 * A API recebe e retorna centavos (ex: 899 = R$ 8,99).
 */
public final class Dinheiro implements Comparable<Dinheiro> {

    private final long centavos;

    private Dinheiro(long centavos) {
        this.centavos = centavos;
    }

    /**
     * Cria um Dinheiro a partir de centavos (ex: Dinheiro.deCentavos(899) = R$ 8,99)
     */
    public static Dinheiro deCentavos(long centavos) {
        return new Dinheiro(centavos);
    }

    /**
     * Cria um Dinheiro a partir de reais (ex: Dinheiro.deReais(8.99) = R$ 8,99)
     * Útil para testes e migração de dados. Não usar em produção.
     */
    public static Dinheiro deReais(double reais) {
        long centavos = Math.round(reais * 100);
        return new Dinheiro(centavos);
    }

    @JsonCreator
    public static Dinheiro fromJson(long centavos) {
        return new Dinheiro(centavos);
    }

    @JsonValue
    public long toCentavos() {
        return centavos;
    }

    public long getCentavos() {
        return centavos;
    }

    public long toReais() {
        return centavos / 100;
    }

    public long getCasasDecimais() {
        return Math.abs(centavos % 100);
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(this.centavos + outro.centavos);
    }

    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(this.centavos - outro.centavos);
    }

    public Dinheiro multiplicar(int fator) {
        return new Dinheiro(this.centavos * fator);
    }

    public boolean isMaiorQue(Dinheiro outro) {
        return this.centavos > outro.centavos;
    }

    public boolean isMenorQue(Dinheiro outro) {
        return this.centavos < outro.centavos;
    }

    public boolean isMaiorOuIgualA(Dinheiro outro) {
        return this.centavos >= outro.centavos;
    }

    public boolean isMenorOuIgualA(Dinheiro outro) {
        return this.centavos <= outro.centavos;
    }

    /**
     * Formata para exibição: R$ 8,99
     */
    public String formatado() {
        long reais = centavos / 100;
        long centavosAbs = Math.abs(centavos % 100);
        return String.format("R$ %d,%02d", reais, centavosAbs);
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return Long.compare(this.centavos, outro.centavos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dinheiro dinheiro)) return false;
        return centavos == dinheiro.centavos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(centavos);
    }

    @Override
    public String toString() {
        return formatado();
    }
}
