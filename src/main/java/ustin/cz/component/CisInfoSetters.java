package ustin.cz.component;

import lombok.Getter;
import lombok.Setter;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import java.util.*;
import java.util.function.BiConsumer;

@Getter
@Setter
public class CisInfoSetters {
    private String cis;
    private String gtin;
    private String productName;
    private String productGroup;
    private String productGroupId;
    private String brand;
    private String tnVedEaes;
    private String tnVedEaesGroup;
    private String manufacturerName;
    private String manufacturerInn;
    private String producerName;
    private String producerInn;
    private String ownerName;
    private String ownerInn;
    private String status;
    private String statusEx;
    private String withdrawReason;
    private String markWithdraw;
    private String isTracking;
    private String isMultipleSales;
    private String cisTrackingType;
    private String packageType;
    private String generalPackageType;
    private String emissionType;
    private String emissionDate;
    private String applicationDate;
    private String introducedDate;
    private String producedDate;
    private List<CertDocSetters> certDocs;

    @Getter
    @Setter
    public static class CertDocSetters {
        private String type;
        private String number;
        private String date;
        private String statusGroup;
        private String indx;

        private static final Map<String, BiConsumer<CertDocSetters, String>> CERT_SETTERS = new LinkedHashMap<>();
        static {
            CERT_SETTERS.put("type", CertDocSetters::setType);
            CERT_SETTERS.put("number", CertDocSetters::setNumber);
            CERT_SETTERS.put("date", CertDocSetters::setDate);
            CERT_SETTERS.put("statusGroup", CertDocSetters::setStatusGroup);
            CERT_SETTERS.put("indx", CertDocSetters::setIndx);
        }
    }

    public static final Map<String, BiConsumer<CisInfoSetters, JsonParser>> SETTERS = new LinkedHashMap<>();
    static {
        SETTERS.put("requestedCis", (d, p) -> d.setCis(p.getValueAsString()));
        SETTERS.put("cis", (d, p) -> d.setCis(p.getValueAsString()));
        SETTERS.put("gtin", (d, p) -> d.setGtin(p.getValueAsString()));
        SETTERS.put("productName", (d, p) -> d.setProductName(p.getValueAsString()));
        SETTERS.put("productGroup", (d, p) -> d.setProductGroup(p.getValueAsString()));
        SETTERS.put("productGroupId", (d, p) -> d.setProductGroupId(p.getValueAsString()));
        SETTERS.put("brand", (d, p) -> d.setBrand(p.getValueAsString()));
        SETTERS.put("tnVedEaes", (d, p) -> d.setTnVedEaes(p.getValueAsString()));
        SETTERS.put("tnVedEaesGroup", (d, p) -> d.setTnVedEaesGroup(p.getValueAsString()));
        SETTERS.put("manufacturerName", (d, p) -> d.setManufacturerName(p.getValueAsString()));
        SETTERS.put("manufacturerInn", (d, p) -> d.setManufacturerInn(p.getValueAsString()));
        SETTERS.put("producerName", (d, p) -> d.setProducerName(p.getValueAsString()));
        SETTERS.put("producerInn", (d, p) -> d.setProducerInn(p.getValueAsString()));
        SETTERS.put("ownerName", (d, p) -> d.setOwnerName(p.getValueAsString()));
        SETTERS.put("ownerInn", (d, p) -> d.setOwnerInn(p.getValueAsString()));
        SETTERS.put("status", (d, p) -> d.setStatus(p.getValueAsString()));
        SETTERS.put("statusEx", (d, p) -> d.setStatusEx(p.getValueAsString()));
        SETTERS.put("withdrawReason", (d, p) -> d.setWithdrawReason(p.getValueAsString()));
        SETTERS.put("markWithdraw", (d, p) -> d.setMarkWithdraw(p.getValueAsString()));
        SETTERS.put("isTracking", (d, p) -> d.setIsTracking(p.getValueAsString()));
        SETTERS.put("isMultipleSales", (d, p) -> d.setIsMultipleSales(p.getValueAsString()));
        SETTERS.put("cisTrackingType", (d, p) -> d.setCisTrackingType(p.getValueAsString()));
        SETTERS.put("packageType", (d, p) -> d.setPackageType(p.getValueAsString()));
        SETTERS.put("generalPackageType", (d, p) -> d.setGeneralPackageType(p.getValueAsString()));
        SETTERS.put("emissionType", (d, p) -> d.setEmissionType(p.getValueAsString()));
        SETTERS.put("emissionDate", (d, p) -> d.setEmissionDate(p.getValueAsString()));
        SETTERS.put("applicationDate", (d, p) -> d.setApplicationDate(p.getValueAsString()));
        SETTERS.put("introducedDate", (d, p) -> d.setIntroducedDate(p.getValueAsString()));
        SETTERS.put("producedDate", (d, p) -> d.setProducedDate(p.getValueAsString()));
        SETTERS.put("certDoc", (d, p) -> {if (p.currentToken() == JsonToken.START_ARRAY) {d.setCertDocs(parseCertDocs(p));}});
    }

    private static List<CertDocSetters> parseCertDocs(JsonParser parser) {
        List<CertDocSetters> list = new ArrayList<>();
        if (parser.currentToken() != JsonToken.START_ARRAY) return list;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() == JsonToken.START_OBJECT) {
                var cert = new CertDocSetters();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    var name = parser.currentName();
                    if (name == null) continue;
                    parser.nextToken();
                    var setter = CertDocSetters.CERT_SETTERS.get(name);
                    if (setter != null) {
                        setter.accept(cert, parser.getValueAsString());
                    } else {
                        parser.skipChildren();
                    }
                }
                list.add(cert);
            }
        }
        return list;
    }
}