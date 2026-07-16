package ustin.cz.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;
import ustin.cz.component.*;
import ustin.cz.component.ColumnNames.ColumnExtractor;
import ustin.cz.component.websocket.ProgressInfo;
import ustin.cz.service.ExternalApiService;
import ustin.cz.service.FileHandlerService;
import ustin.cz.service.WebSocketService;
import ustin.cz.service.handler.ParserHandlerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.util.FileCopyUtils.BUFFER_SIZE;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileHandlerServiceImpl implements FileHandlerService {

    private final ParserHandlerFactory parserHandlerFactory;
    private final ExternalApiService externalApiService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;
    private final ProgressMap progressMap;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Override
    public Workbook downloadAndConvert(RequestDetails details) {
        try {
            var bytes = details.getFileBytes();
            if (bytes.length == 0) return null;

            if (isXlsx(bytes)) return new XSSFWorkbook(new ByteArrayInputStream(bytes));
            if (isXls(bytes)) return new HSSFWorkbook(new ByteArrayInputStream(bytes));

            return new HSSFWorkbook(new POIFSFileSystem(new ByteArrayInputStream(bytes)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String workbookToJson(Workbook workbook, String sessionId, UUID taskId) {
        var values = readFirstColumn(workbook);
        if (values.isEmpty()) throw new RuntimeException("Файл пуст");

        var batches = splitIntoBatches(values);
        var joiner = new StringJoiner(",");

        for (int i = 0; i < batches.size(); i++) {
            try {
                var response = cleanResponse(externalApiService.sendToCisesInfo(createRequestBody(batches.get(i))));
                if (!response.isEmpty()) joiner.add(response);

                var progress = ProgressInfo.builder()
                        .taskId(taskId)
                        .currentBatch(i + 1)
                        .totalBatches(batches.size())
                        .status(RequestStatus.PROCESS)
                        .build();
                progressMap.getProgressMap().put(sessionId, progress);
                webSocketService.sendMessage(sessionId, progress);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("...");
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                log.error("Ошибка батча {}/{}", i + 1, batches.size(), e);
            }
        }
        return "[" + joiner + "]";
    }

    @Override
    public Resource createResourceFromResponse(String json, Set<String> columns, ReportType reportType) {
        var sortedColumns = ColumnNames.sortByOrder(prepareColumnList(columns));

        try (var wb = new SXSSFWorkbook(); var os = new ByteArrayOutputStream(16384)) {
            var sheet = wb.createSheet("CIS Info");
            createHeader(sheet, sortedColumns, wb);
            fillSheetWithData(sheet, json, sortedColumns, reportType);
            wb.write(os);

            return buildResource(os.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания Excel", e);
        }
    }

    private List<String> prepareColumnList(Set<String> columns) {
        return Optional.ofNullable(columns)
                .orElse(ColumnNames.getAllNames())
                .stream()
                .filter(ColumnNames.asExtractorMap()::containsKey)
                .distinct()
                .collect(Collectors.toList());
    }

    private void createHeader(Sheet sheet, List<String> selected, SXSSFWorkbook wb) {
        var header = sheet.createRow(0);
        var style = getHeaderStyle(wb);
        for (int i = 0; i < selected.size(); i++) {
            var cell = header.createCell(i);
            cell.setCellValue(selected.get(i));
            cell.setCellStyle(style);
        }
    }

    private void fillSheetWithData(Sheet sheet, String json, List<String> selected, ReportType type) {
        try (var parser = objectMapper.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) throw new IllegalArgumentException("...");

            List<Object[]> buffer = new ArrayList<>(BUFFER_SIZE);
            int currentRow = 1;

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                var rowData = parserHandlerFactory.getHandler(type).parse(parser, selected);
                buffer.addAll(rowData);
                if (buffer.size() >= BUFFER_SIZE) {
                    currentRow = flushBuffer(buffer, sheet, currentRow);
                    buffer.clear();
                }
            }
            if (!buffer.isEmpty()) flushBuffer(buffer, sheet, currentRow);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга JSON", e);
        }
    }

    private Resource buildResource(byte[] content) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {return "cis_info_" + LocalDateTime.now().format(DATE_FORMATTER) + ".xlsx";}
        };
    }

    private int flushBuffer(List<Object[]> buffer, Sheet sheet, int startRow) {
        int currentRow = startRow;
        for (Object[] rowData : buffer) {
            var row = sheet.createRow(currentRow++);
            for (int i = 0; i < rowData.length; i++) {
                var cell = row.createCell(i);
                var value = rowData[i];
                cell.setCellValue(value != null ? value.toString() : "");
            }
        }
        return currentRow;
    }

    private CellStyle getHeaderStyle(Workbook wb) {
        var style = wb.createCellStyle();
        var font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String createRequestBody(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации списка CIS в JSON", e);
        }
    }

    private String cleanResponse(String response) {
        return Optional.ofNullable(response)
                .map(String::trim)
                .map(s -> s.startsWith("[") && s.endsWith("]") ? s.substring(1, s.length() - 1) : s)
                .orElse("");
    }

    private List<List<String>> splitIntoBatches(List<String> values) {
        List<List<String>> batches = new ArrayList<>();
        int batchSize = 1000;
        for (int i = 0; i < values.size(); i += batchSize) {
            batches.add(new ArrayList<>(values.subList(i, Math.min(i + batchSize, values.size()))));
        }
        return batches;
    }

    private List<String> readFirstColumn(Workbook wb) {
        return Optional.ofNullable(wb.getSheetAt(0))
                .map(sheet -> StreamSupport.stream(sheet.spliterator(), false)
                        .map(row -> row.getCell(0))
                        .filter(Objects::nonNull)
                        .map(cell -> cell.toString().trim())
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private boolean isXlsx(byte[] bytes) {
        return bytes.length >= 4 &&
                bytes[0] == 0x50 &&
                bytes[1] == 0x4B &&
                bytes[2] == 0x03 &&
                bytes[3] == 0x04;
    }

    private boolean isXls(byte[] bytes) {
        return bytes.length >= 8 &&
                bytes[0] == (byte) 0xD0 && bytes[1] == (byte) 0xCF &&
                bytes[2] == (byte) 0x11 && bytes[3] == (byte) 0xE0 &&
                bytes[4] == (byte) 0xA1 && bytes[5] == (byte) 0xB1 &&
                bytes[6] == (byte) 0x1A && bytes[7] == (byte) 0xE1;
    }
}