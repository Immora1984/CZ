package ustin.cz.component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class RequestDetails {
    private UUID id = UUID.randomUUID();
    private String fileName;
    private byte[] fileBytes;
    private MultipartFile file;
    private Resource resource;
    private String sessionId;
    private RequestStatus status;
    private ReportType reportType;
    private String contentType;
    private Set<String> selectedColumns;
}