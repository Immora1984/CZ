package ustin.cz.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ustin.cz.exception.ExternalApiException;
import ustin.cz.service.ExternalApiService;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiServiceImpl implements ExternalApiService {

    @Value("${external-api.bearer-url}")
    private String bearerUrl;
    @Value("${external-api.cises-info}")
    private String cisesInfoUrl;
    @Value("${external-api.credentials}")
    private String credentials;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicReference<String> bearer = new AtomicReference<>();

    @Override
    @Retryable(retryFor = {ExternalApiException.NotAuthenticated.class, ExternalApiException.UnavailableException.class})
    public String sendToCisesInfo(String body) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(cisesInfoUrl))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getBearer())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(60))
                .build();
        try {
            var response = httpClient.send(request, BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                this.bearer.set(null);
                throw new ExternalApiException.NotAuthenticated();
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при отправке запроса к CisesInfo API", e);
            throw new ExternalApiException.UnavailableException();
        }
    }

    @Retryable(retryFor = RuntimeException.class, backoff = @Backoff(delay = 1000, multiplier = 2))
    private String getBearerToken() {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(bearerUrl))
                .POST(HttpRequest.BodyPublishers.ofString(credentials))
                .header(HttpHeaders.AUTHORIZATION, "Basic Y3JwdC1zZXJ2aWNlOnNlY3JldA==")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header(HttpHeaders.COOKIE, "JSESSIONID=E755DA057E0DBAF9296A92FED35285A8")
                .timeout(Duration.ofSeconds(30))
                .build();

        try {
            var response = httpClient.send(request, BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var newBearer = extractAccessToken(response.body());
                this.bearer.set(newBearer);
                return newBearer;
            } else {
                log.error("Ошибка получения токена: статус {}, тело ответа {}", response.statusCode(), response.body());
                throw new ExternalApiException.BearerException();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при запросе токена", e);
            throw new ExternalApiException.ApiException();
        }
    }

    private String getBearer() {
        if (bearer.get() == null) this.bearer.set(getBearerToken());
        return this.bearer.get();
    }

    private String extractAccessToken(String responseBody) {
        try {
            return Optional.ofNullable(objectMapper.readTree(responseBody).get("access_token"))
                    .map(JsonNode::asString)
                    .orElseThrow(ExternalApiException.BearerException::new);
        } catch (Exception e) {
            log.error("Ошибка парсинга ответа авторизации: {}", responseBody, e);
            throw new RuntimeException("Ошибка парсинга ответа", e);
        }
    }
}