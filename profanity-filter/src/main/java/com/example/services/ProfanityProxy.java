package com.example.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;

@SpringBootApplication
@RestController
public class ProfanityProxy {

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST},
            value = "/api/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> foo(@RequestBody(required = false) ObjectNode payload,
                    final HttpServletRequest request) {

        RestTemplate rest = new RestTemplate();
        ObjectNode newPayload = maskProfanityWords(payload);

        final String legacyApiUrl = "http://localhost:8080/";
        final String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        URI uri = UriComponentsBuilder.fromHttpUrl(legacyApiUrl)
                .path(path).build().toUri();

        String method = request.getMethod();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(newPayload.toString(), headers);

        return rest.exchange(uri, HttpMethod.resolve(method), entity, String.class);
    }

    public ObjectNode maskProfanityWords(ObjectNode payload) {
        RestTemplate rest = new RestTemplate();
        URI uri = UriComponentsBuilder.fromHttpUrl("http://localhost:19990/")
                .path("checkprofanity")
                .queryParam("text", payload.at("/title").asText())
                .build().toUri();

        ResponseEntity<Profanity> profanityResponse = rest.getForEntity(uri, Profanity.class);
        Profanity isSwearWord = profanityResponse.getBody();

        if (isSwearWord.containsProfanity == true) {
            payload.put("title", isSwearWord.output);
        } else {
            payload.put("title", isSwearWord.input);
        }

        return payload;
    }

    public static void main(String[] args) {
        SpringApplication.run(ProfanityProxy.class, args);
    }

}

//{"containsProfanity":true,"input":"This is shit","output":"This is ****"}
class Profanity {
    public final boolean containsProfanity;
    public final String input;
    public final String output;

    @JsonCreator
    Profanity(@JsonProperty("containsProfanity") boolean containsProfanity,
              @JsonProperty("input") String input,
              @JsonProperty("output") String output) {
        this.containsProfanity = containsProfanity;
        this.input = input;
        this.output = output;
    }
}