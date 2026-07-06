package ustin.cz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;

public class UserTaskLimiterException extends ErrorResponseException {
    public UserTaskLimiterException(HttpStatusCode statusCode) {
        super(statusCode);
    }

    public static class MaxLimit extends UserTaskLimiterException {
        public MaxLimit() { super(HttpStatus.BAD_REQUEST);}
    }

    public static class NotFound extends UserTaskLimiterException {
        public NotFound() { super(HttpStatus.NOT_FOUND);}
    }

    public static class ResourceNotFound extends UserTaskLimiterException {
        public ResourceNotFound() { super(HttpStatus.NOT_FOUND);}
    }

    public static class NotValidStatus extends UserTaskLimiterException {
        public NotValidStatus() { super(HttpStatus.BAD_REQUEST);}
    }
}
