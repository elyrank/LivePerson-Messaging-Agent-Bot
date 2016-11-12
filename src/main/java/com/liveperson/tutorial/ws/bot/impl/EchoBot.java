package com.liveperson.tutorial.ws.bot.impl;

import com.liveperson.tutorial.ws.bot.base.AgentBot;
import com.liveperson.tutorial.ws.util.Requests;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basic Echo implementation
 *
 * @author elyran
 * @since 11/2/16.
 */
@Component
public class EchoBot extends AgentBot {

    @Value("${lp.messaging.agent.bot.echo.username}")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Value("${lp.messaging.agent.bot.echo.password}")
    public void setPassword(String password) {
        this.password = password;
    }

    @Value("${lp.messaging.agent.bot.echo.message.hello}")
    private String helloMessage;

    @Value("${lp.messaging.agent.bot.echo.message.reconnect}")
    private String reconnectMessage;

    @Override
    public String getHelloMessage() {
        return helloMessage;
    }

    @Override
    public String getReconnectMessage() {
        return reconnectMessage;
    }

    @Override
    public void reply(String convId, String message, AtomicInteger reqId) {
        wsClient.send(Requests.publishContentEvent(convId, "Echo: " + message, reqId.incrementAndGet()));
    }


}
