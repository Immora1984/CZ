package ustin.cz.util;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.ReportType;
import ustin.cz.component.RequestStatus;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString(exclude = {"fileBytes", "file", "resource"})
public class RequestDetails {
    private UUID id;
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