package ustin.cz.component;

import lombok.Getter;
import lombok.Setter;


import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public enum ColumnNames {
    CIS("CIS", (d, c) -> d.cis),
    GTIN("GTIN", (d, c) -> d.gtin),
    PRODUCT_NAME("Product Name", (d, c) -> d.productName),
    PRODUCT_GROUP("Product Group", (d, c) -> d.productGroup),
    PRODUCT_GROUP_ID("Product Group ID", (d, c) -> d.productGroupId),
    BRAND("Brand", (d, c) -> d.brand),
    TN_VED_EAES("TN VED EAES", (d, c) -> d.tnVedEaes),
    TN_VED_EAES_GROUP("TN VED EAES Group", (d, c) -> d.tnVedEaesGroup),
    MANUFACTURER_NAME("Manufacturer Name", (d, c) -> d.manufacturerName),
    MANUFACTURER_INN("Manufacturer INN", (d, c) -> d.manufacturerInn),
    PRODUCER_NAME("Producer Name", (d, c) -> d.producerName),
    PRODUCER_INN("Producer INN", (d, c) -> d.producerInn),
    OWNER_NAME("Owner Name", (d, c) -> d.ownerName),
    OWNER_INN("Owner INN", (d, c) -> d.ownerInn),
    STATUS("Status", (d, c) -> d.status),
    STATUS_EX("Status Ex", (d, c) -> d.statusEx),
    WITHDRAW_REASON("Withdraw Reason", (d, c) -> d.withdrawReason),
    MARK_WITHDRAW("Mark Withdraw", (d, c) -> d.markWithdraw),
    IS_TRACKING("Is Tracking", (d, c) -> d.isTracking),
    IS_MULTIPLE_SALES("Is Multiple Sales", (d, c) -> d.isMultipleSales),
    CIS_TRACKING_TYPE("CIS Tracking Type", (d, c) -> d.cisTrackingType),
    PACKAGE_TYPE("Package Type", (d, c) -> d.packageType),
    GENERAL_PACKAGE_TYPE("General Package Type", (d, c) -> d.generalPackageType),
    EMISSION_TYPE("Emission Type", (d, c) -> d.emissionType),
    EMISSION_DATE("Emission Date", (d, c) -> d.emissionDate),
    APPLICATION_DATE("Application Date", (d, c) -> d.applicationDate),
    INTRODUCED_DATE("Introduced Date", (d, c) -> d.introducedDate),
    PRODUCED_DATE("Produced Date", (d, c) -> d.producedDate),
    CERTIFICATE_TYPE("Certificate Type", (d, c) -> c != null ? c.type : "Нет данных"),
    CERTIFICATE_NUMBER("Certificate Number", (d, c) -> c != null ? c.number : "Нет данных"),
    CERTIFICATE_DATE("Certificate Date", (d, c) -> c != null ? c.date : "Нет данных"),
    CERTIFICATE_STATUS_GROUP("Certificate Status Group", (d, c) -> c != null ? c.statusGroup : "Нет данных"),
    CERTIFICATE_INDEX("Certificate Index", (d, c) -> c != null ? c.indx : "Нет данных");

    @Getter
    private final String displayName;
    private final BiFunction<CisInfoData, CertDocData, String> extractor;

    ColumnNames(String displayName, BiFunction<CisInfoData, CertDocData, String> extractor) {
        this.displayName = displayName;
        this.extractor = extractor;
    }

    public String extract(CisInfoData data, CertDocData cert) {
        return extractor.apply(data, cert);
    }

    // Получить все имена колонок (для дефолтного набора)
    public static Set<String> getAllNames() {
        return Arrays.stream(values())
                .map(ColumnNames::getDisplayName)
                .collect(Collectors.toSet());
    }

    // Получить Map "displayName -> extractor" для использования в EXTRACTORS
    public static Map<String, ColumnExtractor> asExtractorMap() {
        return Arrays.stream(values())
                .collect(Collectors.toMap(
                        ColumnNames::getDisplayName,
                        col -> col::extract
                ));
    }

    @FunctionalInterface
    public interface ColumnExtractor { String extract(CisInfoData data, CertDocData cert);}

    @Getter
    @Setter
    public static class CisInfoData {
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

    @Getter
    @Setter
    public static class CertDocData {
        String type;
        String number;
        String date;
        String statusGroup;
        String indx;
    }
}