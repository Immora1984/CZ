package ustin.cz.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.RequestStatus;
import ustin.cz.component.Response;
import ustin.cz.component.ReportType;
import ustin.cz.service.CZSearchService;
import ustin.cz.service.FileHandlerService;
import ustin.cz.util.ByteArrayMultipartFile;
import ustin.cz.util.Mapper;
import ustin.cz.util.RequestDetails;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CZSearchServiceImpl implements CZSearchService {

    private final FileHandlerService fileHandlerService;
    private final UserTaskLimiter userTaskLimiter;
    private final Mapper mapper;

    private final Map<UUID, RequestDetails> taskMap = new ConcurrentHashMap<>();

    @Override
    public Response getExcel(MultipartFile file, ReportType reportType, String sessionId) {

        if (!userTaskLimiter.canAddTask(sessionId)) {
            int activeCount = userTaskLimiter.getActiveTaskCount(sessionId);
            throw new RuntimeException(
                    String.format("У вас уже %d активных задач (макс: %d)",
                            activeCount, userTaskLimiter.getMaxTasksPerUser())
            );
        }

        var taskId = UUID.randomUUID();

        var details = new RequestDetails();
        details.setId(taskId);
        details.setReportType(reportType);
        details.setStatus(RequestStatus.CREATED);
        details.setFileName(file.getOriginalFilename());
        details.setSessionId(sessionId);

        try {
            details.setFileBytes(file.getBytes());
        } catch (IOException e) {
            log.error("Ошибка сохранения файла", e);
            throw new RuntimeException("Не удалось сохранить файл", e);
        }

        taskMap.put(taskId, details);
        userTaskLimiter.addUserTask(sessionId);

        log.info("Задача {} создана для сессии {}", taskId, sessionId);

        processExcelAsync(taskId);

        var response = new Response();
        response.setId(taskId);
        response.setStatus(RequestStatus.CREATED);

        return response;
    }

    @Async("taskExcelExecutor")
    public void processExcelAsync(UUID taskId) {

        var details = taskMap.get(taskId);
        if (details == null) {
            log.error("Задача {} не найдена", taskId);
            return;
        }

        var sessionId = details.getSessionId();

        log.info("Асинхронная обработка начата. Task ID: {}, Session: {}",
                taskId, sessionId);

        details.setStatus(RequestStatus.PROCESS);

        try {
            var file = new ByteArrayMultipartFile(
                    details.getFileBytes(),
                    details.getOriginalFileName()
            );

            try (var workbook = fileHandlerService.downloadAndConvert(file)) {
                log.info("Workbook создан. Количество листов: {}", workbook.getNumberOfSheets());
                fileHandlerService.processCisesInfo(workbook);
                details.setStatus(RequestStatus.SUCCESS);
                log.info("Асинхронная обработка завершена. Task ID: {}", taskId);
            }

        } catch (Exception e) {
            details.setStatus(RequestStatus.ERROR);
            log.error("Ошибка при асинхронной обработке. Task ID: {}", taskId, e);

        } finally {
            userTaskLimiter.removeUserTask(sessionId);
        }
    }

    @Override
    public Response check(UUID taskId) {
        return mapper.toResponse(taskMap.get(taskId));
    }

    @Override
    public Resource download(UUID id) {
        return null;
    }
}
