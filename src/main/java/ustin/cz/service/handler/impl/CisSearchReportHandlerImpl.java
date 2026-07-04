package ustin.cz.service.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ustin.cz.component.ReportType;
import ustin.cz.component.RequestStatus;
import ustin.cz.service.FileHandlerService;
import ustin.cz.service.event.Event;
import ustin.cz.service.handler.ReportHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class CisSearchReportHandlerImpl implements ReportHandler {

    private final FileHandlerService fileHandlerService;

    @Override
    public ReportType getType() {
        return ReportType.CIS_SEARCH;
    }

    @Override
    public void handle(Event event) {

        log.info("Обработка CIS_INFO отчета");

        event.getTaskMap().get(event.getId()).setStatus(RequestStatus.PROCESS);

        var workbook = fileHandlerService.downloadAndConvert(event.getTaskMap().get(event.getId()));
        var jsonResult = fileHandlerService.processCisesInfo(workbook);

        var details = event.getTaskMap().get(event.getId());
        var selectedColumns = details.getSelectedColumns();

        var resource = fileHandlerService.createResourceFromResponse(jsonResult, selectedColumns);

        details.setResource(resource);
        details.setStatus(RequestStatus.SUCCESS);

        log.info("Обработка CIS_INFO отчета завершена. Выведено {} колонок", selectedColumns.size());
    }
}