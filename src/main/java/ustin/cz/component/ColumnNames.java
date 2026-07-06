package ustin.cz.component;

import java.util.Set;
import java.util.LinkedHashSet;

public class ColumnNames {
    public static final String CIS = "CIS";
    public static final String GTIN = "GTIN";
    public static final String PRODUCT_NAME = "Product Name";
    public static final String PRODUCT_GROUP = "Product Group";
    public static final String PRODUCT_GROUP_ID = "Product Group ID";
    public static final String BRAND = "Brand";
    public static final String TN_VED_EAES = "TN VED EAES";
    public static final String TN_VED_EAES_GROUP = "TN VED EAES Group";
    public static final String MANUFACTURER_NAME = "Manufacturer Name";
    public static final String MANUFACTURER_INN = "Manufacturer INN";
    public static final String PRODUCER_NAME = "Producer Name";
    public static final String PRODUCER_INN = "Producer INN";
    public static final String OWNER_NAME = "Owner Name";
    public static final String OWNER_INN = "Owner INN";
    public static final String STATUS = "Status";
    public static final String STATUS_EX = "Status Ex";
    public static final String WITHDRAW_REASON = "Withdraw Reason";
    public static final String MARK_WITHDRAW = "Mark Withdraw";
    public static final String IS_TRACKING = "Is Tracking";
    public static final String IS_MULTIPLE_SALES = "Is Multiple Sales";
    public static final String CIS_TRACKING_TYPE = "CIS Tracking Type";
    public static final String PACKAGE_TYPE = "Package Type";
    public static final String GENERAL_PACKAGE_TYPE = "General Package Type";
    public static final String EMISSION_TYPE = "Emission Type";
    public static final String EMISSION_DATE = "Emission Date";
    public static final String APPLICATION_DATE = "Application Date";
    public static final String INTRODUCED_DATE = "Introduced Date";
    public static final String PRODUCED_DATE = "Produced Date";
    public static final String CERTIFICATE_TYPE = "Certificate Type";
    public static final String CERTIFICATE_NUMBER = "Certificate Number";
    public static final String CERTIFICATE_DATE = "Certificate Date";
    public static final String CERTIFICATE_STATUS_GROUP = "Certificate Status Group";
    public static final String CERTIFICATE_INDEX = "Certificate Index";

    public static Set<String> getAllColumnNames() {
        Set<String> columns = new LinkedHashSet<>();
        columns.add(CIS);
        columns.add(GTIN);
        columns.add(PRODUCT_NAME);
        columns.add(PRODUCT_GROUP);
        columns.add(PRODUCT_GROUP_ID);
        columns.add(BRAND);
        columns.add(TN_VED_EAES);
        columns.add(TN_VED_EAES_GROUP);
        columns.add(MANUFACTURER_NAME);
        columns.add(MANUFACTURER_INN);
        columns.add(PRODUCER_NAME);
        columns.add(PRODUCER_INN);
        columns.add(OWNER_NAME);
        columns.add(OWNER_INN);
        columns.add(STATUS);
        columns.add(STATUS_EX);
        columns.add(WITHDRAW_REASON);
        columns.add(MARK_WITHDRAW);
        columns.add(IS_TRACKING);
        columns.add(IS_MULTIPLE_SALES);
        columns.add(CIS_TRACKING_TYPE);
        columns.add(PACKAGE_TYPE);
        columns.add(GENERAL_PACKAGE_TYPE);
        columns.add(EMISSION_TYPE);
        columns.add(EMISSION_DATE);
        columns.add(APPLICATION_DATE);
        columns.add(INTRODUCED_DATE);
        columns.add(PRODUCED_DATE);
        columns.add(CERTIFICATE_TYPE);
        columns.add(CERTIFICATE_NUMBER);
        columns.add(CERTIFICATE_DATE);
        columns.add(CERTIFICATE_STATUS_GROUP);
        columns.add(CERTIFICATE_INDEX);
        return columns;
    }
}