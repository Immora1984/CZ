package ustin.cz.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiServiceImpl implements ExternalApiService {

    @Getter
    @Setter
    private String bearer;
    private Instant tokenExpiryTime;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);

    @Override
    @Retryable(
            retryFor = RuntimeException.class,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String getTokenFromExternalApi() {
        log.info("Запрос нового bearer токена из внешнего API");

        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://markirovka.crpt.ru/oauth/token"))
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
                this.bearer = newBearer;
                this.tokenExpiryTime = Instant.now().plus(TOKEN_LIFETIME);

                log.info("Bearer токен успешно получен. Истекает: {}", tokenExpiryTime);
                return newBearer;
            } else {
                log.error("Ошибка получения токена: статус {}, тело ответа {}",
                        response.statusCode(), response.body());
                throw new RuntimeException("Ошибка получения токена. Код: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Ошибка при запросе токена", e);
            throw new ExternalApiException.ApiException();
        }
    }

    private boolean isTokenExpired() {
        return bearer == null || tokenExpiryTime == null || Instant.now().isAfter(tokenExpiryTime);
    }

    private synchronized String refreshTokenIfNeeded() {
        if (isTokenExpired()) {
            log.info("Токен истек или отсутствует, обновляем...");
            return getTokenFromExternalApi();
        }
        return bearer;
    }

    public String getCurrentBearerToken() {
        return refreshTokenIfNeeded();
    }

    @Override
    public String sendToCisesInfo(String body) {
        var currentToken = getCurrentBearerToken();

        if (currentToken == null || currentToken.isEmpty()) {
            throw new RuntimeException("Bearer токен не получен");
        }

        log.info("Отправка запроса к CisesInfo API");
        log.debug("Тело запроса: {}", body);

        try {
            var response = httpClient.send(doRequest(currentToken, body), BodyHandlers.ofString());

            int statusCode = response.statusCode();
            var responseBody = response.body();

            log.info("Статус ответа: {}", statusCode);
            log.debug("Тело ответа: {}", responseBody);

            if (statusCode == 401) {
                log.warn("Токен невалиден (401), обновляем и пробуем снова");

                getTokenFromExternalApi();

                var retryResponse = httpClient.send(doRequest(getCurrentBearerToken(), body), BodyHandlers.ofString());

                if (retryResponse.statusCode() == 200) {
                    log.info("Запрос успешно выполнен после обновления токена");
                    return retryResponse.body();
                } else {
                    log.error("Ошибка API после обновления токена: {}", retryResponse.statusCode());
                    throw new RuntimeException("Ошибка API: " + retryResponse.statusCode());
                }
            }

            if (statusCode >= 400) {
                log.error("Ошибка API: {} - {}", statusCode, responseBody);
                throw new RuntimeException("API вернул ошибку: " + statusCode + " - " + responseBody);
            }

            return responseBody;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Ошибка при отправке запроса к CisesInfo API", e);
            throw new RuntimeException("Ошибка соединения с API", e);
        } catch (Exception e) {
            log.error("Неожиданная ошибка", e);
            throw new RuntimeException("Ошибка при выполнении запроса", e);
        }
    }

    private HttpRequest doRequest(String token, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create("https://markirovka.crpt.ru/api/v3/true-api/cises/info"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    private String extractAccessToken(String responseBody) {
        try {
            var jsonNode = objectMapper.readTree(responseBody);

            if (!jsonNode.has("access_token")) {
                log.error("Ответ не содержит access_token: {}", responseBody);
                throw new RuntimeException("Неверный формат ответа авторизации");
            }

            var token = jsonNode.get("access_token").asString();
            log.info("Токен извлечен успешно. Длина: {}", token.length());
            return token;

        } catch (Exception e) {
            log.error("Ошибка парсинга ответа авторизации: {}", responseBody, e);
            throw new RuntimeException("Ошибка парсинга ответа", e);
        }
    }
}