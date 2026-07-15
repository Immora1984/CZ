package ustin.cz.component;

import lombok.Getter;
import ustin.cz.component.CisInfoSetters.CertDocSetters;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public enum ColumnNames {
    CIS("CIS", (d, c) -> d.getCis()),
    GTIN("GTIN", (d, c) -> d.getGtin()),
    PRODUCT_NAME("Product Name", (d, c) -> d.getProductName()),
    PRODUCT_GROUP("Product Group", (d, c) -> d.getProductGroup()),
    PRODUCT_GROUP_ID("Product Group ID", (d, c) -> d.getProductGroupId()),
    BRAND("Brand", (d, c) -> d.getBrand()),
    TN_VED_EAES("TN VED EAES", (d, c) -> d.getTnVedEaes()),
    TN_VED_EAES_GROUP("TN VED EAES Group", (d, c) -> d.getTnVedEaesGroup()),
    MANUFACTURER_NAME("Manufacturer Name", (d, c) -> d.getManufacturerName()),
    MANUFACTURER_INN("Manufacturer INN", (d, c) -> d.getManufacturerInn()),
    PRODUCER_NAME("Producer Name", (d, c) -> d.getProducerName()),
    PRODUCER_INN("Producer INN", (d, c) -> d.getProducerInn()),
    OWNER_NAME("Owner Name", (d, c) -> d.getOwnerName()),
    OWNER_INN("Owner INN", (d, c) -> d.getOwnerInn()),
    STATUS("Status", (d, c) -> d.getStatus()),
    STATUS_EX("Status Ex", (d, c) -> d.getStatusEx()),
    WITHDRAW_REASON("Withdraw Reason", (d, c) -> d.getWithdrawReason()),
    MARK_WITHDRAW("Mark Withdraw", (d, c) -> d.getMarkWithdraw()),
    IS_TRACKING("Is Tracking", (d, c) -> d.getIsTracking()),
    IS_MULTIPLE_SALES("Is Multiple Sales", (d, c) -> d.getIsMultipleSales()),
    CIS_TRACKING_TYPE("CIS Tracking Type", (d, c) -> d.getCisTrackingType()),
    PACKAGE_TYPE("Package Type", (d, c) -> d.getPackageType()),
    GENERAL_PACKAGE_TYPE("General Package Type", (d, c) -> d.getGeneralPackageType()),
    EMISSION_TYPE("Emission Type", (d, c) -> d.getEmissionType()),
    EMISSION_DATE("Emission Date", (d, c) -> d.getEmissionDate()),
    APPLICATION_DATE("Application Date", (d, c) -> d.getApplicationDate()),
    INTRODUCED_DATE("Introduced Date", (d, c) -> d.getIntroducedDate()),
    PRODUCED_DATE("Produced Date", (d, c) -> d.getProducedDate()),
    CERTIFICATE_TYPE("Certificate Type", (d, c) -> c != null ? c.getType() : "Нет данных"),
    CERTIFICATE_NUMBER("Certificate Number", (d, c) -> c != null ? c.getNumber() : "Нет данных"),
    CERTIFICATE_DATE("Certificate Date", (d, c) -> c != null ? c.getDate() : "Нет данных"),
    CERTIFICATE_STATUS_GROUP("Certificate Status Group", (d, c) -> c != null ? c.getStatusGroup() : "Нет данных"),
    CERTIFICATE_INDEX("Certificate Index", (d, c) -> c != null ? c.getIndx() : "Нет данных");

    @Getter
    private final String displayName;
    private final BiFunction<CisInfoSetters, CertDocSetters, String> extractor;

    ColumnNames(String displayName, BiFunction<CisInfoSetters, CertDocSetters, String> extractor) {
        this.displayName = displayName;
        this.extractor = extractor;
    }

    public static List<String> sortByOrder(Collection<String> names) {
        var order = IntStream.range(0, values().length)
                .boxed()
                .collect(Collectors.toMap(i -> values()[i].getDisplayName(), i -> i));

        return names.stream()
                .filter(order::containsKey)
                .sorted(Comparator.comparingInt(order::get))
                .collect(Collectors.toList());
    }


    public String extract(CisInfoSetters data, CertDocSetters cert) {
        return extractor.apply(data, cert);
    }

    public static Set<String> getAllNames() {
        return Arrays.stream(values())
                .map(ColumnNames::getDisplayName)
                .collect(Collectors.toSet());
    }

    public static Map<String, ColumnExtractor> asExtractorMap() {
        return Arrays.stream(values())
                .collect(Collectors.toMap(
                        ColumnNames::getDisplayName,
                        col -> col::extract
                ));
    }

    @FunctionalInterface
    public interface ColumnExtractor { String extract(CisInfoSetters data, CertDocSetters cert);}

}