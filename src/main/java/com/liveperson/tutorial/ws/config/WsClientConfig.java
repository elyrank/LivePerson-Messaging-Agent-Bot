package com.liveperson.tutorial.ws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author elyran
 * @since 11/2/16.
 */
@Configuration
public class WsClientConfig {

    @Bean
    public WebSocketContainer webSocketContainer() {
        return ContainerProvider.getWebSocketContainer();
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ClientEndpointConfig.Configurator clientEndpointConfig() {
        return new ClientEndpointConfig.Configurator();
    }
}
