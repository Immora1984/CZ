package ustin.cz.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ustin.cz.component.RequestStatus;
import ustin.cz.service.FileHandlerService;
import ustin.cz.service.impl.UserTaskLimiter;
import ustin.cz.util.ByteArrayMultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class CZEventListener {

    private final UserTaskLimiter userTaskLimiter;
    private final FileHandlerService fileHandlerService;

    @Async
    @EventListener
    public void handleEvent(Event event) {
        log.info("Событие получено: Task ID: {}, File: {}", event.getId(), event.getReportType());

        var taskId = event.getId();
        var details = event.getTaskMap().get(taskId);
        if (details == null) {
            log.error("Задача {} не найдена", taskId);
            return;
        }

        var sessionId = details.getSessionId();

        log.info("Асинхронная обработка начата. Task ID: {}, Session: {}, File: {}",
                taskId, sessionId, details.getFileName());

        details.setStatus(RequestStatus.PROCESS);

        try {
            var file = new ByteArrayMultipartFile(
                    details.getFileBytes(),
                    details.getFileName()
            );

            try (var workbook = fileHandlerService.downloadAndConvert(file)) {
                log.info("Workbook создан. Количество листов: {}", workbook.getNumberOfSheets());

                var jsonResult = fileHandlerService.processCisesInfo(workbook, details);

                details.setResult(jsonResult);
                details.setStatus(RequestStatus.SUCCESS);
                log.info("Асинхронная обработка завершена. Task ID: {}", taskId);

            }
        } catch (Exception e) {
            details.setStatus(RequestStatus.ERROR);
            details.setErrorMessage(e.getMessage());
            log.error("Ошибка при асинхронной обработке. Task ID: {}", taskId, e);

        } finally {
            userTaskLimiter.removeUserTask(sessionId);
        }
    }
}
