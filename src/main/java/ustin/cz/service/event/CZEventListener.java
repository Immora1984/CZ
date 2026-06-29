package ustin.cz.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ustin.cz.component.RequestStatus;
import ustin.cz.service.FileHandlerService;

@Slf4j
@Component
@RequiredArgsConstructor
public class CZEventListener {

    private final FileHandlerService fileHandlerService;

    @Async
    @EventListener
    public void handleEvent(Event event) {
        log.info("Событие получено: Task ID: {}", event.getId());

        var multipartFile = event.getTaskMap().get(event.getId()).getFile();
        var jsonResult = fileHandlerService.processCisesInfo(fileHandlerService.downloadAndConvert(multipartFile));
        var resource = fileHandlerService.createExcelResourceFromResponse(jsonResult);

        event.getTaskMap().get(event.getId()).setResource(resource);
        event.getTaskMap().get(event.getId()).setStatus(RequestStatus.SUCCESS);

        log.info("Ресурс сохранён");
    }
}