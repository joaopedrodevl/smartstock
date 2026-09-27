package br.com.smartstock.smartstock.infra.config;

import br.com.smartstock.smartstock.infra.websocket.EmbarcadoWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final EmbarcadoWebSocketHandler embarcadoWebSocketHandler;

    public WebSocketConfig(EmbarcadoWebSocketHandler embarcadoWebSocketHandler) {
        this.embarcadoWebSocketHandler = embarcadoWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(embarcadoWebSocketHandler, "/ws/embarcado")
                .setAllowedOrigins("*");
    }
}
