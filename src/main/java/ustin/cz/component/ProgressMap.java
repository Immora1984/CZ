package ustin.cz.component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ProgressMap {

    @Getter
    private final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();

    public void removeProgress(String sessionId) {
        progressMap.remove(sessionId);
        log.debug("Прогресс для сессии {} удалён", sessionId);
    }
}
