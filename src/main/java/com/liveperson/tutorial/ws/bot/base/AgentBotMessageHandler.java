package com.liveperson.tutorial.ws.bot.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.liveperson.tutorial.ws.client.JsonMessageHandler;
import com.liveperson.tutorial.ws.client.WsClient;
import com.liveperson.tutorial.ws.util.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author elyran
 * @since 10/26/16.
 */
public class AgentBotMessageHandler implements JsonMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(AgentBotMessageHandler.class);

    private AgentBot agentBot;
    private WsClient wsClient;

    public AgentBotMessageHandler(AgentBot agentBot, WsClient wsClient) {
        this.agentBot = agentBot;
        this.wsClient = wsClient;
    }

    private AtomicInteger reqId = new AtomicInteger(1);

    private Map<String, Conversation> conversations = new ConcurrentHashMap<>();


    public void init() {
        wsClient.send(Requests.subscribeConversationsList(new String[]{agentBot.getAgentId()}, new String[]{"OPEN"}, reqId.incrementAndGet(), agentBot.accountId));
    }

    @Override
    public void onMessage(JsonNode node) {
        final String type = node.get("type").asText();
        switch (type) {
            case Requests.RING_UPDATED:
                handleRing(node);
                break;
            case Requests.GET_USER_PROFILE + Requests.RESPONSE:
                handleGetUserProfile(node);
                break;
            case Requests.EX_CONVERSATION_CHANGE_NOTIFICATION:
                handleConversations(node);
                break;
            case Requests.ONLINE_EVENT_DISTRIBUTION:
                handleMessagingEvents(node);
                break;

        }
    }

    private void handleMessagingEvents(JsonNode node) {
        final JsonNode body = node.get("body");
        final String convId = body.get("dialogId").asText();
        final JsonNode seqNode = body.get("sequence");
        final Conversation conversation = conversations.get(convId);
        if (seqNode != null && conversation != null && conversation.compareAndSetSequence(seqNode.asInt())) {
            final int sequence = seqNode.asInt();
            //check if it is my message - and respond only if not
            if (!body.get("originatorId").asText().equals(agentBot.getAgentId())) {
                sendRead(convId, sequence);
                sendAccept(convId, sequence);
                final JsonNode event = body.get("event");
                final String eventType = event.get("type").asText();
                if (eventType.equals("ContentEvent")) {
                    final String message = event.get("message").asText();
                    reply(convId, message);
                }
            }
        }
    }

    private void handleGetUserProfile(JsonNode node) {
        final String userId = node.findPath("userId").asText();
    }

    private void handleRing(JsonNode node) {
        final JsonNode body = node.get("body");
        final String ringId = body.get("ringId").asText();
        final String ringState = body.get("ringState").asText();
        String currentConversation = body.get("convId").asText();
        final String skillId = body.get("skillId").asText();
        if (ringState.equals("WAITING")) {
            handleRing(ringId, currentConversation, skillId);
        } else if (ringState.equals("ACCEPTED")) {
            logger.info("conversation {} was accepted successfully", currentConversation);
            //wsClient.send(Requests.publishContentEvent(currentConversation, agentBot.getHelloMessage(), reqId.incrementAndGet()));
        }
    }

    protected void handleRing(String ringId, String currentConversation, String skillId) {
        agentBot.handleRing(ringId, currentConversation, skillId, reqId);
    }

    private void reply(String convId, String message) {
        final String response = message.trim();
        if (!agentBot.genericReply(convId, response, reqId)) {
            agentBot.reply(convId, response, reqId);
        }
    }

    private void sendAccept(String convId, int sequence) {
        Requests.publishAcceptStatusEvent(convId, "ACCEPT", new int[]{sequence}, reqId.incrementAndGet());
    }

    private void sendRead(String convId, int sequence) {
        Requests.publishAcceptStatusEvent(convId, "READ", new int[]{sequence}, reqId.incrementAndGet());
    }

    private void handleConversations(JsonNode node) {
        final JsonNode changes = node.findPath("changes");
        if (changes.isArray()) {
            for (JsonNode change : changes) {
                final String type = change.get("type").asText();
                final String convId = change.findPath("convId").asText();
                if (type.equals("UPSERT")) {
                    final Conversation conversation = new Conversation();
                    final Conversation existing = conversations.putIfAbsent(convId, conversation);
                    if (existing == null) {
                        final int sequence = change.findPath("sequence").asInt();
                        conversation.compareAndSetSequence(sequence);
                        wsClient.send(Requests.publishContentEvent(convId, agentBot.getHelloMessage(), reqId.incrementAndGet()));
                    }
                } else if (type.equals("DELETE")) {
                    conversations.remove(convId);
                }

            }

        }
    }


}
