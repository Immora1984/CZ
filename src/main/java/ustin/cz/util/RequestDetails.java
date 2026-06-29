package ustin.cz.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.ReportType;
import ustin.cz.component.RequestStatus;

import java.util.UUID;

@Getter
@Setter
public class RequestDetails {
    private UUID id;
    private String fileName;          // ✅ Только одно поле для имени
    private byte[] fileBytes;
    private MultipartFile file;
    private Resource resource;
    private String sessionId;
    private RequestStatus status;
    private ReportType reportType;
    private String contentType;
}