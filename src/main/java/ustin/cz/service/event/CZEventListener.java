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

    @Async
    @EventListener
    public void handleEvent(Event event) {
        log.info("Событие получено: Task ID: {}", event.getId());

        var details = event.getTaskMap().get(event.getId());
        if (details != null) reportHandlerFactory.getHandler(event.getReportType()).handle(event);

    }
}