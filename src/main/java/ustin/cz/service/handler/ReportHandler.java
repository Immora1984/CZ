package ustin.cz.service.handler;

import ustin.cz.component.ReportType;
import ustin.cz.service.event.Event;

public interface ReportHandler {

    ReportType getType();
    void handle(Event event);

}
