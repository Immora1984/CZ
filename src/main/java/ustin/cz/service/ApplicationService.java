package ustin.cz.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.ProgressInfo;
import ustin.cz.component.Response;
import ustin.cz.component.ReportType;
import ustin.cz.component.ColumnSelection;

import java.util.UUID;

public interface ApplicationService {

    Response process(MultipartFile file, ReportType reportType, ColumnSelection columnSelection, String sessionId);

    Resource download(UUID id);

    ProgressInfo getProgress(String sessionId);

    void removeTask(UUID taskId);
}
