package ustin.cz.service.impl;

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
import ustin.cz.service.ExternalApiService;
import ustin.cz.service.FileHandlerService;
import ustin.cz.service.WebSocketService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileHandlerServiceImpl implements FileHandlerService {

    private final WebSocketService webSocketService;
    private final ProgressMap progressMap;
    private final ExternalApiService externalApiService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Map<String, Integer> COLUMN_WIDTHS = new HashMap<>();
    private static final Map<String, ColumnExtractor> EXTRACTORS = new LinkedHashMap<>();

    static {
        EXTRACTORS.put(ColumnNames.CIS, (d, c) -> d.cis != null ? d.cis : "");
        EXTRACTORS.put(ColumnNames.GTIN, (d, c) -> d.gtin != null ? d.gtin : "");
        EXTRACTORS.put(ColumnNames.PRODUCT_NAME, (d, c) -> d.productName != null ? d.productName : "");
        EXTRACTORS.put(ColumnNames.PRODUCT_GROUP, (d, c) -> d.productGroup != null ? d.productGroup : "");
        EXTRACTORS.put(ColumnNames.PRODUCT_GROUP_ID, (d, c) -> d.productGroupId != null ? d.productGroupId : "");
        EXTRACTORS.put(ColumnNames.BRAND, (d, c) -> d.brand != null ? d.brand : "");
        EXTRACTORS.put(ColumnNames.TN_VED_EAES, (d, c) -> d.tnVedEaes != null ? d.tnVedEaes : "");
        EXTRACTORS.put(ColumnNames.TN_VED_EAES_GROUP, (d, c) -> d.tnVedEaesGroup != null ? d.tnVedEaesGroup : "");
        EXTRACTORS.put(ColumnNames.MANUFACTURER_NAME, (d, c) -> d.manufacturerName != null ? d.manufacturerName : "");
        EXTRACTORS.put(ColumnNames.MANUFACTURER_INN, (d, c) -> d.manufacturerInn != null ? d.manufacturerInn : "");
        EXTRACTORS.put(ColumnNames.PRODUCER_NAME, (d, c) -> d.producerName != null ? d.producerName : "");
        EXTRACTORS.put(ColumnNames.PRODUCER_INN, (d, c) -> d.producerInn != null ? d.producerInn : "");
        EXTRACTORS.put(ColumnNames.OWNER_NAME, (d, c) -> d.ownerName != null ? d.ownerName : "");
        EXTRACTORS.put(ColumnNames.OWNER_INN, (d, c) -> d.ownerInn != null ? d.ownerInn : "");
        EXTRACTORS.put(ColumnNames.STATUS, (d, c) -> d.status != null ? d.status : "");
        EXTRACTORS.put(ColumnNames.STATUS_EX, (d, c) -> d.statusEx != null ? d.statusEx : "");
        EXTRACTORS.put(ColumnNames.WITHDRAW_REASON, (d, c) -> d.withdrawReason != null ? d.withdrawReason : "");
        EXTRACTORS.put(ColumnNames.MARK_WITHDRAW, (d, c) -> d.markWithdraw != null ? d.markWithdraw : "");
        EXTRACTORS.put(ColumnNames.IS_TRACKING, (d, c) -> d.isTracking != null ? d.isTracking : "");
        EXTRACTORS.put(ColumnNames.IS_MULTIPLE_SALES, (d, c) -> d.isMultipleSales != null ? d.isMultipleSales : "");
        EXTRACTORS.put(ColumnNames.CIS_TRACKING_TYPE, (d, c) -> d.cisTrackingType != null ? d.cisTrackingType : "");
        EXTRACTORS.put(ColumnNames.PACKAGE_TYPE, (d, c) -> d.packageType != null ? d.packageType : "");
        EXTRACTORS.put(ColumnNames.GENERAL_PACKAGE_TYPE, (d, c) -> d.generalPackageType != null ? d.generalPackageType : "");
        EXTRACTORS.put(ColumnNames.EMISSION_TYPE, (d, c) -> d.emissionType != null ? d.emissionType : "");
        EXTRACTORS.put(ColumnNames.EMISSION_DATE, (d, c) -> d.emissionDate != null ? d.emissionDate : "");
        EXTRACTORS.put(ColumnNames.APPLICATION_DATE, (d, c) -> d.applicationDate != null ? d.applicationDate : "");
        EXTRACTORS.put(ColumnNames.INTRODUCED_DATE, (d, c) -> d.introducedDate != null ? d.introducedDate : "");
        EXTRACTORS.put(ColumnNames.PRODUCED_DATE, (d, c) -> d.producedDate != null ? d.producedDate : "");
        EXTRACTORS.put(ColumnNames.CERTIFICATE_TYPE, (d, c) -> c != null ? c.type : "Нет данных");
        EXTRACTORS.put(ColumnNames.CERTIFICATE_NUMBER, (d, c) -> c != null ? c.number : "Нет данных");
        EXTRACTORS.put(ColumnNames.CERTIFICATE_DATE, (d, c) -> c != null ? c.date : "Нет данных");
        EXTRACTORS.put(ColumnNames.CERTIFICATE_STATUS_GROUP, (d, c) -> c != null ? c.statusGroup : "Нет данных");
        EXTRACTORS.put(ColumnNames.CERTIFICATE_INDEX, (d, c) -> c != null ? c.indx : "Нет данных");

        COLUMN_WIDTHS.put(ColumnNames.PRODUCT_NAME, 40);
        COLUMN_WIDTHS.put(ColumnNames.MANUFACTURER_NAME, 30);
        COLUMN_WIDTHS.put(ColumnNames.PRODUCER_NAME, 30);
        COLUMN_WIDTHS.put(ColumnNames.OWNER_NAME, 30);
        COLUMN_WIDTHS.put(ColumnNames.CIS, 25);
        COLUMN_WIDTHS.put(ColumnNames.CERTIFICATE_NUMBER, 25);
        COLUMN_WIDTHS.put(ColumnNames.MANUFACTURER_INN, 18);
        COLUMN_WIDTHS.put(ColumnNames.PRODUCER_INN, 18);
        COLUMN_WIDTHS.put(ColumnNames.OWNER_INN, 18);
        COLUMN_WIDTHS.put(ColumnNames.EMISSION_DATE, 20);
        COLUMN_WIDTHS.put(ColumnNames.APPLICATION_DATE, 20);
        COLUMN_WIDTHS.put(ColumnNames.INTRODUCED_DATE, 20);
        COLUMN_WIDTHS.put(ColumnNames.PRODUCED_DATE, 20);
        COLUMN_WIDTHS.put(ColumnNames.CERTIFICATE_DATE, 20);
    }

    @Override
    public Workbook downloadAndConvert(RequestDetails details) {
        try {
            byte[] bytes = details.getFileBytes();
            if (bytes.length == 0) {
                return null;
            }
            if (isXlsx(bytes)) {
                return new XSSFWorkbook(new ByteArrayInputStream(bytes));
            }
            return new HSSFWorkbook(new POIFSFileSystem(new ByteArrayInputStream(bytes)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String workbookToJson(Workbook workbook, String sessionId, UUID taskId) {
        List<String> values = readFirstColumn(workbook);
        if (values.isEmpty()) {
            throw new RuntimeException("Файл пуст");
        }

        List<List<String>> batches = splitIntoBatches(values);
        int totalBatches = batches.size();
        List<String> results = new ArrayList<>();

        // Инициализируем прогресс только один раз
        var progress = progressMap.getProgressMap().get(sessionId);
        if (progress != null) {
            progress.setTotalBatches(totalBatches);
            progress.setCurrentBatch(0);
            progress.setTaskId(taskId);
            progressMap.getProgressMap().put(sessionId, progress);
            webSocketService.sendMessage(sessionId, progress);
        }

        for (int i = 0; i < totalBatches; i++) {
            try {
                String body = createRequestBody(batches.get(i));
                String response = cleanResponse(externalApiService.sendToCisesInfo(body));

                if (!response.isEmpty()) results.add(response);

                log.info("Батч {}/{} обработан", i + 1, totalBatches);

                // Обновляем прогресс
                if (progress != null) {
                    progress.setCurrentBatch(i + 1);
                    progressMap.getProgressMap().put(sessionId, progress);
                    webSocketService.sendMessage(sessionId, progress);
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Поток был прерван во время ожидания между батчами");
                    throw new RuntimeException("Обработка прервана", e);
                }
            } catch (Exception e) {
                log.error("Ошибка батча {}/{}", i + 1, totalBatches, e);
            }
        }

        return results.stream().collect(Collectors.joining(",", "[", "]"));
    }

    @Override
    public Resource createResourceFromResponse(String json, Set<String> columns) {
        Set<String> selected = Optional.ofNullable(columns)
                .orElse(ColumnNames.getAllColumnNames())
                .stream()
                .filter(EXTRACTORS::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100);
             var os = new ByteArrayOutputStream()) {

            var sheet = wb.createSheet("CIS Info");
            var header = sheet.createRow(0);
            var style = getHeaderStyle(wb);

            int colIndex = 0;
            for (String name : selected) {
                Cell cell = header.createCell(colIndex);
                cell.setCellValue(name);
                cell.setCellStyle(style);
                colIndex++;
            }

            try (JsonParser parser = objectMapper.createParser(json)) {
                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    throw new RuntimeException("Не JSON массив");
                }

                List<Object[]> buffer = new ArrayList<>();
                int rowNum = 1;

                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    List<Object[]> rowData = parseCisInfo(parser, selected);
                    if (!rowData.isEmpty()) {
                        buffer.addAll(rowData);
                        if (buffer.size() >= 1000) {
                            rowNum = flushBuffer(buffer, sheet, rowNum);
                            buffer.clear();
                        }
                    }
                }
                if (!buffer.isEmpty()) {
                    flushBuffer(buffer, sheet, rowNum);
                }
            }

            setColumnWidths(sheet, selected);
            wb.write(os);
            wb.dispose();

            byte[] excelBytes = os.toByteArray();
            return new ByteArrayResource(excelBytes) {
                @Override
                public String getFilename() {
                    return "cis_info_" + LocalDateTime.now().format(DATE_FORMATTER) + ".xlsx";
                }
            };

        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания Excel", e);
        }
    }

    private List<Object[]> parseCisInfo(JsonParser parser, Set<String> columns) {
        List<Object[]> result = new ArrayList<>();

        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return result;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.currentName();
            if (name == null) {
                continue;
            }
            parser.nextToken();

            if ("cisInfo".equals(name) && parser.currentToken() == JsonToken.START_OBJECT) {
                return parseObject(parser, columns);
            }
        }
        return result;
    }

    private List<Object[]> parseObject(JsonParser parser, Set<String> columns) {
        var data = new CisInfoData();
        List<Object[]> result = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var name = parser.currentName();
            if (name == null) {
                continue;
            }
            parser.nextToken();

            switch (name) {
                case "requestedCis":
                case "cis":
                    data.cis = parser.getValueAsString();
                    break;
                case "gtin":
                    data.gtin = parser.getValueAsString();
                    break;
                case "productName":
                    data.productName = parser.getValueAsString();
                    break;
                case "productGroup":
                    data.productGroup = parser.getValueAsString();
                    break;
                case "productGroupId":
                    data.productGroupId = parser.getValueAsString();
                    break;
                case "brand":
                    data.brand = parser.getValueAsString();
                    break;
                case "tnVedEaes":
                    data.tnVedEaes = parser.getValueAsString();
                    break;
                case "tnVedEaesGroup":
                    data.tnVedEaesGroup = parser.getValueAsString();
                    break;
                case "manufacturerName":
                    data.manufacturerName = parser.getValueAsString();
                    break;
                case "manufacturerInn":
                    data.manufacturerInn = parser.getValueAsString();
                    break;
                case "producerName":
                    data.producerName = parser.getValueAsString();
                    break;
                case "producerInn":
                    data.producerInn = parser.getValueAsString();
                    break;
                case "ownerName":
                    data.ownerName = parser.getValueAsString();
                    break;
                case "ownerInn":
                    data.ownerInn = parser.getValueAsString();
                    break;
                case "status":
                    data.status = parser.getValueAsString();
                    break;
                case "statusEx":
                    data.statusEx = parser.getValueAsString();
                    break;
                case "withdrawReason":
                    data.withdrawReason = parser.getValueAsString();
                    break;
                case "markWithdraw":
                    data.markWithdraw = parser.getValueAsString();
                    break;
                case "isTracking":
                    data.isTracking = parser.getValueAsString();
                    break;
                case "isMultipleSales":
                    data.isMultipleSales = parser.getValueAsString();
                    break;
                case "cisTrackingType":
                    data.cisTrackingType = parser.getValueAsString();
                    break;
                case "packageType":
                    data.packageType = parser.getValueAsString();
                    break;
                case "generalPackageType":
                    data.generalPackageType = parser.getValueAsString();
                    break;
                case "emissionType":
                    data.emissionType = parser.getValueAsString();
                    break;
                case "emissionDate":
                    data.emissionDate = parser.getValueAsString();
                    break;
                case "applicationDate":
                    data.applicationDate = parser.getValueAsString();
                    break;
                case "introducedDate":
                    data.introducedDate = parser.getValueAsString();
                    break;
                case "producedDate":
                    data.producedDate = parser.getValueAsString();
                    break;
                case "certDoc":
                    if (parser.currentToken() == JsonToken.START_ARRAY) {
                        data.certDocs = parseCertDocs(parser);
                    }
                    break;
                default:
                    parser.skipChildren();
                    break;
            }
        }

        if (!data.certDocs.isEmpty()) {
            for (CertDocData cert : data.certDocs) {
                result.add(toRowArray(data, cert, columns));
            }
        } else if (data.cis != null) {
            result.add(toRowArray(data, null, columns));
        }
        return result;
    }

    private List<CertDocData> parseCertDocs(JsonParser parser) {
        List<CertDocData> result = new ArrayList<>();
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            return result;
        }

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() == JsonToken.START_OBJECT) {
                var cert = new CertDocData();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    var name = parser.currentName();
                    if (name == null) {
                        continue;
                    }
                    parser.nextToken();
                    switch (name) {
                        case "type":
                            cert.type = parser.getValueAsString();
                            break;
                        case "number":
                            cert.number = parser.getValueAsString();
                            break;
                        case "date":
                            cert.date = parser.getValueAsString();
                            break;
                        case "statusGroup":
                            cert.statusGroup = parser.getValueAsString();
                            break;
                        case "indx":
                            cert.indx = parser.getValueAsString();
                            break;
                        default:
                            parser.skipChildren();
                            break;
                    }
                }
                result.add(cert);
            }
        }
        return result;
    }

    private Object[] toRowArray(CisInfoData data, CertDocData cert, Set<String> columns) {
        List<Object> row = new ArrayList<>();
        for (String columnName : columns) {
            var extractor = EXTRACTORS.get(columnName);
            if (extractor != null) {
                row.add(extractor.extract(data, cert));
            }
        }
        return row.toArray();
    }

    private int flushBuffer(List<Object[]> buffer, Sheet sheet, int startRow) {
        int currentRow = startRow;
        for (Object[] rowData : buffer) {
            Row row = sheet.createRow(currentRow);
            for (int i = 0; i < rowData.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(rowData[i] != null ? rowData[i].toString() : "");
            }
            currentRow++;
        }
        return currentRow;
    }

    private void setColumnWidths(Sheet sheet, Set<String> columns) {
        int index = 0;
        for (String col : columns) {
            sheet.setColumnWidth(index, COLUMN_WIDTHS.getOrDefault(col, 15) * 256);
            index++;
        }
    }

    private CellStyle getHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
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
        return values.stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String cleanResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private List<List<String>> splitIntoBatches(List<String> values) {
        List<List<String>> batches = new ArrayList<>();
        int batchSize = 1000;
        for (int i = 0; i < values.size(); i += batchSize) {
            int end = Math.min(i + batchSize, values.size());
            batches.add(new ArrayList<>(values.subList(i, end)));
        }
        return batches;
    }

    private List<String> readFirstColumn(Workbook wb) {
        List<String> values = new ArrayList<>();
        Sheet sheet = wb.getSheetAt(0);
        if (sheet == null) {
            return values;
        }

        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue;
            }
            Cell cell = row.getCell(0);
            if (cell != null) {
                String val = cell.toString().trim();
                if (!val.isEmpty()) {
                    values.add(val);
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
}