package ustin.cz.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.Response;
import ustin.cz.component.ReportType;
import ustin.cz.excel.ColumnSelectionDto;
import ustin.cz.service.ApplicationService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CZController {

    private final ApplicationService ApplicationService;

    @PostMapping
    Response process(@RequestPart @Valid @NotNull MultipartFile file,
                     @Valid @NotNull ReportType reportType,
                     @RequestPart(required = false) ColumnSelectionDto columnSelection,
                     HttpServletRequest httpServletRequest) {
        return ApplicationService.process(file, reportType, columnSelection, httpServletRequest.getSession().getId());
    }

    @GetMapping("/{id}")
    Response check(@PathVariable UUID id) {
        return ApplicationService.check(id);
    }

    @GetMapping(path = "/download/{id}")
    ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {
        var resource = ApplicationService.download(id);
        var filename = resource.getFilename();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(resource);
    }
}
