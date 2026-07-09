package ustin.cz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;

public class WebSocketException extends ErrorResponseException {
    public WebSocketException(HttpStatusCode status) {super(status);}

    public static class SessionIdIsNotPresent extends WebSocketException{
        public SessionIdIsNotPresent() {super(HttpStatus.BAD_REQUEST);}
    }
}
