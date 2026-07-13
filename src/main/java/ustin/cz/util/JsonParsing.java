package ustin.cz.util;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import ustin.cz.component.CertDocSetters;
import ustin.cz.component.ColumnNames.CertDocData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class JsonParsing {

    public static List<CertDocData> parseCertDocs(JsonParser parser) {
        List<CertDocData> result = new ArrayList<>();

        if (parser.currentToken() != JsonToken.START_ARRAY) return result;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() == JsonToken.START_OBJECT) {
                var cert = new CertDocData();
                parseFields(parser, cert, CertDocSetters.getCERT_SETTERS());
                result.add(cert);
            }
        }
        return result;
    }

    private static <T> void parseFields(JsonParser parser, T target,
                                        Map<String, BiConsumer<T, String>> setters) {
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var name = parser.currentName();
            if (name == null) continue;
            parser.nextToken();
            var setter = setters.get(name);
            if (setter != null) {
                setter.accept(target, parser.getValueAsString());
            } else {
                parser.skipChildren();
            }
        }
    }
}