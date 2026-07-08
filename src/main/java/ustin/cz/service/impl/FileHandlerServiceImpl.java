package ustin.cz.service.impl;

import io.micrometer.common.util.StringUtils;
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
import ustin.cz.component.ColumnNames;
import ustin.cz.component.ProgressInfo;
import ustin.cz.component.RequestStatus;
import ustin.cz.service.ApplicationService;
import ustin.cz.service.ExternalApiService;
import ustin.cz.service.FileHandlerService;
import ustin.cz.component.RequestDetails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileHandlerServiceImpl implements FileHandlerService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ExternalApiService externalApiService;
    private final ObjectMapper objectMapper;

    private static final Map<String, ColumnExtractor> COLUMN_EXTRACTORS = new LinkedHashMap<>();

    static {
        COLUMN_EXTRACTORS.put(ColumnNames.CIS, (data, cert) -> data.cis != null ? data.cis : "");
        COLUMN_EXTRACTORS.put(ColumnNames.GTIN, (data, cert) -> data.gtin != null ? data.gtin : "");
        COLUMN_EXTRACTORS.put(ColumnNames.PRODUCT_NAME, (data, cert) -> data.productName != null ? data.productName : "");
        COLUMN_EXTRACTORS.put(ColumnNames.PRODUCT_GROUP, (data, cert) -> data.productGroup != null ? data.productGroup : "");
        COLUMN_EXTRACTORS.put(ColumnNames.PRODUCT_GROUP_ID, (data, cert) -> data.productGroupId != null ? data.productGroupId : "");
        COLUMN_EXTRACTORS.put(ColumnNames.BRAND, (data, cert) -> data.brand != null ? data.brand : "");
        COLUMN_EXTRACTORS.put(ColumnNames.TN_VED_EAES, (data, cert) -> data.tnVedEaes != null ? data.tnVedEaes : "");
        COLUMN_EXTRACTORS.put(ColumnNames.TN_VED_EAES_GROUP, (data, cert) -> data.tnVedEaesGroup != null ? data.tnVedEaesGroup : "");
        COLUMN_EXTRACTORS.put(ColumnNames.MANUFACTURER_NAME, (data, cert) -> data.manufacturerName != null ? data.manufacturerName : "");
        COLUMN_EXTRACTORS.put(ColumnNames.MANUFACTURER_INN, (data, cert) -> data.manufacturerInn != null ? data.manufacturerInn : "");
        COLUMN_EXTRACTORS.put(ColumnNames.PRODUCER_NAME, (data, cert) -> data.producerName != null ? data.producerName : "");
        COLUMN_EXTRACTORS.put(ColumnNames.PRODUCER_INN, (data, cert) -> data.producerInn != null ? data.producerInn : "");
        COLUMN_EXTRACTORS.put(ColumnNames.OWNER_NAME, (data, cert) -> data.ownerName != null ? data.ownerName : "");
        COLUMN_EXTRACTORS.put(ColumnNames.OWNER_INN, (data, cert) -> data.ownerInn != null ? data.ownerInn : "");
        COLUMN_EXTRACTORS.put(ColumnNames.STATUS, (data, cert) -> data.status != null ? data.status : "");
        COLUMN_EXTRACTORS.put(ColumnNames.STATUS_EX, (data, cert) -> data.statusEx != null ? data.statusEx : "");
        COLUMN_EXTRACTORS.put(ColumnNames.WITHDRAW_REASON, (data, cert) -> data.withdrawReason != null ? data.withdrawReason : "");
        COLUMN_EXTRACTORS.put(ColumnNames.MARK_WITHDRAW, (data, cert) -> data.markWithdraw != null ? data.markWithdraw : "");
        COLUMN_EXTRACTORS.put(ColumnNames.IS_TRACKING, (data, cert) -> data.isTracking != null ? data.isTracking : "");
        COLUMN_EXTRACTORS.put(ColumnNames.IS_MULTIPLE_SALES, (data, cert) -> data.isMultipleSales != null ? data.isMultipleSales : "");
        COLUMN_EXTRACTORS.put(ColumnNames.CIS_TRACKING_TYPE, (data, cert) -> data.cisTrackingType != null ? data.cisTrackingType : "");
        COLUMN_EXTRACTORS.put(ColumnNames.PACKAGE_TYPE, (data, cert) -> data.packageType != null ? data.packageType : "");
        COLUMN_EXTRACTORS.put(ColumnNames.GENERAL_PACKAGE_TYPE, (data, cert) -> data.generalPackageType != null ? data.generalPackageType : "");
        COLUMN_EXTRACTORS.put(ColumnNames.EMISSION_TYPE, (data, cert) -> data.emissionType != null ? data.emissionType : "");
        COLUMN_EXTRACTORS.put(ColumnNames.EMISSION_DATE, (data, cert) -> data.emissionDate != null ? data.emissionDate : "");
        COLUMN_EXTRACTORS.put(ColumnNames.APPLICATION_DATE, (data, cert) -> data.applicationDate != null ? data.applicationDate : "");
        COLUMN_EXTRACTORS.put(ColumnNames.INTRODUCED_DATE, (data, cert) -> data.introducedDate != null ? data.introducedDate : "");
        COLUMN_EXTRACTORS.put(ColumnNames.PRODUCED_DATE, (data, cert) -> data.producedDate != null ? data.producedDate : "");

        // Поля сертификатов
        COLUMN_EXTRACTORS.put(ColumnNames.CERTIFICATE_TYPE, (data, cert) -> cert != null ? cert.type : "Нет данных");
        COLUMN_EXTRACTORS.put(ColumnNames.CERTIFICATE_NUMBER, (data, cert) -> cert != null ? cert.number : "Нет данных");
        COLUMN_EXTRACTORS.put(ColumnNames.CERTIFICATE_DATE, (data, cert) -> cert != null ? cert.date : "Нет данных");
        COLUMN_EXTRACTORS.put(ColumnNames.CERTIFICATE_STATUS_GROUP, (data, cert) -> cert != null ? cert.statusGroup : "Нет данных");
        COLUMN_EXTRACTORS.put(ColumnNames.CERTIFICATE_INDEX, (data, cert) -> cert != null ? cert.indx : "Нет данных");
    }

    @Override
    public Workbook downloadAndConvert(RequestDetails requestDetails) {
        if (requestDetails == null) {
            throw new RuntimeException("Задача не найдена");
        }

        try {
            // Используем сохраненные байты вместо file.getBytes()
            byte[] bytes = requestDetails.getFileBytes();

            if (bytes == null || bytes.length == 0) {
                throw new RuntimeException("Файл пуст или не сохранен");
            }

            log.info("Конвертация файла, размер: {} байт", bytes.length);

            if (isXlsx(bytes)) {
                log.info("Определен формат XLSX");
                return new XSSFWorkbook(new ByteArrayInputStream(bytes));
            } else if (isXls(bytes)) {
                log.info("Определен формат XLS");
                return new HSSFWorkbook(new POIFSFileSystem(new ByteArrayInputStream(bytes)));
            } else {
                log.warn("Неизвестный формат, пробуем как XSSF...");
                try {
                    return new XSSFWorkbook(new ByteArrayInputStream(bytes));
                } catch (Exception e) {
                    log.warn("Не удалось открыть как XSSF, пробуем HSSF: {}", e.getMessage());
                    return new HSSFWorkbook(new POIFSFileSystem(new ByteArrayInputStream(bytes)));
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при конвертации файла: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать Excel файл", e);
        }
    }

    @Override
    public String processCisesSearch(Workbook workbook, String sessionId, ApplicationService applicationService) {
        log.info("🔍 Начало обработки CIS_SEARCH для сессии: {}", sessionId);

        try {
            // Инициализация прогресса
            var progress = ProgressInfo.builder()
                    .status(RequestStatus.PROCESS)
                    .progress(0)
                    .currentBatch(0)
                    .totalBatches(0)
                    .build();
            applicationService.updateProgress(sessionId, progress);

            // Читаем первый столбец
            var values = readFirstColumn(workbook);
            log.info("Прочитано {} строк из файла", values.size());

            if (values.isEmpty()) {
                throw new RuntimeException("Файл пуст или не содержит данных в первом столбце");
            }

            // Разбиваем на батчи
            var batches = splitIntoBatches(values);
            int totalBatches = batches.size();
            log.info("Разбито на {} пачек", totalBatches);

            // Обновляем прогресс
            progress.setTotalBatches(totalBatches);
            applicationService.updateProgress(sessionId, progress);

            List<String> results = new ArrayList<>();

            // Обрабатываем каждый батч
            for (int i = 0; i < totalBatches; i++) {
                try {
                    // Рассчитываем прогресс
                    int progressPercent = (int) (((double) (i + 1) / totalBatches) * 100);

                    // Обновляем прогресс
                    progress.setCurrentBatch(i + 1);
                    progress.setProgress(progressPercent);
                    applicationService.updateProgress(sessionId, progress);

                    // Отправляем запрос
                    var response = externalApiService.sendToCisesInfo(convertToJsonArray(batches.get(i)));
                    var cleanResponse = cleanResponse(response);

                    if (StringUtils.isNotBlank(cleanResponse)) {
                        results.add(cleanResponse);
                    }

                    log.info("Батч {}/{} обработан", i + 1, totalBatches);

                    // Задержка между запросами
                    Thread.sleep(500);

                } catch (Exception e) {
                    log.error("Ошибка при обработке батча {}", i + 1, e);
                    applicationService.updateProgress(sessionId, progress);
                }
            }

            // Финальная обработка
            progress.setProgress(98);
            applicationService.updateProgress(sessionId, progress);

            // Собираем результат
            var finalResult = results.stream().collect(Collectors.joining(",", "[", "]"));

            progress.setStatus(RequestStatus.SUCCESS);
            progress.setProgress(100);
            applicationService.updateProgress(sessionId, progress);

            log.info("Обработка CIS_SEARCH завершена. Получено {} результатов", results.size());
            return finalResult;

        } catch (Exception e) {
            log.error("Ошибка при обработке CIS_SEARCH: {}", e.getMessage(), e);

            ProgressInfo progress = new ProgressInfo();
            progress.setStatus(RequestStatus.ERROR);
            progress.setErrorDetails(e.getMessage());
            applicationService.updateProgress(sessionId, progress);

            throw new RuntimeException("Ошибка обработки данных", e);
        }
    }

    @Override
    public Resource createResourceFromResponse(String jsonResponse, Set<String> selectedColumns) {
        // Если не указаны колонки - используем все
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            selectedColumns = ColumnNames.getAllColumnNames();
        }

        // Фильтруем только существующие колонки
        Set<String> finalSelectedColumns = selectedColumns.stream()
                .filter(COLUMN_EXTRACTORS::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        try (var workbook = new SXSSFWorkbook(100);
             var outputStream = new ByteArrayOutputStream()) {

            var sheet = workbook.createSheet("CIS Info");

            // Создаем заголовки только для выбранных колонок
            createHeaders(sheet, finalSelectedColumns);

            int rowNum = 1;

            try (var parser = objectMapper.createParser(jsonResponse)) {
                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    throw new RuntimeException("Ожидался массив JSON");
                }

                List<Object[]> rowBuffer = new ArrayList<>(1000);

                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    var rowData = parseCisInfo(parser, finalSelectedColumns);
                    if (rowData != null && !rowData.isEmpty()) {
                        rowBuffer.addAll(rowData);

                        if (rowBuffer.size() >= 1000) {
                            rowNum = flushBuffer(rowBuffer, sheet, rowNum);
                            rowBuffer.clear();
                        }
                    }
                }

                if (!rowBuffer.isEmpty()) {
                    rowNum = flushBuffer(rowBuffer, sheet, rowNum);
                    rowBuffer.clear();
                }
            }

            // Устанавливаем ширины только для выбранных колонок
            setColumnWidths(sheet, finalSelectedColumns);

            workbook.write(outputStream);
            workbook.dispose();

            var excelBytes = outputStream.toByteArray();

            return new ByteArrayResource(excelBytes) {
                @Override
                public String getFilename() {
                    return generateFileName();
                }
            };

        } catch (Exception e) {
            log.error("Ошибка при формировании Excel-файла: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при создании Excel файла: " + e.getMessage(), e);
        }
    }

    private List<Object[]> parseCisInfo(JsonParser parser, Set<String> selectedColumns) throws IOException {
        List<Object[]> certDataList = new ArrayList<>();

        if (parser.currentToken() != JsonToken.START_OBJECT) return certDataList;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var fieldName = parser.currentName();

            if (fieldName == null) continue;

            parser.nextToken();

            if ("cisInfo".equals(fieldName)) {
                if (parser.currentToken() == JsonToken.START_OBJECT) {
                    return parseCisInfoObject(parser, selectedColumns);
                } else {
                    parser.skipChildren();
                }
            }
        }

        return certDataList;
    }

    private List<Object[]> parseCisInfoObject(JsonParser parser, Set<String> selectedColumns) throws IOException {
        CisInfoData data = new CisInfoData();
        List<Object[]> certDataList = new ArrayList<>();

        if (parser.currentToken() != JsonToken.START_OBJECT) return certDataList;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var fieldName = parser.currentName();

            if (fieldName == null) continue;

            parser.nextToken();

            switch (fieldName) {
                case "requestedCis", "cis" -> data.cis = parser.getValueAsString();
                case "gtin" -> data.gtin = parser.getValueAsString();
                case "productName" -> data.productName = parser.getValueAsString();
                case "productGroup" -> data.productGroup = parser.getValueAsString();
                case "productGroupId" -> data.productGroupId = parser.getValueAsString();
                case "brand" -> data.brand = parser.getValueAsString();
                case "tnVedEaes" -> data.tnVedEaes = parser.getValueAsString();
                case "tnVedEaesGroup" -> data.tnVedEaesGroup = parser.getValueAsString();
                case "manufacturerName" -> data.manufacturerName = parser.getValueAsString();
                case "manufacturerInn" -> data.manufacturerInn = parser.getValueAsString();
                case "producerName" -> data.producerName = parser.getValueAsString();
                case "producerInn" -> data.producerInn = parser.getValueAsString();
                case "ownerName" -> data.ownerName = parser.getValueAsString();
                case "ownerInn" -> data.ownerInn = parser.getValueAsString();
                case "status" -> data.status = parser.getValueAsString();
                case "statusEx" -> data.statusEx = parser.getValueAsString();
                case "withdrawReason" -> data.withdrawReason = parser.getValueAsString();
                case "markWithdraw" -> data.markWithdraw = parser.getValueAsString();
                case "isTracking" -> data.isTracking = parser.getValueAsString();
                case "isMultipleSales" -> data.isMultipleSales = parser.getValueAsString();
                case "cisTrackingType" -> data.cisTrackingType = parser.getValueAsString();
                case "packageType" -> data.packageType = parser.getValueAsString();
                case "generalPackageType" -> data.generalPackageType = parser.getValueAsString();
                case "emissionType" -> data.emissionType = parser.getValueAsString();
                case "emissionDate" -> data.emissionDate = parser.getValueAsString();
                case "applicationDate" -> data.applicationDate = parser.getValueAsString();
                case "introducedDate" -> data.introducedDate = parser.getValueAsString();
                case "producedDate" -> data.producedDate = parser.getValueAsString();
                case "certDoc" -> {
                    if (parser.currentToken() == JsonToken.START_ARRAY) {
                        data.certDocs = parseCertDocs(parser);
                    }
                }
                default -> parser.skipChildren();
            }
        }

        if (!data.certDocs.isEmpty()) {
            for (CertDocData cert : data.certDocs) {
                certDataList.add(data.toRowArray(cert, selectedColumns));
            }
        } else if (data.cis != null && !data.cis.isEmpty()) {
            certDataList.add(data.toRowArray(null, selectedColumns));
        }

        return certDataList;
    }

    private List<CertDocData> parseCertDocs(JsonParser parser) {
        List<CertDocData> result = new ArrayList<>();

        if (parser.currentToken() != JsonToken.START_ARRAY) return result;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() == JsonToken.START_OBJECT) {
                CertDocData cert = new CertDocData();

                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    var fieldName = parser.currentName();

                    if (fieldName == null) continue;

                    parser.nextToken();

                    switch (fieldName) {
                        case "type" -> cert.type = parser.getValueAsString();
                        case "number" -> cert.number = parser.getValueAsString();
                        case "date" -> cert.date = parser.getValueAsString();
                        case "statusGroup" -> cert.statusGroup = parser.getValueAsString();
                        case "indx" -> cert.indx = parser.getValueAsString();
                        default -> parser.skipChildren();
                    }
                }

                result.add(cert);
            } else {
                parser.skipChildren();
            }
        }

        return result;
    }

    private static class CisInfoData {
        String cis;
        String gtin;
        String productName;
        String productGroup;
        String productGroupId;
        String brand;
        String tnVedEaes;
        String tnVedEaesGroup;
        String manufacturerName;
        String manufacturerInn;
        String producerName;
        String producerInn;
        String ownerName;
        String ownerInn;
        String status;
        String statusEx;
        String withdrawReason;
        String markWithdraw;
        String isTracking;
        String isMultipleSales;
        String cisTrackingType;
        String packageType;
        String generalPackageType;
        String emissionType;
        String emissionDate;
        String applicationDate;
        String introducedDate;
        String producedDate;
        List<CertDocData> certDocs = new ArrayList<>();

        Object[] toRowArray(CertDocData cert, Set<String> selectedColumns) {
            List<Object> row = new ArrayList<>();

            for (String columnName : selectedColumns) {
                ColumnExtractor extractor = COLUMN_EXTRACTORS.get(columnName);
                if (extractor != null) {
                    row.add(extractor.extract(this, cert));
                }
            }

            return row.toArray();
        }
    }

    private static class CertDocData {
        String type;
        String number;
        String date;
        String statusGroup;
        String indx;
    }

    @FunctionalInterface
    private interface ColumnExtractor {
        String extract(CisInfoData data, CertDocData cert);
    }

    private int flushBuffer(List<Object[]> buffer, Sheet sheet, int startRow) {
        for (Object[] rowData : buffer) {
            var row = sheet.createRow(startRow++);
            for (int i = 0; i < rowData.length; i++) {
                Object value = rowData[i];
                Cell cell = row.createCell(i);
                if (value != null) {
                    cell.setCellValue(value.toString());
                } else {
                    cell.setCellValue("");
                }
            }
        }
        return startRow;
    }

    private void createHeaders(Sheet sheet, Set<String> selectedColumns) {
        var headerRow = sheet.createRow(0);
        var headerStyle = getHeaderStyle(sheet.getWorkbook());

        int colIndex = 0;
        for (String columnName : selectedColumns) {
            var cell = headerRow.createCell(colIndex++);
            cell.setCellValue(columnName);
            cell.setCellStyle(headerStyle);
        }
    }

    private void setColumnWidths(Sheet sheet, Set<String> selectedColumns) {
        // Базовая ширина для колонок
        int[] defaultWidths = {
                25, 15, 40, 15, 15, 15, 15, 15, 30, 18, 30, 18, 30, 18,
                15, 15, 18, 15, 15, 18, 18, 15, 20, 15, 20, 20, 20, 20,
                20, 25, 15, 20, 15
        };

        // Маппинг названий колонок к ширине
        Map<String, Integer> widthMap = new LinkedHashMap<>();
        String[] allColumns = ColumnNames.getAllColumnNames().toArray(new String[0]);
        for (int i = 0; i < allColumns.length && i < defaultWidths.length; i++) {
            widthMap.put(allColumns[i], defaultWidths[i]);
        }

        int colIndex = 0;
        for (String columnName : selectedColumns) {
            Integer width = widthMap.get(columnName);
            if (width == null) {
                width = 20; // значение по умолчанию
            }
            int widthInChars = width * 256;
            if (widthInChars < 3000) {
                widthInChars = 3000;
            }
            sheet.setColumnWidth(colIndex++, widthInChars);
        }
    }

    private CellStyle cachedHeaderStyle;

    private CellStyle getHeaderStyle(Workbook workbook) {
        if (cachedHeaderStyle == null) {
            cachedHeaderStyle = createHeaderStyle(workbook);
        }
        return cachedHeaderStyle;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        var style = workbook.createCellStyle();
        var font = workbook.createFont();
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

    private String generateFileName() {
        return "cis_info_" + LocalDateTime.now().format(DATE_FORMATTER) + ".xlsx";
    }

    public String convertToJsonArray(List<String> values) {
        return values.stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String cleanResponse(String response) {
        if (response == null || response.isEmpty()) return "";

        var trimmed = response.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) return trimmed.substring(1, trimmed.length() - 1);

        return trimmed;
    }

    public List<List<String>> splitIntoBatches(List<String> values) {
        List<List<String>> batches = new ArrayList<>();
        int batchSize = 1000;
        for (int i = 0; i < values.size(); i += batchSize) {
            int end = Math.min(i + batchSize, values.size());
            batches.add(new ArrayList<>(values.subList(i, end)));
        }
        return batches;
    }

    public List<String> readFirstColumn(Workbook workbook) {
        List<String> values = new ArrayList<>();
        var sheet = workbook.getSheetAt(0);

        if (sheet == null) {
            log.warn("Лист не найден");
            return values;
        }

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;

            var cell = row.getCell(0);
            if (cell != null) {
                String value = cell.toString().trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }

        return values;
    }

    private boolean isXlsx(byte[] bytes) {
        return bytes.length >= 4 &&
                bytes[0] == 0x50 && bytes[1] == 0x4B &&
                bytes[2] == 0x03 && bytes[3] == 0x04;
    }

    private boolean isXls(byte[] bytes) {
        return bytes.length >= 8 &&
                bytes[0] == (byte) 0xD0 && bytes[1] == (byte) 0xCF &&
                bytes[2] == (byte) 0x11 && bytes[3] == (byte) 0xE0 &&
                bytes[4] == (byte) 0xA1 && bytes[5] == (byte) 0xB1 &&
                bytes[6] == (byte) 0x1A && bytes[7] == (byte) 0xE1;
    }
}