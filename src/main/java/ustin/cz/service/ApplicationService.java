package ustin.cz.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.Response;
import ustin.cz.component.ReportType;
import ustin.cz.excel.ColumnSelectionDto;

import java.util.UUID;

public interface ApplicationService {

    Response process(MultipartFile file, ReportType reportType, ColumnSelectionDto columnSelection, String sessionId);

    Response check(UUID id);

    Resource download(UUID id);

}
