package ustin.cz.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.util.RequestDetails;

public interface FileHandlerService {

    Workbook downloadAndConvert(MultipartFile file);

    String processCisesInfo(Workbook workbook, RequestDetails requestDetails);
}
