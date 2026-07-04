package ustin.cz.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.Resource;
import ustin.cz.util.RequestDetails;

import java.util.Set;


public interface FileHandlerService {

    Workbook downloadAndConvert(RequestDetails requestDetails);

    String processCisesInfo(Workbook workbook);

    Resource createResourceFromResponse(String jsonResponse, Set<String> selectedColumns);
}
