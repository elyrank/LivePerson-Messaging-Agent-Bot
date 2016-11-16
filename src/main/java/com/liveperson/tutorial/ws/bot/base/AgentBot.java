package com.liveperson.tutorial.ws.bot.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.liveperson.tutorial.ws.client.WsClient;
import com.liveperson.tutorial.ws.util.JsonUtil;
import com.liveperson.tutorial.ws.util.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author elyran
 * @since 10/20/16.
 */

public abstract class AgentBot implements ReplyLogic {

    private static final Logger logger = LoggerFactory.getLogger(AgentBot.class);

    @Value("${lp.messaging.api.version}")
    private int version;

    @Value("${lp.messaging.accountId}")
    protected String accountId;

    @Value("${lp.messaging.uri.format}")
    private String uriFormat;

    @Value("${lp.agentVep.domain}")
    private String agentVepDomain;

    @Value("${lp.messaging.agent.bot.message.bye}")
    private String byeMessage;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RestTemplate restTemplate;

    protected String userName;
    protected String password;
    protected WsClient wsClient;
    private AgentBotMessageHandler agentBotMessageHandler;
    private AgentDetails agentDetails;

    public AgentBot() {
    }

    @PostConstruct
    public void init() {
        try {
            agentDetails = agentAuth();
            wsClient = context.getBean(WsClient.class);
            agentBotMessageHandler = new AgentBotMessageHandler(this, wsClient);
            String uri = String.format(uriFormat, agentDetails.messagingDomain, accountId, agentDetails.bearer, version);
            //add logger handler
            wsClient.addMessageHandler(node -> logger.info("agent [{}] received message: {}",userName, node.toString()));
            //add message handler
            wsClient.addMessageHandler(agentBotMessageHandler);
            //add tasks to run after connection is opened
            wsClient.addOnOpenHandler(session -> agentBotMessageHandler.init());
            wsClient.connect(uri);
            logger.info("agent [{}] wsClient connected successfully to uri {}", userName, uri);
        } catch (Exception e) {
            logger.error("failed to initialize wsClient", e);
        }
    }

    private AgentDetails agentAuth() {
        try {
            JsonNode jsonNode = JsonUtil.object().put("username", userName).put("password", password);
            String url = String.format("https://%s/api/account/%s/login?v=1.1", agentVepDomain, accountId);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            final ResponseEntity<JsonNode> entity = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(jsonNode.toString(), headers), JsonNode.class);
            final JsonNode body = entity.getBody();
            final String bearer = body.get("bearer").asText();
            final String userId = body.get("config").get("userId").asText();
            String messagingDomain = null;
            final JsonNode domains = body.get("csdsCollectionResponse").get("baseURIs");
            for (JsonNode domain : domains) {
                if (domain.get("service").asText().equals("asyncMessagingEnt")) {
                    messagingDomain = domain.get("baseURI").asText();
                    break;
                }
            }
            return new AgentDetails(accountId + "." + userId, bearer, messagingDomain);
        } catch (Exception e) {
            logger.error("failed to authenticate agent {}", userName, e);
            throw e;
        }
    }

    @Override
    public void handleRing(String ringId, String currentConversation, String skillId, AtomicInteger reqId) {
         //by default - do not accept conversations when skill is unassigned (-1) - this will make sure only the bot you want to answer will take the conversation
        acceptConversation(ringId, currentConversation, skillId, reqId, !skillId.equals("-1"));
    }

    protected void acceptConversation(String ringId, String currentConversation, String skillId, AtomicInteger reqId, boolean accept) {
        logger.info("accept conversation {} in skill: {}", currentConversation, skillId);
        wsClient.send(Requests.updateRingState(ringId, reqId.incrementAndGet(), accept));
    }

    public String getAgentId() {
        return agentDetails.userId;
    }

    public boolean genericReply(String convId, String response, AtomicInteger reqId) {
        //all bots will respond if this text is entered
        if (response.equalsIgnoreCase("bye") || response.equalsIgnoreCase("exit")) {
            wsClient.send(Requests.publishContentEvent(convId, byeMessage, reqId.incrementAndGet()));
            resolveConversation(convId, reqId.incrementAndGet());
            return true;
        }
        return false;
    }

    protected void resolveConversation(String convId, int reqId) {
        wsClient.send(Requests.resolveConversation(convId, reqId));
    }

    static class AgentDetails {
        public final String userId;
        public final String bearer;
        public final String messagingDomain;


        public AgentDetails(String userId, String bearer, String messagingDomain) {
            this.userId = userId;
            this.bearer = bearer;
            this.messagingDomain = messagingDomain;
        }

    }
}
