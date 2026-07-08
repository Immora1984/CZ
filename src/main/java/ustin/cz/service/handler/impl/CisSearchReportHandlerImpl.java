package ustin.cz.service.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ustin.cz.component.ProgressInfo;
import ustin.cz.component.ReportType;
import ustin.cz.component.RequestStatus;
import ustin.cz.component.Response;
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
        var details = event.getRequestDetails();

        var sessionId = details.getSessionId();

        try {
            details.setStatus(RequestStatus.PROCESS);

            var jsonResult = fileHandlerService.processCisesSearch(
                    fileHandlerService.downloadAndConvert(details),
                    sessionId,
                    applicationService
            );

            var resource = fileHandlerService.createResourceFromResponse(
                    jsonResult,
                    details.getSelectedColumns()
            );

            details.setResource(resource);
            details.setStatus(RequestStatus.SUCCESS);

            simpMessagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/result",
                    Response.builder()
                            .id(details.getId())
                            .reportType(getType())
                            .status(details.getStatus())
                            .sessionId(sessionId)
                            .build());


            log.info("✅ Обработка CIS_SEARCH отчета завершена для задачи: {}", details.getId());

        } catch (Exception e) {
            log.error("❌ Ошибка при обработке CIS_SEARCH: {}", e.getMessage(), e);

            sendError(sessionId, e.getMessage());

            details.setStatus(RequestStatus.ERROR);
        }
    }

    private void sendError(String sessionId, String errorMessage) {

        var progressInfo = ProgressInfo.builder()
                .status(RequestStatus.ERROR)
                .progress(0)

                .errorDetails(errorMessage)
                .build();

        applicationService.updateProgress(sessionId, progressInfo);
    }
}