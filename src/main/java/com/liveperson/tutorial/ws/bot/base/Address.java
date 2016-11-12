package com.liveperson.tutorial.ws.bot.base;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;

/**
 * @author elyran
 * @since 11/8/16.
 */
@Component
public class Address {

    private static final Logger logger = LoggerFactory.getLogger(Address.class);
    public static final String GOOGLE_API = "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${google.api.key}")
    private String apiKey;

    public Data getMapData(String address) {
        try {
            final String addressEncoded = URLEncoder.encode(address, "UTF-8");
            final String url = String.format(GOOGLE_API, addressEncoded, apiKey);
            logger.info("sending google api request: {}", url);
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add(HttpHeaders.ACCEPT, "application/json");
            final ResponseEntity<JsonNode> entity = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            final JsonNode body = entity.getBody();
            if (body.get("status").asText().equals("OK")) {
                final String formatted_address = body.findPath("formatted_address").asText();
                final JsonNode location = body.findPath("location");
                logger.info("address result: {}", body);
                return new Data(formatted_address, location.get("lng").asDouble(), location.get("lat").asDouble());
            } else {
                logger.warn("no results found for address: {}", address);
            }
        } catch (Exception e) {
            logger.error("failed to get address ",e);
        }
        return null;
    }

    public static class Data {
        private String address;
        private double longitude;
        private double latitude;

        public Data(String address, double longitude, double latitude) {
            this.address = address;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        public String getAddress() {
            return address;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getLatitude() {
            return latitude;
        }
    }


}
