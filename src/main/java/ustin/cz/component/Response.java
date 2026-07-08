package ustin.cz.component;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class Response {

    private UUID id;
    private RequestStatus status;
    private ReportType reportType;
    private String sessionId;

}