package br.com.smartstock.smartstock.infra.websocket;

public record LeituraRfidEvent(
        String leituraId,
        String codigoRfid,
        String chavePublica
) {
}
