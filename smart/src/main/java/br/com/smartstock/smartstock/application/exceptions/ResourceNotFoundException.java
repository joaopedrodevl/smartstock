package br.com.smartstock.smartstock.application.exceptions;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String recurso;
    private final String detalhe;

    public ResourceNotFoundException(String recurso, String detalhe) {
        super(String.format("%s nao encontrado: %s", recurso, detalhe));
        this.recurso = recurso;
        this.detalhe = detalhe;
    }

    public ResourceNotFoundException(String recurso) {
        super(String.format("%s nao encontrado", recurso));
        this.recurso = recurso;
        this.detalhe = null;
    }
}
