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
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;
import ustin.cz.service.ExternalApiService;
import ustin.cz.service.FileHandlerService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileHandlerServiceImpl implements FileHandlerService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ExternalApiService externalApiService;
    private final ObjectMapper objectMapper;

    @Override
    public Workbook downloadAndConvert(MultipartFile file) {
        try {
            var bytes = file.getBytes();

            if (isXlsx(bytes)) {
                return new XSSFWorkbook(new ByteArrayInputStream(bytes));
            } else if (isXls(bytes)) {
                return new HSSFWorkbook(new POIFSFileSystem(new ByteArrayInputStream(bytes)));
            } else {
                try {
                    return new XSSFWorkbook(new ByteArrayInputStream(bytes));
                } catch (Exception e) {
                    return new HSSFWorkbook(new POIFSFileSystem(new ByteArrayInputStream(bytes)));
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при конвертации файла: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать Excel файл", e);
        }
    }

    @Override
    public String processCisesInfo(Workbook workbook) {
        try {
            var values = readFirstColumn(workbook);

            log.info("Прочитано {} строк из файла", values.size());

            if (values.isEmpty()) {
                throw new RuntimeException("Файл пуст или не содержит данных в первом столбце");
            }

            var batches = splitIntoBatches(values);
            log.info("Разбито на {} пачек по 1000 строк", batches.size());

            return IntStream.range(0, batches.size())
                    .mapToObj(i -> {
                        try {
                            var response = externalApiService.sendToCisesInfo(convertToJsonArray(batches.get(i)));
                            var cleanResponse = cleanResponse(response);

                            log.info("Пачка {}/{} обработана", i + 1, batches.size());

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                            return cleanResponse;

                        } catch (Exception e) {
                            log.error("Ошибка при обработке пачки {}", i + 1, e);
                            return "";
                        }
                    })
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.joining(",", "[", "]"));

        } catch (Exception e) {
            log.error("Ошибка при обработке cises/info: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка обработки данных", e);
        }
    }

    @Override
    public Resource createResourceFromResponse(String jsonResponse) {
        try (var workbook = new SXSSFWorkbook(100);
             var outputStream = new ByteArrayOutputStream()) {

            var sheet = workbook.createSheet("CIS Info");
            createHeaders(sheet);

            int rowNum = 1;

            try (var parser = objectMapper.createParser(jsonResponse)) {
                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    throw new RuntimeException("Ожидался массив JSON");
                }

                List<Object[]> rowBuffer = new ArrayList<>(1000);

                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    var rowData = parseCisInfo(parser);
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

            setColumnWidths(sheet);

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

    private List<Object[]> parseCisInfo(JsonParser parser) throws IOException {
        List<Object[]> certDataList = new ArrayList<>();

        if (parser.currentToken() != JsonToken.START_OBJECT) return certDataList;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var fieldName = parser.currentName();

            if (fieldName == null) continue;

            parser.nextToken();

            if ("cisInfo".equals(fieldName)) {
                if (parser.currentToken() == JsonToken.START_OBJECT) {
                    return parseCisInfoObject(parser);
                } else {
                    parser.skipChildren();
                }
            }
        }

        return certDataList;
    }

    private List<Object[]> parseCisInfoObject(JsonParser parser) throws IOException {
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

        // Если есть сертификаты - создаем строку для каждого
        if (!data.certDocs.isEmpty()) {
            for (CertDocData cert : data.certDocs) {
                certDataList.add(data.toRowArray(cert));
            }
        } else if (data.cis != null && !data.cis.isEmpty()) {
            // Если нет сертификатов - создаем строку с "Нет данных"
            certDataList.add(data.toRowArray(null));
        }

        return certDataList;
    }

    private List<CertDocData> parseCertDocs(JsonParser parser) throws IOException {
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

        Object[] toRowArray(CertDocData cert) {
            return new Object[]{
                    cis != null ? cis : "",
                    gtin != null ? gtin : "",
                    productName != null ? productName : "",
                    productGroup != null ? productGroup : "",
                    productGroupId != null ? productGroupId : "",
                    brand != null ? brand : "",
                    tnVedEaes != null ? tnVedEaes : "",
                    tnVedEaesGroup != null ? tnVedEaesGroup : "",
                    manufacturerName != null ? manufacturerName : "",
                    manufacturerInn != null ? manufacturerInn : "",
                    producerName != null ? producerName : "",
                    producerInn != null ? producerInn : "",
                    ownerName != null ? ownerName : "",
                    ownerInn != null ? ownerInn : "",
                    status != null ? status : "",
                    statusEx != null ? statusEx : "",
                    withdrawReason != null ? withdrawReason : "",
                    markWithdraw != null ? markWithdraw : "",
                    isTracking != null ? isTracking : "",
                    isMultipleSales != null ? isMultipleSales : "",
                    cisTrackingType != null ? cisTrackingType : "",
                    packageType != null ? packageType : "",
                    generalPackageType != null ? generalPackageType : "",
                    emissionType != null ? emissionType : "",
                    emissionDate != null ? emissionDate : "",
                    applicationDate != null ? applicationDate : "",
                    introducedDate != null ? introducedDate : "",
                    producedDate != null ? producedDate : "",
                    cert != null ? cert.type : "Нет данных",
                    cert != null ? cert.number : "Нет данных",
                    cert != null ? cert.date : "Нет данных",
                    cert != null ? cert.statusGroup : "Нет данных",
                    cert != null ? cert.indx : "Нет данных"
            };
        }
    }

    private static class CertDocData {
        String type;
        String number;
        String date;
        String statusGroup;
        String indx;
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

    private void createHeaders(Sheet sheet) {
        var headerRow = sheet.createRow(0);
        String[] headers = {
                "CIS", "GTIN", "Product Name", "Product Group", "Product Group ID",
                "Brand", "TN VED EAES", "TN VED EAES Group", "Manufacturer Name",
                "Manufacturer INN", "Producer Name", "Producer INN", "Owner Name",
                "Owner INN", "Status", "Status Ex", "Withdraw Reason", "Mark Withdraw",
                "Is Tracking", "Is Multiple Sales", "CIS Tracking Type", "Package Type",
                "General Package Type", "Emission Type", "Emission Date",
                "Application Date", "Introduced Date", "Produced Date",
                "Certificate Type", "Certificate Number", "Certificate Date",
                "Certificate Status Group", "Certificate Index"
        };

        var headerStyle = getHeaderStyle(sheet.getWorkbook());

        for (int i = 0; i < headers.length; i++) {
            var cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void setColumnWidths(Sheet sheet) {
        int[] widths = {
                25, 15, 40, 15, 15, 15, 15, 15, 30, 18, 30, 18, 30, 18,
                15, 15, 18, 15, 15, 18, 18, 15, 20, 15, 20, 20, 20, 20,
                20, 25, 15, 20, 15
        };
        for (int i = 0; i < widths.length; i++) {
            int width = widths[i] * 256;
            if (width < 3000) {
                width = 3000;
            }
            sheet.setColumnWidth(i, width);
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
