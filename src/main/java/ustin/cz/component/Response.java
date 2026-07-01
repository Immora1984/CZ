package ustin.cz.component;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Response {

    private UUID id;
    private RequestStatus status;
    private ReportType reportType;

}