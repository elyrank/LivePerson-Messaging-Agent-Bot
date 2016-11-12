package com.liveperson.tutorial.ws.bot.base;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the main functionality of the bot - get a message from a user and decide how to reply to him.
 *
 * @author Elyran Kogan
 * @since 11/2/16.
 */

public interface ReplyLogic {

    String getHelloMessage();
    String getReconnectMessage();
    void reply(String convId, String message, AtomicInteger reqId);
    void handleRing(String ringId, String currentConversation, String skillId, AtomicInteger reqId);
}
