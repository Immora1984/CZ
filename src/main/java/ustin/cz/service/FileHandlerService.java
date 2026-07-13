package ustin.cz.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.Resource;
import ustin.cz.component.ReportType;
import ustin.cz.component.RequestDetails;

import java.util.Set;
import java.util.UUID;


public interface FileHandlerService {

    Workbook downloadAndConvert(RequestDetails requestDetails);

    String workbookToJson(Workbook workbook, String sessionId, UUID taskId);

    Resource createResourceFromResponse(String jsonResponse, Set<String> selectedColumns, ReportType reportType);
}
