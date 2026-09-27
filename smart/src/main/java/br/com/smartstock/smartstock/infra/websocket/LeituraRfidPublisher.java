package br.com.smartstock.smartstock.infra.websocket;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class LeituraRfidPublisher {

    private static ApplicationEventPublisher publisher;

    public LeituraRfidPublisher(ApplicationEventPublisher publisher) {
        LeituraRfidPublisher.publisher = publisher;
    }

    public static void publish(LeituraRfidEvent event) {
        publisher.publishEvent(event);
    }
}
