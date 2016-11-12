package com.liveperson.tutorial.ws.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author elyran
 * @since 11/1/16.
 */
public class Requests {

    private static final Logger logger = LoggerFactory.getLogger(Requests.class);

    public static final String PUBLISH_EVENT = ".ams.ms.PublishEvent";
    public static final String UPDATE_RING_STATE = ".ams.routing.UpdateRingState";
    public static final String SUBSCRIBE_EX_CONVERSATIONS = ".ams.aam.SubscribeExConversations";
    public static final String GET_USER_PROFILE = ".ams.userprofile.GetUserProfile";
    public static final String EX_CONVERSATION_CHANGE_NOTIFICATION = ".ams.aam.ExConversationChangeNotification";
    public static final String RESPONSE = "$Response";
    public static final String RING_UPDATED = ".ams.routing.RingUpdated";
    public static final String UPDATE_CONVERSATION_FIELD = ".ams.cm.UpdateConversationField";
    public static final String ONLINE_EVENT_DISTRIBUTION = ".ams.ms.OnlineEventDistribution";

    public static JsonNode generateRequest(String requestType, JsonNode body, long reqId) {
        return JsonUtil.object()
                .put("kind", "req")
                .put("id", reqId)
                .put("type", requestType)
                .set("body", body);
    }

    public static JsonNode updateRingState(String ringId, long reqId, boolean accept) {
        JsonNode body = JsonUtil.object()
                .put("ringId", ringId)
                .put("ringState", accept ? "ACCEPTED" : "REJECTED");
        return generateRequest(UPDATE_RING_STATE, body, reqId);
    }

    public static JsonNode publishContentEvent(String dialogId, String message, long reqId) {
        JsonNode eventNode = JsonUtil.object()
                .put("type", "ContentEvent")
                .put("contentType", "text/plain")
                .put("message", message);

        JsonNode bodyNode = JsonUtil.object().
                put("dialogId", dialogId).
                set("event", eventNode);

        return generateRequest(PUBLISH_EVENT, bodyNode, reqId);
    }


    public static JsonNode subscribeConversationsList(String[] agentIds, String[] convStates, long reqId, String accountId) {
        ArrayNode JsonAgentIds = JsonUtil.array();
        if (agentIds != null) {
            for (String agentId : agentIds) {
                JsonAgentIds.add(agentId);
            }
        }
        ArrayNode jsonConvStates = JsonUtil.array();
        if (convStates != null) {
            for (String convState : convStates) {
                jsonConvStates.add(convState);
            }
        }
        Map<String, JsonNode> requestProperties = new HashMap<>();
        requestProperties.put("agentIds", agentIds != null ? JsonAgentIds : null);
        requestProperties.put("convState", jsonConvStates);

        JsonNode bodyNode = JsonUtil.object().setAll(requestProperties);
        ((ObjectNode)bodyNode).put("brandId",accountId);
        return generateRequest(SUBSCRIBE_EX_CONVERSATIONS, bodyNode, reqId);
    }

    public static JsonNode getUserProfile(long reqId) {
        return generateRequest(GET_USER_PROFILE, JsonUtil.object(), reqId);
    }

    public static JsonNode publishAcceptStatusEvent(String convId, String deliveryStatus, int[] sequenceList, long reqId) {
        ArrayNode jsonSequenceList = JsonUtil.array();
        for (int aSequenceList : sequenceList) {
            jsonSequenceList.add(aSequenceList);
        }

        JsonNode eventNode = JsonUtil.object()
                .put("type", "AcceptStatusEvent")
                .put("status", deliveryStatus)
                .set("sequenceList", jsonSequenceList);

        JsonNode bodyNode = JsonUtil.object()
                .put("dialogId", convId)
                .set("event", eventNode);

        return generateRequest(PUBLISH_EVENT, bodyNode, reqId);
    }

    public static JsonNode transferToSkill(String conversationId, String assignedAgentId, long newSkill, long reqId) {

        JsonNode skillField = JsonUtil.object().
                put("field", "Skill").
                put("type", "UPDATE").
                put("skill", newSkill);
        JsonNode participantField = JsonUtil.object().
                put("field", "ParticipantsChange").
                put("type", "REMOVE").
                put("userId", assignedAgentId).
                put("role", "ASSIGNED_AGENT");

        ArrayNode jsonConversationFields = JsonUtil.array();
        jsonConversationFields.add(skillField);
        jsonConversationFields.add(participantField);

        JsonNode bodyNode = JsonUtil.object().
                put("conversationId", conversationId).
                set("conversationField", jsonConversationFields);

        return generateRequest(UPDATE_CONVERSATION_FIELD, bodyNode, reqId);
    }

    public static JsonNode resolveConversation(String convId, int reqId) {

        JsonNode conversationField = JsonUtil.object().
                put("field", "ConversationStateField").
                put("conversationState", "CLOSE");
        JsonNode bodyNode = JsonUtil.object().
                put("conversationId", convId).
                set("conversationField", conversationField);
        return generateRequest(UPDATE_CONVERSATION_FIELD, bodyNode, reqId);
    }
}
