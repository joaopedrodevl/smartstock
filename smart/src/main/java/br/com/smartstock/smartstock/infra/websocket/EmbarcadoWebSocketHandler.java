package br.com.smartstock.smartstock.infra.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmbarcadoWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(EmbarcadoWebSocketHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionEmbarcadoMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket conectado - sessionId: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Mensagem recebida - sessionId: {}, payload: {}", session.getId(), payload);

        Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
        String tipo = (String) msg.get("tipo");

        if ("REGISTRO".equals(tipo)) {
            String chavePublica = (String) msg.get("chavePublica");
            sessions.put(chavePublica, session);
            sessionEmbarcadoMap.put(session.getId(), chavePublica);
            log.info("Embarcado registrado via WebSocket - chavePublica: {}, sessionId: {}", chavePublica, session.getId());

            enviarMensagem(session, Map.of(
                    "tipo", "REGISTRO_CONFIRMADO",
                    "status", "ok"
            ));
        } else if ("RFID_LIDO".equals(tipo)) {
            String leituraId = (String) msg.get("leituraId");
            String codigoRfid = (String) msg.get("codigoRfid");
            String chavePublica = sessionEmbarcadoMap.get(session.getId());

            log.info("RFID lido pelo embarcado - leituraId: {}, rfid: {}, chavePublica: {}",
                    leituraId, codigoRfid, chavePublica);

            if (leituraId != null && codigoRfid != null) {
                LeituraRfidEvent event = new LeituraRfidEvent(leituraId, codigoRfid, chavePublica);
                LeituraRfidPublisher.publish(event);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String chavePublica = sessionEmbarcadoMap.remove(session.getId());
        if (chavePublica != null) {
            sessions.remove(chavePublica);
            log.info("Embarcado desconectado - chavePublica: {}", chavePublica);
        }
    }

    public boolean enviarParaEmbarcado(String chavePublica, Map<String, Object> mensagem) {
        WebSocketSession session = sessions.get(chavePublica);
        if (session == null || !session.isOpen()) {
            log.warn("Embarcado nao conectado via WebSocket - chavePublica: {}", chavePublica);
            return false;
        }

        try {
            enviarMensagem(session, mensagem);
            return true;
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem para embarcado - chavePublica: {}", chavePublica, e);
            return false;
        }
    }

    public boolean isEmbarcadoConectado(String chavePublica) {
        WebSocketSession session = sessions.get(chavePublica);
        return session != null && session.isOpen();
    }

    private void enviarMensagem(WebSocketSession session, Map<String, Object> mensagem) throws IOException {
        String json = objectMapper.writeValueAsString(mensagem);
        session.sendMessage(new TextMessage(json));
    }
}
