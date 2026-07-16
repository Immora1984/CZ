package ustin.cz.util;

import org.springframework.stereotype.Component;
import ustin.cz.component.RequestDetails;
import ustin.cz.component.websocket.Response;

@Component
public class Mapper {

    public Response toResponse(RequestDetails details) {
        return Response.builder()
                .id(details.getId())
                .status(details.getStatus())
                .sessionId(details.getSessionId())
                .reportType(details.getReportType())
                .build();
    }
}
