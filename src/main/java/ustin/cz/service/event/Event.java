package ustin.cz.service.event;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;
import ustin.cz.component.ReportType;
import ustin.cz.util.RequestDetails;

import java.util.Map;
import java.util.UUID;

@Getter
@ToString
public class Event extends ApplicationEvent {

    private final UUID id;
    private final ReportType reportType;
    private final Map<UUID, RequestDetails> taskMap;

    public Event(Object source, UUID id, ReportType reportType, Map<UUID, RequestDetails> taskMap) {
        super(source);
        this.id = id;
        this.reportType = reportType;
        this.taskMap = taskMap;
    }
}