package ustin.cz.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileHandlerService {

    Workbook downloadAndConvert(MultipartFile file);

    String processCisesInfo(Workbook workbook);

    Resource createExcelResourceFromResponse(String jsonResponse);
}
