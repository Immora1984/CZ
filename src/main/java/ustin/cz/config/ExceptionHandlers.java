package ustin.cz.config;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.BindErrorUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionHandlers extends ResponseEntityExceptionHandler {
    private final MessageSource messageSource;

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        var pd = ex.updateAndGetBody(messageSource, LocaleContextHolder.getLocale());
        pd.setProperty("fields", convertFieldErrors(ex.getFieldErrors()));
        return new ResponseEntity<>(pd, HttpStatus.BAD_REQUEST);
    }

    protected Map<String, String> convertFieldErrors(List<FieldError> fieldErrors) {
        return BindErrorUtils.resolve(fieldErrors, messageSource, LocaleContextHolder.getLocale()).entrySet().stream()
                .collect(Collectors.toMap(it -> it.getKey().getField(), Map.Entry::getValue));
    }
}
