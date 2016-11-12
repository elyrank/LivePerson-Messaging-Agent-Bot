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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author elyran
 * @since 10/20/16.
 */
@Component
@Scope("prototype")
public class ClientEndPoint extends Endpoint implements MessageHandler.Whole<String>{

    protected Session session;
    private Collection<JsonMessageHandler> handlers = new ArrayList<>();

    @Autowired
    CounterService counterService;

    @Autowired
    private ScheduledExecutorService executor;

    private static final Logger logger = LoggerFactory.getLogger(ClientEndPoint.class);
    private ScheduledFuture<?> pingTask;

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        session.addMessageHandler(this);
        this.session = session;
        logger.info("connection opened: {}", session);
        pingTask = schedulePingTask();
    }

    /**
     * Schedule a ping task so the connection will have a keep alive
     * for the POC it is enough to do only the ping without actually waiting for the pong
     * @return scheduled task for ping messages
     */
    private ScheduledFuture<?> schedulePingTask() {
        return executor.scheduleWithFixedDelay(() -> {
            try {
                session.getAsyncRemote().sendPing(ByteBuffer.wrap("ping".getBytes()));
            } catch (Exception e) {
                logger.error("failed to send ping", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("connection closed, reason:  {}, session: {}", closeReason, session);
        pingTask.cancel(false);
        super.onClose(session, closeReason);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        logger.error("error on session: {}", session, thr);
        super.onError(session, thr);
    }

    @Override
    public void onMessage(String message) {
        counterService.increment("ws.incoming.messages");
        try {
            Optional.of(JsonUtil.CODEC.decode(message)).ifPresent(this::handle);
        } catch (DecodeException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void handle(JsonNode msg) {
        handlers.forEach(handler -> handler.onMessage(msg));
    }

    public void addMessageHandler(JsonMessageHandler jsonMessageHandler) {
        handlers.add(jsonMessageHandler);
    }
}