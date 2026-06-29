package ustin.cz.service;

public interface ExternalApiService {

    String getTokenFromExternalApi();

    String sendToCisesInfo(String body);

    String getCurrentBearerToken();

}