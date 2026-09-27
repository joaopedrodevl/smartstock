package br.com.smartstock.smartstock.application.exceptions;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {
    private final String dadoBuscado;

    public UserNotFoundException(String dadoBuscado) {
        super(String.format("Usuário '%s' não foi encontrado.", dadoBuscado));
        this.dadoBuscado = dadoBuscado;
    }

}
