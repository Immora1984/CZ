package ustin.cz.service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ustin.cz.component.ReportType;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParserHandlerFactory {

    private final List<ParserHandler> handlers;

    public ParserHandler getHandler(ReportType type) {
        return handlers.stream()
                .filter(handler -> handler.getType() == type)
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Не найден обработчик для типа: {}", type);
                    return new RuntimeException("Не найден обработчик для типа: " + type);
                });
    }
}
