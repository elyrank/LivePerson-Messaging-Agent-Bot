package com.liveperson.tutorial.ws.client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author elyran
 * @since 10/20/16.
 */
public interface JsonMessageHandler {
    void onMessage(JsonNode node);
}
