package ustin.cz.component;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskMap {

    @Getter
    private final Map<UUID, RequestDetails> taskMap = new ConcurrentHashMap<>();

    public void removeTask(UUID taskId) {taskMap.remove(taskId);}
}
