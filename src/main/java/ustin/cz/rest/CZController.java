package ustin.cz.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.*;
import ustin.cz.service.ApplicationService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CZController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<Response> processFile(
            @RequestParam MultipartFile file,
            @RequestParam ReportType reportType,
            @RequestParam(required = false) ColumnSelection columnSelection,
            @RequestHeader(value = "X-Session-ID") String sessionIdHeader) {

        log.info("📤 Файл: {}, размер: {} байт", file.getOriginalFilename(), file.getSize());

        Response response = applicationService.process(
                file,
                reportType,
                columnSelection,
                sessionIdHeader
        );

        return ResponseEntity.ok(response);
    }

    @MessageMapping("/progress")
    public void getCurrentProgress(@Payload Map<String, String> payload) {
        System.out.println(payload.size());
        var sessionId = payload != null && payload.containsKey("sessionId")
                ? payload.get("sessionId")
                : UUID.randomUUID().toString();

        log.info("📌 Запрос текущего прогресса для сессии: {}", sessionId);

        var progress = applicationService.getProgress(sessionId);

        if (progress != null) {
            log.info("📊 Найден прогресс: {}% - {}", progress.getProgress(), progress.getMessage());
            simpMessagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/progress",
                    progress
            );
        } else {
            log.info("⚠️ Прогресс НЕ найден для сессии: {}", sessionId);
            simpMessagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/progress",
                    Map.of(
                            "status", "NO_ACTIVE_PROCESS",
                            "message", "Нет активного процесса обработки"
                    )
            );
        }
    }

    @GetMapping("/download/{id}")
    ResponseEntity<Resource> getResult(@PathVariable UUID id) {
        log.info("📥 Запрос на скачивание: {}", id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result.xlsx\"")
                .body(applicationService.download(id));
    }
}