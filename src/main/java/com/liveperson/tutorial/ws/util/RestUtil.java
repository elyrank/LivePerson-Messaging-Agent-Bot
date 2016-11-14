package com.liveperson.tutorial.ws.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @author elyran
 * @since 11/15/16.
 */
public class RestUtil {

    public static JsonNode getJsonNode(String url, RestTemplate restTemplate) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        final ResponseEntity<JsonNode> entity = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        return entity.getBody();
    }
}
