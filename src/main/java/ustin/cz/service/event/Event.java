package ustin.cz.service.event;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;
import ustin.cz.component.RequestDetails;

@Getter
@ToString
public class Event extends ApplicationEvent {

    private final RequestDetails requestDetails;

    public Event(Object source, RequestDetails requestDetails) {
        super(source);
        this.requestDetails = requestDetails;
    }
}