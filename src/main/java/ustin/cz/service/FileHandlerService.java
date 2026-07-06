package ustin.cz.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.Resource;
import ustin.cz.component.RequestDetails;

import java.util.Set;


public interface FileHandlerService {

    Workbook downloadAndConvert(RequestDetails requestDetails);

    String processCisesSearch(Workbook workbook, String sessionId, ApplicationService applicationService);

    Resource createResourceFromResponse(String jsonResponse, Set<String> selectedColumns);
}
