package com.liveperson.tutorial.ws.bot.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.liveperson.tutorial.ws.bot.base.Address;
import com.liveperson.tutorial.ws.bot.base.AgentBot;
import com.liveperson.tutorial.ws.util.JsonUtil;
import com.liveperson.tutorial.ws.util.Requests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Address Bot implementation
 * This bot will display a message to write an address, display that address in a map and ask the user to approve
 * after approval - transfer the conversation back to the main bot
 *
 * @author elyran
 * @since 11/2/16.
 */
@Component
public class AddressBot extends AgentBot {


    @Value("${lp.messaging.agent.bot.address.username}")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Value("${lp.messaging.agent.bot.address.password}")
    public void setPassword(String password) {
        this.password = password;
    }

    @Value("${lp.messaging.agent.bot.address.option.accept}")
    private String accept;

    @Value("${lp.messaging.agent.bot.address.option.decline}")
    private String decline;

    @Value("${lp.messaging.agent.bot.address.message.hello}")
    private String helloMessage;

    @Value("${lp.messaging.agent.bot.address.message.reconnect}")
    private String reconnectMessage;

    @Value("${lp.messaging.agent.bot.address.message.select.error}")
    private String errorMessage;

    @Value("${lp.messaging.agent.bot.address.message.select.success}")
    private String successMessage;

    @Autowired
    Address address;

    @Override
    public String getHelloMessage() {
        return helloMessage;
    }

    @Override
    public String getReconnectMessage() {
        return reconnectMessage;
    }

    @Override
    public void reply(String convId, String response, AtomicInteger reqId) {
        if (response.equals(accept)) {
            addressAccepted(convId, reqId);
        } else if (response.equals(decline)) {
            sendMessage(convId, getReconnectMessage(), reqId.incrementAndGet());
        } else {
            setAddress(convId, response, reqId);
        }
    }

    private void setAddress(String convId, String response, AtomicInteger reqId) {
        //try to set address
        final Address.Data mapData = address.getMapData(response);
        if (mapData != null) {
            sendMessage(convId, buildSelection("Is this the correct address?", mapData), reqId.incrementAndGet());
        } else {
            sendMessage(convId, errorMessage, reqId.incrementAndGet());
        }
    }

    private void addressAccepted(String convId, AtomicInteger reqId) {
        //address approved
        sendMessage(convId, successMessage, reqId.incrementAndGet());
        transfer(convId, reqId.incrementAndGet());
    }

    private void transfer(String convId, long reqId) {
        wsClient.send(Requests.transferToSkill(convId, getAgentId(), -1, reqId));
    }

    private void sendMessage(String convId, String message, long reqId) {
        wsClient.send(Requests.publishContentEvent(convId, message, reqId));
    }

    /**
     * build a message in a special format the IOS app will be able to read and display as buttons
     * this is an experimental format and is not released yet.
     * @param message - the message text header
     * @return formatted text to display selection
     */
    public String buildSelection(String message, Address.Data data) {
        StringBuilder sb = new StringBuilder();
        String start = "@@LP@@{\"content\": \"" + message + "\",\"boards\":[[";
        sb.append(start);
        List<String> options = new ArrayList<>();
        options.add("map");
        options.add(accept);
        options.add(decline);

        final String str = options.stream()
                .map(skill -> skill.equals("map") ? build(data.getAddress(), "map", data) : build(skill, "btn", null))
                .collect(Collectors.joining(","));

        sb.append(str).append("]]}");
        return sb.toString();
    }

    private String build(String text, String type, Address.Data data) {
        final ObjectNode dataNode = JsonUtil.object()
                .put("action", "msg")
                .put("text", text);

        if (data != null) {
            dataNode.put("longitude", data.getLongitude() + "")
                    .put("latitude", data.getLatitude() + "");
        }
        return JsonUtil.object().put("type", type).set("data", dataNode).toString();
    }


}
