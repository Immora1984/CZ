package ustin.cz.service.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ustin.cz.component.*;
import ustin.cz.component.websocket.ProgressInfo;
import ustin.cz.service.FileHandlerService;
import ustin.cz.service.WebSocketService;
import ustin.cz.service.event.Event;
import ustin.cz.service.handler.ReportHandler;
import ustin.cz.service.impl.UserTaskLimiter;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CisInfoReportHandlerImpl implements ReportHandler {

    private final FileHandlerService fileHandlerService;
    private final WebSocketService webSocketService;
    private final ProgressMap progressMap;
    private final UserTaskLimiter userTaskLimiter;

    @Override
    public ReportType getType() {
        return ReportType.CIS_INFO;
    }

    @Override
    public void handle(Event event) {
        var details = event.getRequestDetails();
        var sessionId = details.getSessionId();

        try {
            details.setStatus(RequestStatus.PROCESS);

            var jsonResult = fileHandlerService.workbookToJson(
                    fileHandlerService.downloadAndConvert(details),
                    sessionId,
                    details.getId()
            );

            var resource = fileHandlerService.createResourceFromResponse(
                    jsonResult,
                    details.getSelectedColumns(),
                    details.getReportType()
            );

            details.setResource(resource);
            details.setStatus(RequestStatus.SUCCESS);

            updateProgress(sessionId, p -> {
                p.setStatus(RequestStatus.SUCCESS);
                p.setCurrentBatch(p.getTotalBatches());
                p.setErrorDetails(null);
            });

            log.info("✅ Обработка {} отчета завершена для задачи: {}", getType(), details.getId());

        } catch (Exception e) {
            log.error("❌ Ошибка при обработке {}: {}", getType(), e.getMessage(), e);

            details.setStatus(RequestStatus.ERROR);

            updateProgress(sessionId, p -> {
                p.setStatus(RequestStatus.ERROR);
                p.setErrorDetails(e.getMessage());
            });

        } finally {
            userTaskLimiter.removeUserTask(sessionId);
            progressMap.removeProgress(sessionId);
        }
    }

    private void updateProgress(String sessionId, Consumer<ProgressInfo> updater) {
        var progress = progressMap.getProgressMap().get(sessionId);
        if (progress != null) {
            updater.accept(progress);
            progressMap.getProgressMap().put(sessionId, progress);
            webSocketService.sendMessage(sessionId, progress);
        }
    }
}