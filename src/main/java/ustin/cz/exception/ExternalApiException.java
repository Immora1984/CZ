package ustin.cz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class ExternalApiException extends ErrorResponseException {

    public ExternalApiException(HttpStatus status) {super(status);}

    public static class ApiException extends ExternalApiException {
        public ApiException() {
            super(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public static class BearerException extends ExternalApiException {
        public BearerException() {super(HttpStatus.SERVICE_UNAVAILABLE);}
    }

    public static class UnavailableException extends ExternalApiException {
        public UnavailableException() {super(HttpStatus.SERVICE_UNAVAILABLE);}
    }

    public static class NotAuthenticated extends ExternalApiException {
        public NotAuthenticated() {super(HttpStatus.BAD_REQUEST);}
    }
}