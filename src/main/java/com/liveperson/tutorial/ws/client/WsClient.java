package com.liveperson.tutorial.ws.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.liveperson.tutorial.ws.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.*;

/**
 * @author elyran
 * @since 10/20/16.
 */
@Component
@Scope("prototype")
public class WsClient {

    private static final Logger logger = LoggerFactory.getLogger(WsClient.class);

    @Autowired
    CounterService counterService;

    @Autowired
    private WebSocketContainer container;

    private Session session;

    @Autowired
    private ClientEndPoint endPoint;

    @Autowired
    private ScheduledExecutorService executor;

    @Autowired
    private ClientEndpointConfig.Configurator clientEndpointConfigurator;

    private ScheduledFuture<?> reconnectTask;
    private OnOpenHandler onOpenHandler;

    public void connect(String uri) throws DeploymentException, IOException, InterruptedException {
        logger.info("connecting websocket to: {} ", uri);
        doConnect(uri);
        reconnectTask = scheduleReconnectTask(uri);
    }

    /**
     * if for some reason the connection was closed - reconnect the websocket
     * @param uri - the uri to connect to
     * @return scheduled task for running reconnect
     */
    private ScheduledFuture<?> scheduleReconnectTask(String uri) {
        return executor.scheduleWithFixedDelay(() -> {
            if (!session.isOpen()) {
                try {
                    doConnect(uri);
                } catch (Exception e) {
                    logger.error("failed to reconnect ", e);
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void doConnect(String uri) throws DeploymentException, IOException {
        session = container.connectToServer(endPoint,
                ClientEndpointConfig.Builder.create().configurator(clientEndpointConfigurator).build(),
                URI.create(uri));
        onOpenHandler.doOnOpen(session);
    }

    public void send(JsonNode node) {
        try {
            logger.info("sending request: {}", node);
            counterService.increment("ws.outgoing.messages");
            final Future<Void> future = session.getAsyncRemote().sendText(JsonUtil.CODEC.encode(node));
            handleError(node, future);
        } catch (EncodeException e) {
            logger.error("failed to encode json: {}", node);
        }
    }

    /**
     * Async handle failure - just to be aware if something went wrong
     * @param node - the json message
     * @param future - the result of send operation
     */
    private void handleError(JsonNode node, Future<Void> future) {
        final CompletableFuture<Void> completableFuture = makeCompletableFuture(future);
        completableFuture.whenComplete((aVoid, throwable) -> {
            if (throwable != null) {
                logger.error("failed to send message: {}", node, throwable);
            }
        });
    }

    public void close() throws IOException {
        if (session != null && session.isOpen()) {
            session.close();
            reconnectTask.cancel(false);
        }
    }

    public void addMessageHandler(JsonMessageHandler jsonMessageHandler) {
        endPoint.addMessageHandler(jsonMessageHandler);
    }

    public void addOnOpenHandler(OnOpenHandler onOpenHandler) {
        this.onOpenHandler = onOpenHandler;
    }


    public static CompletableFuture<Void> makeCompletableFuture(Future<Void> future) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get();
            } catch (InterruptedException|ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
