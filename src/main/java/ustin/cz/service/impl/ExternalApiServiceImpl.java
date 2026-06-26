package ustin.cz.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import ustin.cz.service.ExternalApiService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiServiceImpl implements ExternalApiService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Getter
    @Setter
    private String bearer;

    // ✅ Время истечения токена
    private Instant tokenExpiryTime;

    // ✅ Время жизни токена (55 минут, так как обычно 60 минут)
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(55);

    @Override
    @Retryable(
            retryFor = RuntimeException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String getBearer() {
        log.info("Запрос нового bearer токена");

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
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String newBearer = extractAccessToken(response.body());
                this.bearer = newBearer;

                // ✅ Устанавливаем время истечения токена
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
            throw new RuntimeException("Ошибка соединения с сервером авторизации", e);
        }
    }

    /**
     * ✅ Проверяет, истек ли токен
     */
    private boolean isTokenExpired() {
        return bearer == null ||
                tokenExpiryTime == null ||
                Instant.now().isAfter(tokenExpiryTime);
    }

    /**
     * ✅ Обновляет токен если истек
     */
    private synchronized void refreshTokenIfNeeded() {
        if (isTokenExpired()) {
            log.info("Токен истек или отсутствует, обновляем...");
            getBearer();
        }
    }

    @Override
    public String sendToCisesInfo(String body) {
        refreshTokenIfNeeded();

        if (bearer == null || bearer.isEmpty()) {
            throw new RuntimeException("Bearer токен не получен");
        }

        log.info("Отправка запроса к CisesInfo API");
        log.debug("Тело запроса: {}", body);

        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://markirovka.crpt.ru/api/v3/true-api/cises/info"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bearer)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            log.info("Статус ответа: {}", statusCode);
            log.debug("Тело ответа: {}", responseBody);

            // ✅ Если токен протух (401), обновляем и повторяем запрос
            if (statusCode == 401) {
                log.warn("Токен невалиден (401), обновляем и пробуем снова");
                getBearer(); // Обновляем токен

                // Повторяем запрос с новым токеном
                var retryRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://markirovka.crpt.ru/api/v3/true-api/cises/info"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + bearer)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .timeout(Duration.ofSeconds(60))
                        .build();

                var retryResponse = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString());

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

    private String extractAccessToken(String responseBody) {
        try {
            var jsonNode = objectMapper.readTree(responseBody);

            if (!jsonNode.has("access_token")) {
                log.error("Ответ не содержит access_token: {}", responseBody);
                throw new RuntimeException("Неверный формат ответа авторизации");
            }

            String token = jsonNode.get("access_token").asString();
            log.info("Токен извлечен успешно. Длина: {}", token.length());
            return token;

        } catch (Exception e) {
            log.error("Ошибка парсинга ответа авторизации: {}", responseBody, e);
            throw new RuntimeException("Ошибка парсинга ответа", e);
        }
    }

    /**
     * ✅ Проверяет валидность токена
     */
    public boolean isTokenValid() {
        return bearer != null &&
                !bearer.isEmpty() &&
                tokenExpiryTime != null &&
                Instant.now().isBefore(tokenExpiryTime);
    }

    /**
     * ✅ Получает оставшееся время жизни токена в секундах
     */
    public long getTokenRemainingTimeSeconds() {
        if (tokenExpiryTime == null) {
            return 0;
        }
        long remaining = Duration.between(Instant.now(), tokenExpiryTime).getSeconds();
        return Math.max(0, remaining);
    }
}