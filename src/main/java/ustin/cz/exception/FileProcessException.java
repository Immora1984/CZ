package ustin.cz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;

public class FileProcessException extends ErrorResponseException {
    public FileProcessException(HttpStatusCode statusCode) {
        super(statusCode);
    }

    public static class ErrorSave extends FileProcessException {
        public ErrorSave() { super(HttpStatus.BAD_REQUEST);}
    }
}
