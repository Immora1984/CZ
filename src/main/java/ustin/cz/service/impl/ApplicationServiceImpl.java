package ustin.cz.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.*;
import ustin.cz.exception.FileProcessException;
import ustin.cz.exception.UserTaskLimiterException;
import ustin.cz.service.ApplicationService;
import ustin.cz.service.event.Event;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationEventPublisher eventPublisher;
    private final UserTaskLimiter userTaskLimiter;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<UUID, RequestDetails> taskMap = new ConcurrentHashMap<>();
    private final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();

    @Override
    public Response process(MultipartFile file, ReportType reportType, ColumnSelection columnSelection, String sessionId) {

        if (!userTaskLimiter.canAddTask(sessionId)) {
            throw new UserTaskLimiterException.MaxLimit();
        }

        var taskId = UUID.randomUUID();

        var details = new RequestDetails();
        details.setId(taskId);
        details.setFile(file);
        details.setSessionId(sessionId);
        details.setReportType(reportType);
        details.setStatus(RequestStatus.CREATED);
        details.setContentType(file.getContentType());
        details.setFileName(file.getOriginalFilename());

        if (columnSelection != null && columnSelection.getSelectedColumns() != null
                && !columnSelection.getSelectedColumns().isEmpty()) {
            details.setSelectedColumns(columnSelection.getSelectedColumns());
        } else {
            details.setSelectedColumns(ColumnNames.getAllColumnNames());
        }

        try {
            details.setFileBytes(file.getBytes());
        } catch (IOException e) {
            log.error("Ошибка сохранения файла", e);
            throw new FileProcessException.ErrorSave();
        }

        taskMap.put(taskId, details);
        userTaskLimiter.addUserTask(sessionId);

        log.info("Задача {} создана для сессии {}", taskId, sessionId);
        log.info("Выбрано {} колонок для отображения", details.getSelectedColumns().size());

        var event = new Event(this, details.getId(), details.getReportType(), taskMap);
        eventPublisher.publishEvent(event);

        log.info("Опубликовано событие: {}", event);

        var response = new Response();
        response.setId(taskId);
        response.setSessionId(sessionId);
        response.setReportType(reportType);
        response.setStatus(RequestStatus.CREATED);

        return response;
    }

    @Override
    public Resource download(UUID taskId) {
        var details = taskMap.get(taskId);

        if (details == null) throw new UserTaskLimiterException.NotFound();
        if (details.getResource() == null) throw new UserTaskLimiterException.ResourceNotFound();
        if (details.getStatus() != RequestStatus.SUCCESS) throw new UserTaskLimiterException.NotValidStatus();

        return details.getResource();
    }

    @Override
    public ProgressInfo getProgress(String sessionId) {
        return progressMap.get(sessionId);
    }

    /**
     * Обновление прогресса и отправка клиенту
     */
    @Override
    public void updateProgress(String sessionId, ProgressInfo progress) {
        log.info("📊=== ОБНОВЛЕНИЕ ПРОГРЕССА ===");
        log.info("SessionId: {}", sessionId);
        log.info("Progress: {}%", progress.getProgress());
        log.info("Message: {}", progress.getMessage());
        log.info("Status: {}", progress.getStatus());

        progressMap.put(sessionId, progress);

        try {
            // Попробуем отправить двумя способами для теста
            // Способ 1: через /user
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/progress",
                    progress
            );
            log.info("✅ Сообщение отправлено в /queue/progress для пользователя {}", sessionId);

            // Способ 2: напрямую (для теста)
            messagingTemplate.convertAndSend(
                    "/topic/progress-" + sessionId,
                    progress
            );
            log.info("✅ Сообщение отправлено в /topic/progress-{}", sessionId);

        } catch (Exception e) {
            log.error("❌ Ошибка при отправке: ", e);
        }
    }
}