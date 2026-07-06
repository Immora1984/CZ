package ustin.cz.component;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileProcessRequest {
    private MultipartFile file;
    private ReportType reportType;
    private ColumnSelection columnSelection;
}