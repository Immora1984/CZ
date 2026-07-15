package ustin.cz.service.handler.impl;

import org.springframework.stereotype.Component;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import ustin.cz.component.CisInfoSetters;
import ustin.cz.component.CisInfoSetters.CertDocSetters;
import ustin.cz.component.ColumnNames;
import ustin.cz.component.ReportType;
import ustin.cz.service.handler.ParserHandler;

import java.util.ArrayList;
import java.util.List;

import static ustin.cz.service.impl.FileHandlerServiceImpl.EXTRACTORS;

@Component
public class CisInfoParserHandlerImpl implements ParserHandler {

    @Override
    public ReportType getType() {return ReportType.CIS_INFO;}


    @Override
    public List<Object[]> parse(JsonParser parser, List<String> columns) {
        if (parser.currentToken() != JsonToken.START_OBJECT) return List.of();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var name = parser.currentName();
            if (name == null) continue;
            parser.nextToken();
            if ("cisInfo".equals(name) && parser.currentToken() == JsonToken.START_OBJECT) {
                return parseObject(parser, columns);
            }
        }
        return List.of();
    }

    private List<Object[]> parseObject(JsonParser parser, List<String> columns) {
        var data = new CisInfoSetters();
        List<Object[]> result = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var name = parser.currentName();
            if (name == null) continue;
            parser.nextToken();

            var setter = CisInfoSetters.SETTERS.get(name);
            if (setter != null) {
                setter.accept(data, parser);
            } else {
                parser.skipChildren();
            }
        }

        var orderedColumns = ColumnNames.sortByOrder(columns);

        var certs = data.getCertDocs();
        if (certs != null && !certs.isEmpty()) {
            for (CertDocSetters cert : certs)
                result.add(toRowArray(data, cert, orderedColumns));
        } else if (data.getCis() != null)
            result.add(toRowArray(data, null, orderedColumns));

        return result;
    }

    private Object[] toRowArray(CisInfoSetters data, CertDocSetters cert, List<String> columns) {
        var row = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            row[i] = EXTRACTORS.get(columns.get(i)).extract(data, cert);
        }
        return row;
    }
}