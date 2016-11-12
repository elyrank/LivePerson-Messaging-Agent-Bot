package com.liveperson.tutorial.ws.client;

import javax.websocket.Session;

/**
 * @author elyran
 * @since 11/6/16.
 */
public interface OnOpenHandler {
    void doOnOpen(Session session);
}
