package ustin.cz.component;

import lombok.Getter;
import ustin.cz.component.ColumnNames.CertDocData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class CertDocSetters {

    @Getter
    private static final Map<String, BiConsumer<CertDocData, String>> CERT_SETTERS;

    static {
        CERT_SETTERS = new HashMap<>();
        CERT_SETTERS.put("type", CertDocData::setType);
        CERT_SETTERS.put("number", CertDocData::setNumber);
        CERT_SETTERS.put("date", CertDocData::setDate);
        CERT_SETTERS.put("statusGroup", CertDocData::setStatusGroup);
        CERT_SETTERS.put("indx", CertDocData::setIndx);
    }

}