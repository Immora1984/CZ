package ustin.cz.service;


public interface ExternalApiService {

    /**
     * ✅ Получает токен из внешнего API авторизации
     */
    String getTokenFromExternalApi();

    /**
     * ✅ Отправляет запрос к CisesInfo API
     */
    String sendToCisesInfo(String body);

    /**
     * ✅ Получает текущий валидный токен (автоматически обновляет если истек)
     */
    String getCurrentBearerToken();

    /**
     * ✅ Проверяет валидность токена
     */
    boolean isTokenValid();

    /**
     * ✅ Получает оставшееся время жизни токена в секундах
     */
    long getTokenRemainingTimeSeconds();
}