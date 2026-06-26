package ustin.cz.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;

import java.util.UUID;

@Getter
@Setter
public class Response {

    private UUID id;
    private RequestStatus status;
    private ReportType reportType;

}