package ustin.cz.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class UserTaskLimiter {

    @Value("${max-task-per-user}")
    private int MAX_TASKS_PER_USER;

    private final Map<String, AtomicInteger> sessionTasks = new ConcurrentHashMap<>();

    public boolean canAddTask(String sessionId) {
        var count = sessionTasks.get(sessionId);
        return count == null || count.get() < MAX_TASKS_PER_USER;
    }

    public void addUserTask(String sessionId) {
        sessionTasks.computeIfAbsent(sessionId, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public int getActiveTaskCount(String sessionId) {
        var count = sessionTasks.get(sessionId);
        return count != null ? count.get() : 0;
    }

    public int getMaxTasksPerUser() {
        return MAX_TASKS_PER_USER;
    }
}