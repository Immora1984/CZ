package ustin.cz.service.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ustin.cz.component.ProgressInfo;
import ustin.cz.component.ReportType;
import ustin.cz.component.RequestStatus;
import ustin.cz.service.ApplicationService;
import ustin.cz.service.FileHandlerService;
import ustin.cz.service.event.Event;
import ustin.cz.service.handler.ReportHandler;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CisSearchReportHandlerImpl implements ReportHandler {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final FileHandlerService fileHandlerService;
    private final ApplicationService applicationService;

    @Override
    public ReportType getType() {
        return ReportType.CIS_SEARCH;
    }

    @Override
    public void handle(Event event) {
        log.info("🔍 Начало обработки CIS_SEARCH отчета для задачи: {}", event.getId());

        var details = event.getTaskMap().get(event.getId());
        if (details == null) {
            log.error("Детали задачи не найдены");
            return;
        }

        var sessionId = details.getSessionId();
        log.info("Сессия: {}", sessionId);

        try {
            // 1. Обновляем статус
            details.setStatus(RequestStatus.PROCESS);

            // 2. Отправляем начальный прогресс
            sendProgress(sessionId, 0, "Начало обработки CIS_SEARCH...", 0, 0);

            // 3. Конвертируем файл
            log.info("Конвертация файла...");
            sendProgress(sessionId, 10, "Конвертация файла...", 0, 0);

            var workbook = fileHandlerService.downloadAndConvert(details);

            log.info("Обработка CIS_SEARCH...");
            sendProgress(sessionId, 20, "Обработка CIS_SEARCH данных...", 0, 0);

            var jsonResult = fileHandlerService.processCisesSearch(
                    workbook,
                    sessionId,
                    applicationService
            );

            log.info("Создание Excel файла...");
            sendProgress(sessionId, 90, "Создание Excel файла...", 0, 0);

            var resource = fileHandlerService.createResourceFromResponse(
                    jsonResult,
                    details.getSelectedColumns()
            );

            // 6. Сохраняем результат
            details.setResource(resource);
            details.setStatus(RequestStatus.SUCCESS);

            // 7. Отправляем финальный результат
            sendProgress(sessionId, 100, "✅ Обработка CIS_SEARCH завершена!", 0, 0);

            // Отправляем результат через WebSocket

            simpMessagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/result",
                    Map.of(
                            "status", "COMPLETED",
                            "taskId", event.getId().toString(),
                            "message", "Обработка CIS_SEARCH завершена успешно!"
                    )
            );


            log.info("✅ Обработка CIS_SEARCH отчета завершена для задачи: {}", event.getId());

        } catch (Exception e) {
            log.error("❌ Ошибка при обработке CIS_SEARCH: {}", e.getMessage(), e);

            // Отправляем ошибку
            sendError(sessionId, e.getMessage());

            details.setStatus(RequestStatus.ERROR);
        }
    }

    private void sendProgress(String sessionId, int progress, String message, int currentBatch, int totalBatches) {
        if (applicationService == null) {
            log.warn("ApplicationService is null, cannot send progress");
            return;
        }

        var progressInfo = ProgressInfo.builder()
                .status(progress >= 100 ? RequestStatus.SUCCESS : RequestStatus.PROCESS)
                .progress(progress)
                .currentBatch(currentBatch)
                .totalBatches(totalBatches)
                .message(message)
                .build();

        log.info("📊 Отправка прогресса: {}% - {}", progress, message);
        applicationService.updateProgress(sessionId, progressInfo);
    }

    private void sendError(String sessionId, String errorMessage) {
        if (applicationService == null) {
            log.warn("ApplicationService is null, cannot send error");
            return;
        }

        ProgressInfo progressInfo = ProgressInfo.builder()
                .status(RequestStatus.ERROR)
                .progress(0)
                .message("❌ Ошибка: " + errorMessage)
                .errorDetails(errorMessage)
                .build();

        applicationService.updateProgress(sessionId, progressInfo);
    }
}