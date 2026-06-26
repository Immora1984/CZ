package ustin.cz.service;

import tools.jackson.databind.JsonNode;

public interface ExternalApiService {

    String getBearer();

    String sendToCisesInfo(String body);
}
