package ustin.cz.component;

import lombok.Getter;
import org.springframework.stereotype.Component;
import ustin.cz.component.websocket.ProgressInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProgressMap {

    @Getter
    private final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();

    public void removeProgress(String sessionId) {progressMap.remove(sessionId);}
}
