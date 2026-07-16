package ustin.cz.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.*;
import ustin.cz.component.websocket.Response;
import ustin.cz.exception.WebSocketException;
import ustin.cz.service.ApplicationService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CZController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<Response> processFile(@RequestParam MultipartFile file,
                                                @RequestParam ReportType reportType,
                                                @RequestHeader(value = "X-Session-ID") String sessionIdHeader,
                                                @RequestParam(required = false) ColumnSelection columnSelection) {
        return ResponseEntity.ok(applicationService.process(file, reportType, columnSelection, sessionIdHeader));
    }

    @MessageMapping("/progress")
    public void getCurrentProgress(@Payload Map<String, String> payload) {
        applicationService.getProgress(Optional.ofNullable(payload.get("sessionId"))
                .filter(s -> !s.isEmpty())
                .orElseThrow(WebSocketException.SessionIdIsNotPresent::new));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        var resource = applicationService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                        .attachment()
                        .filename(resource.getFilename())
                        .build().toString())
                .body(resource);
    }
}