package ustin.cz.component;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import ustin.cz.component.ColumnNames.CisInfoData;
import ustin.cz.util.JsonParsing;


import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class CisInfoSetters {

    public static final Map<String, BiConsumer<CisInfoData, String>> CIS_SETTERS;
    public static final Map<String, BiConsumer<CisInfoData, JsonParser>> CIS_CUSTOM;

    static {
        CIS_SETTERS = new HashMap<>();
        CIS_SETTERS.put("requestedCis", CisInfoData::setCis);
        CIS_SETTERS.put("cis", CisInfoData::setCis);
        CIS_SETTERS.put("gtin", CisInfoData::setGtin);
        CIS_SETTERS.put("productName", CisInfoData::setProductName);
        CIS_SETTERS.put("productGroup", CisInfoData::setProductGroup);
        CIS_SETTERS.put("productGroupId", CisInfoData::setProductGroupId);
        CIS_SETTERS.put("brand", CisInfoData::setBrand);
        CIS_SETTERS.put("tnVedEaes", CisInfoData::setTnVedEaes);
        CIS_SETTERS.put("tnVedEaesGroup", CisInfoData::setTnVedEaesGroup);
        CIS_SETTERS.put("manufacturerName", CisInfoData::setManufacturerName);
        CIS_SETTERS.put("manufacturerInn", CisInfoData::setManufacturerInn);
        CIS_SETTERS.put("producerName", CisInfoData::setProducerName);
        CIS_SETTERS.put("producerInn", CisInfoData::setProducerInn);
        CIS_SETTERS.put("ownerName", CisInfoData::setOwnerName);
        CIS_SETTERS.put("ownerInn", CisInfoData::setOwnerInn);
        CIS_SETTERS.put("status", CisInfoData::setStatus);
        CIS_SETTERS.put("statusEx", CisInfoData::setStatusEx);
        CIS_SETTERS.put("withdrawReason", CisInfoData::setWithdrawReason);
        CIS_SETTERS.put("markWithdraw", CisInfoData::setMarkWithdraw);
        CIS_SETTERS.put("isTracking", CisInfoData::setIsTracking);
        CIS_SETTERS.put("isMultipleSales", CisInfoData::setIsMultipleSales);
        CIS_SETTERS.put("cisTrackingType", CisInfoData::setCisTrackingType);
        CIS_SETTERS.put("packageType", CisInfoData::setPackageType);
        CIS_SETTERS.put("generalPackageType", CisInfoData::setGeneralPackageType);
        CIS_SETTERS.put("emissionType", CisInfoData::setEmissionType);
        CIS_SETTERS.put("emissionDate", CisInfoData::setEmissionDate);
        CIS_SETTERS.put("applicationDate", CisInfoData::setApplicationDate);
        CIS_SETTERS.put("introducedDate", CisInfoData::setIntroducedDate);
        CIS_SETTERS.put("producedDate", CisInfoData::setProducedDate);

        CIS_CUSTOM = new HashMap<>();
        CIS_CUSTOM.put("certDoc", (data, parser) -> {
            if (parser.currentToken() == JsonToken.START_ARRAY) data.setCertDocs(JsonParsing.parseCertDocs(parser));
        });
    }

}