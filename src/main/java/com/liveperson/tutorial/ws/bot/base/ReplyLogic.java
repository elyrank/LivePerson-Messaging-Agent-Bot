package com.liveperson.tutorial.ws.bot.base;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the main functionality of the bot - get a message from a user and decide how to reply to him.
 *
 * @author Elyran Kogan
 * @since 11/2/16.
 */

public interface ReplyLogic {
    /**
     * @return The first message the bot will send
     */
    String getHelloMessage();
    /**
     * @return when disconnected and came back online
     */
    String getReconnectMessage();
    /**
     * This is the main logic of the bot
     * Decide here how to react to a consumer's message
     */
    void reply(String convId, String message, AtomicInteger reqId);
    /**
     * Decide if to accept or reject an incoming ring (new conversation request)
     */
    void handleRing(String ringId, String currentConversation, String skillId, AtomicInteger reqId);
}
