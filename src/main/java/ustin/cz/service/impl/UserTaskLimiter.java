package ustin.cz.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class UserTaskLimiter {

    private static final int MAX_TASKS_PER_USER = 3;
    private final Map<String, AtomicInteger> sessionTasks = new ConcurrentHashMap<>();

    public boolean canAddTask(String sessionId) {
        var count = sessionTasks.get(sessionId);
        return count == null || count.get() < MAX_TASKS_PER_USER;
    }

    public void addUserTask(String sessionId) {
        sessionTasks.computeIfAbsent(sessionId, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public void removeUserTask(String sessionId) {
        var count = sessionTasks.get(sessionId);
        if (count != null) {
            int newCount = count.decrementAndGet();
            if (newCount == 0) {
                sessionTasks.remove(sessionId);
            }
        }
    }

    public int getActiveTaskCount(String sessionId) {
        var count = sessionTasks.get(sessionId);
        return count != null ? count.get() : 0;
    }

    public int getMaxTasksPerUser() {
        return MAX_TASKS_PER_USER;
    }
}