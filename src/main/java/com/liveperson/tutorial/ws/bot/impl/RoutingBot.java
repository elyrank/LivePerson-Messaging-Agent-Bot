package com.liveperson.tutorial.ws.bot.impl;

import com.liveperson.tutorial.ws.bot.base.AgentBot;
import com.liveperson.tutorial.ws.util.JsonUtil;
import com.liveperson.tutorial.ws.util.Requests;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Routing Bot implementation
 * This bot will display a message to select a skill
 * and transfer the conversation according to the selection
 *
 * @author elyran
 * @since 11/2/16.
 */
@Component
public class RoutingBot extends AgentBot {


    @Value("${lp.messaging.agent.bot.routing.username}")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Value("${lp.messaging.agent.bot.routing.password}")
    public void setPassword(String password) {
        this.password = password;
    }

    @Value("${lp.messaging.agent.bot.routing.message.hello}")
    private String helloMessage;

    @Value("${lp.messaging.agent.bot.routing.message.reconnect}")
    private String reconnectMessage;

    @Value("${lp.messaging.agent.bot.routing.message.select.error}")
    private String errorMessage;

    @Value("${lp.messaging.agent.bot.routing.message.select.success}")
    private String successMessage;

    @Value("#{'${lp.messaging.agent.bot.routing.skill.names}'.split(',')}")
    private List<String> skillNames;

    @Value("#{'${lp.messaging.agent.bot.routing.skill.ids}'.split(',')}")
    private List<Long> skillIds;

    @Override
    public String getHelloMessage() {
        return buildSelection(helloMessage);
    }

    @Override
    public String getReconnectMessage() {
        return buildSelection(reconnectMessage);
    }

    @Override
    public void handleRing(String ringId, String currentConversation, String skillId, AtomicInteger reqId) {
        acceptConversation(ringId, currentConversation, skillId, reqId, true);
    }

    @Override
    public void reply(String convId, String response, AtomicInteger reqId) {
        for (int i = 0; i < skillNames.size(); i++) {
            final String skillName = skillNames.get(i);
            final Long skillId = skillIds.get(i);
            if (response.equals(String.valueOf(skillName))) {
                sendMessage(convId, String.format(successMessage, skillName), reqId.incrementAndGet());
                transfer(convId, skillId, reqId.incrementAndGet());
                return;
            }
        }
        if (response.equals("Check usage")) {
            sendMessage(convId, "You have used 90% of your data for Nov. with two weeks remaining", reqId.incrementAndGet());
        } else {
            //no valid response
            sendMessage(convId, errorMessage, reqId.incrementAndGet());
            sendMessage(convId, getReconnectMessage(), reqId.incrementAndGet());
        }
    }

    private void transfer(String convId, Long skillId, long reqId) {
        wsClient.send(Requests.transferToSkill(convId, getAgentId(), skillId, reqId));
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
    public String buildSelection(String message) {
        StringBuilder sb = new StringBuilder();
        String start = "@@LP@@{\"content\": \"" + message + "\",\"boards\":[";
        sb.append(start);
        final String skillObj = skillNames.stream()
                .map(skill -> JsonUtil.array().add(JsonUtil.object().put("type", "btn").set("data", JsonUtil.object().put("action", "msg").put("text", skill))).toString())
                .collect(Collectors.joining(","));
        sb.append(skillObj).append("]}");
        return sb.toString();
    }


}
