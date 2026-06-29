package ustin.cz.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.Response;
import ustin.cz.component.ReportType;

import java.util.UUID;

public interface CZSearchService {

    Response process(MultipartFile file, ReportType reportType, String sessionId);

    Response check(UUID id);

    Resource download(UUID id);

}
