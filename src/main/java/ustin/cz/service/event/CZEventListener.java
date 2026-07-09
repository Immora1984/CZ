package ustin.cz.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ustin.cz.service.handler.ReportHandlerFactory;

@Slf4j
@Component
@RequiredArgsConstructor
public class CZEventListener {

    private final ReportHandlerFactory reportHandlerFactory;

    @EventListener
    @Async("taskExcelExecutor")
    public void handleEvent(Event event) {
        log.info("Событие получено: Task ID: {}", event.getRequestDetails().getId());
        reportHandlerFactory.getHandler(event.getRequestDetails().getReportType()).handle(event);
    }
}