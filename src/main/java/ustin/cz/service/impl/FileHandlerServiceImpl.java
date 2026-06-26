package ustin.cz.service.impl;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ustin.cz.service.ExternalApiService;
import ustin.cz.service.FileHandlerService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileHandlerServiceImpl implements FileHandlerService {

    private final ExternalApiService externalApiService;

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
                            List<String> batch = batches.get(i);
                            int batchNumber = i + 1;

                            // ✅ Обновляем прогресс

                            var jsonArray = convertToJsonArray(batch);
                            var response = externalApiService.sendToCisesInfo(jsonArray);

                            var cleanResponse = cleanResponse(response);

                            log.info("Пачка {}/{} обработана", batchNumber, batches.size());

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

    private String convertToJsonArray(List<String> values) {
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

    private List<String> readFirstColumn(Workbook workbook) {
        List<String> values = new ArrayList<>();
        var sheet = workbook.getSheetAt(0);

        if (sheet == null) {
            log.warn("Лист не найден");
            return values;
        }

        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue;
            }

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
                bytes[0] == (byte)0xD0 && bytes[1] == (byte)0xCF &&
                bytes[2] == (byte)0x11 && bytes[3] == (byte)0xE0 &&
                bytes[4] == (byte)0xA1 && bytes[5] == (byte)0xB1 &&
                bytes[6] == (byte)0x1A && bytes[7] == (byte)0xE1;
    }
}
