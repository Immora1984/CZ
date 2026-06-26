package ustin.cz.exception;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.time.Instant;

public class ExternalApiException extends ErrorResponseException {

    private static MessageSource messageSource;

    public ExternalApiException(HttpStatus status) {
        super(status);
        getBody().setProperty("timestamp", Instant.now());
    }

    public static class ApiException extends ExternalApiException {
        public ApiException() {
            super(HttpStatus.SERVICE_UNAVAILABLE);
        }

        public ApiException(Throwable cause) {
            this();
            initCause(cause);
        }
    }

    public static class BearerException extends ExternalApiException {
        public BearerException() {super(HttpStatus.UNAUTHORIZED);}
    }

    public static class UnavailableException extends ExternalApiException {
        public UnavailableException() {super(HttpStatus.SERVICE_UNAVAILABLE);}

        public UnavailableException(Throwable cause) {
            this();
            initCause(cause);
        }
    }
}