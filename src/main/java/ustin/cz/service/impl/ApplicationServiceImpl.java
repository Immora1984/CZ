package ustin.cz.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.*;
import ustin.cz.component.websocket.Response;
import ustin.cz.exception.FileProcessException;
import ustin.cz.exception.UserTaskLimiterException;
import ustin.cz.service.ApplicationService;
import ustin.cz.service.WebSocketService;
import ustin.cz.service.event.Event;
import ustin.cz.util.Mapper;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationEventPublisher eventPublisher;
    private final WebSocketService webSocketService;
    private final UserTaskLimiter userTaskLimiter;
    private final ProgressMap progressMap;
    private final TaskMap taskMap;
    private final Mapper mapper;

    @Override
    public Response process(MultipartFile file, ReportType reportType, ColumnSelection columnSelection, String sessionId) {

        if (sessionId == null || sessionId.isEmpty()) throw new IllegalArgumentException("Session ID не может быть пустым");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Файл не может быть пустым");
        if (!userTaskLimiter.canAddTask(sessionId)) throw new UserTaskLimiterException.MaxLimit();

        var details = new RequestDetails();
        details.setFile(file);
        details.setSessionId(sessionId);
        details.setReportType(reportType);
        details.setStatus(RequestStatus.CREATED);
        details.setContentType(file.getContentType());
        details.setFileName(file.getOriginalFilename());
        details.setSelectedColumns(Optional.ofNullable(columnSelection)
                .map(ColumnSelection::getSelectedColumns)
                .orElse(ColumnNames.getAllNames()));
        try {
            details.setFileBytes(file.getBytes());
        } catch (IOException e) {
            details.setStatus(RequestStatus.ERROR);
            taskMap.getTaskMap().put(details.getId(), details);
            log.error("Ошибка сохранения файла", e);
            throw new FileProcessException.ErrorSave();
        }

        taskMap.getTaskMap().put(details.getId(), details);
        userTaskLimiter.addUserTask(sessionId);

        eventPublisher.publishEvent(new Event(this, details));

        return mapper.toResponse(details);
    }

    @Override
    public Resource download(UUID taskId) {
        var details = taskMap.getTaskMap().get(taskId);

        if (details == null) throw new UserTaskLimiterException.NotFound();
        if (details.getResource() == null) throw new UserTaskLimiterException.ResourceNotFound();
        if (details.getStatus() != RequestStatus.SUCCESS) throw new UserTaskLimiterException.NotValidStatus();

        return details.getResource();
    }

    @Override
    public void getProgress(String sessionId) {
        Optional.ofNullable(progressMap.getProgressMap().get(sessionId))
                .ifPresent(progress -> webSocketService.sendMessage(sessionId, progress));
    }
}