package com.cac.oa.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Minimal OpenAI-compatible gateway client. Provider keys stay inside this server process. */
@Component
public class OpenAiGatewayClient {
    private final RestTemplate restTemplate;
    private final RestTemplate streamingRestTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiGatewayClient(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
        SimpleClientHttpRequestFactory streamingFactory = new SimpleClientHttpRequestFactory();
        streamingFactory.setConnectTimeout(10_000);
        streamingFactory.setReadTimeout(300_000);
        this.streamingRestTemplate = new RestTemplate(streamingFactory);
        this.objectMapper = objectMapper;
    }

    public JsonNode get(String baseUrl, String path, String apiKey) {
        return exchange(baseUrl, path, HttpMethod.GET, null, apiKey);
    }

    public JsonNode post(String baseUrl, String path, Object body, String apiKey) {
        return exchange(baseUrl, path, HttpMethod.POST, body, apiKey);
    }

    public StreamResponse streamPost(String baseUrl, String path, Object body, String apiKey,
            StreamListener listener) {
        validateBaseUrl(baseUrl);
        try {
            return streamingRestTemplate.execute(baseUrl + path, HttpMethod.POST, request -> {
                HttpHeaders headers = request.getHeaders();
                headers.setBearerAuth(apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);
                objectMapper.writeValue(request.getBody(), body);
            }, response -> readStream(response, listener));
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "The AI gateway rejected this request (HTTP " + exception.getRawStatusCode() + ")");
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to reach the AI gateway. Check its address and network access.");
        }
    }

    private JsonNode exchange(String baseUrl, String path, HttpMethod method, Object body, String apiKey) {
        validateBaseUrl(baseUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + path, method, new HttpEntity<Object>(body, headers), String.class);
            return objectMapper.readTree(response.getBody());
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("The AI gateway rejected this request (HTTP " + exception.getRawStatusCode() + ")");
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to reach the AI gateway. Check its address and network access.");
        }
    }

    private StreamResponse readStream(org.springframework.http.client.ClientHttpResponse response,
            StreamListener listener) throws IOException {
        StreamResponse result = new StreamResponse();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring("data:".length()).trim();
                if (payload.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }
                JsonNode event = objectMapper.readTree(payload);
                result.setId(event.path("id").asText(result.getId()));
                result.setModel(event.path("model").asText(result.getModel()));
                JsonNode choice = event.path("choices").path(0);
                String delta = choice.path("delta").path("content").asText();
                if (!delta.isEmpty()) {
                    listener.onDelta(delta);
                }
                String finishReason = choice.path("finish_reason").asText();
                if (!finishReason.isEmpty()) {
                    result.setFinishReason(finishReason);
                }
            }
        }
        return result;
    }

    public String normalizeBaseUrl(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Gateway address is required");
        }
        String result = value.trim().replaceAll("/+$", "");
        validateBaseUrl(result);
        return result;
    }

    private void validateBaseUrl(String value) {
        try {
            URI uri = new URI(value);
            boolean supportedScheme = "https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme());
            if (!supportedScheme || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Gateway address must be a valid HTTP or HTTPS URL");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Gateway address must be a valid HTTP or HTTPS URL");
        }
    }

    @FunctionalInterface
    public interface StreamListener {
        void onDelta(String content) throws IOException;
    }

    public static class StreamResponse {
        private String id;
        private String model;
        private String finishReason;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }
}
