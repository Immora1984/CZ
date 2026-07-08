package ustin.cz.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import ustin.cz.exception.ExternalApiException;
import ustin.cz.service.ExternalApiService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiServiceImpl implements ExternalApiService {

    @Value("${external-api.bearer-url}")
    private String bearerUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicReference<String> bearer = new AtomicReference<>();

    @Override
    @Retryable(retryFor = {ExternalApiException.NotAuthenticated.class, ExternalApiException.UnavailableException.class})
    public String sendToCisesInfo(String body) {
        try {
            var response = httpClient.send(createRequest(body), BodyHandlers.ofString());

            if (response.statusCode() == 401) throw new ExternalApiException.NotAuthenticated();

            return response.body();
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при отправке запроса к CisesInfo API", e);
            throw new ExternalApiException.UnavailableException();
        }
    }

    private HttpRequest createRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create("https://markirovka.crpt.ru/api/v3/true-api/cises/info"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getBearer())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Retryable(
            retryFor = RuntimeException.class,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private String getBearerToken() {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(bearerUrl))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=password&username=p.serebrennikov@call-service.ru&password=BvHx4a07*3c/"))
                .header("Authorization", "Basic Y3JwdC1zZXJ2aWNlOnNlY3JldA==")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", "JSESSIONID=E755DA057E0DBAF9296A92FED35285A8")
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
            var jsonNode = objectMapper.readTree(responseBody);

            if (!jsonNode.has("access_token")) throw new ExternalApiException.BearerException();

            return jsonNode.get("access_token").asString();
        } catch (Exception e) {
            log.error("Ошибка парсинга ответа авторизации: {}", responseBody, e);
            throw new RuntimeException("Ошибка парсинга ответа", e);
        }
    }
}