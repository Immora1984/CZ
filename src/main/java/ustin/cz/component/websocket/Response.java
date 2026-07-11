package ustin.cz.component.websocket;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ustin.cz.component.ReportType;
import ustin.cz.component.RequestStatus;

import java.util.UUID;

@Getter
@Setter
@Builder
public class Response implements Message {

    private UUID id;
    private RequestStatus status;
    private ReportType reportType;
    private String sessionId;

}