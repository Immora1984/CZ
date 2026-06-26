package ustin.cz.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.component.Response;
import ustin.cz.component.ReportType;
import ustin.cz.service.CZSearchService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CZController {

    private final CZSearchService CZSearchService;

    @PostMapping
    Response process(@RequestPart @Valid @NotNull MultipartFile file,
                     @Valid @NotNull ReportType reportType,
                     HttpServletRequest httpServletRequest) {
        return CZSearchService.getExcel(file, reportType, httpServletRequest.getSession().getId());
    }

    @GetMapping("/{id}")
    Response check(@PathVariable UUID id) {
        return CZSearchService.check(id);
    }

    @GetMapping(path = "/download/{id}")
    ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {
        return ResponseEntity.ok(CZSearchService.download(id));
    }
}
