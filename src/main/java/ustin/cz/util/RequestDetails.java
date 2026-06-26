package ustin.cz.util;

import lombok.Getter;
import lombok.Setter;
import ustin.cz.component.ReportType;
import ustin.cz.component.RequestStatus;

import java.util.UUID;

@Getter
@Setter
public class RequestDetails {
    private UUID id;
    private String fileName;
    private String originalFileName;
    private byte[] fileBytes;
    private String sessionId;
    private RequestStatus status;
    private ReportType reportType;
    private String contentType;
}
