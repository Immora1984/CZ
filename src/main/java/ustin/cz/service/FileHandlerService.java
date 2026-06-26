package ustin.cz.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.multipart.MultipartFile;

public interface FileHandlerService {

    Workbook downloadAndConvert(MultipartFile file);

    void processCisesInfo(Workbook workbook);
}
