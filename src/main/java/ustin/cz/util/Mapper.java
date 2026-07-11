package ustin.cz.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ustin.cz.component.RequestDetails;
import ustin.cz.component.websocket.Response;

@Slf4j
@Component
public class Mapper {

    public Response toResponse(RequestDetails requestDetails) {
        return Response.builder()
                .id(requestDetails.getId())
                .status(requestDetails.getStatus())
                .reportType(requestDetails.getReportType())
                .build();
    }
}
