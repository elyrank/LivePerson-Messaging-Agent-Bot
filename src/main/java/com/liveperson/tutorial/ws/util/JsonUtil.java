package com.liveperson.tutorial.ws.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.websocket.DecodeException;
import javax.websocket.EncodeException;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonUtil {
    public static JsonCodec CODEC = new JsonCodec();
    public static ObjectMapper om = new ObjectMapper();

    public static class JsonCodec {

        public JsonNode decode(String json) throws DecodeException {
            try {
                return om.readTree(json);
            } catch (IOException ex) {
                return MissingNode.getInstance();
            }
        }

        public String encode(JsonNode jsonNode) throws EncodeException {
            try {
                return om.writeValueAsString(jsonNode);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    public static JsonNode fullPath(JsonNode node, String keysPath) {
        // skip the empty root key
        for (String key : Stream.of(keysPath.split("/")).skip(1).collect(Collectors.toList()))
            node = node.path(key);
        return node;
    }

    public static ArrayNode array() {
        return om.createArrayNode();
    }

    public static ObjectNode object() {
        return om.createObjectNode();
    }


}
