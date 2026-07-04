package ustin.cz.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.RequestStatus;
import ustin.cz.component.Response;
import ustin.cz.component.ReportType;
import ustin.cz.excel.ColumnNames;
import ustin.cz.excel.ColumnSelectionDto;
import ustin.cz.service.ApplicationService;
import ustin.cz.service.event.Event;
import ustin.cz.util.Mapper;
import ustin.cz.util.RequestDetails;

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
    private final Mapper mapper;

    private final Map<UUID, RequestDetails> taskMap = new ConcurrentHashMap<>();

    @Override
    public Response process(MultipartFile file, ReportType reportType, ColumnSelectionDto columnSelection, String sessionId) {

        if (!userTaskLimiter.canAddTask(sessionId)) {
            throw new RuntimeException(
                    String.format("У вас уже %d активных задач (макс: %d)",
                            userTaskLimiter.getActiveTaskCount(sessionId), userTaskLimiter.getMaxTasksPerUser())
            );
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

        // Устанавливаем выбранные колонки (по умолчанию все)
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
            throw new RuntimeException("Не удалось сохранить файл", e);
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
        response.setReportType(reportType);
        response.setStatus(RequestStatus.CREATED);

        return response;
    }

    @Override
    public Response check(UUID taskId) {
        var details = taskMap.get(taskId);
        if (details == null) {
            var response = new Response();
            response.setId(taskId);
            response.setStatus(RequestStatus.ERROR);
            return response;
        }
        return mapper.toResponse(details);
    }

    @Override
    public Resource download(UUID taskId) {
        var details = taskMap.get(taskId);

        if (details == null) throw new RuntimeException("Задача не найдена");
        if (details.getResource() == null) throw new RuntimeException("Результат не найден");
        if (details.getStatus() != RequestStatus.SUCCESS) throw new RuntimeException("Задача еще не завершена или завершилась с ошибкой");

        return details.getResource();
    }
}