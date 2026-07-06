package ustin.cz.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ustin.cz.component.RequestDetails;
import ustin.cz.component.Response;

@Slf4j
@Component
public class Mapper {

    public Response toResponse(RequestDetails requestDetails) {
        var target = new Response();
        target.setId(requestDetails.getId());
        target.setStatus(requestDetails.getStatus());
        target.setReportType(requestDetails.getReportType());
        return target;
    }
}
