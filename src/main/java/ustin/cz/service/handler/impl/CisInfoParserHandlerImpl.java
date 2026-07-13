package ustin.cz.service.handler.impl;

import org.springframework.stereotype.Component;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import ustin.cz.component.ColumnNames;
import ustin.cz.component.ColumnNames.CertDocData;
import ustin.cz.component.ColumnNames.CisInfoData;
import ustin.cz.component.ReportType;
import ustin.cz.service.handler.ParserHandler;

import java.util.ArrayList;
import java.util.List;

import static ustin.cz.service.impl.FileHandlerServiceImpl.EXTRACTORS;

@Component
public class CisInfoParserHandlerImpl implements ParserHandler {

    @Override
    public ReportType getType() {
        return ReportType.CIS_INFO;
    }

    @Override
    public List<Object[]> parse(JsonParser parser, List<String> columns) {
        List<Object[]> result = new ArrayList<>();

        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return result;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var name = parser.currentName();
            if (name == null) continue;

            parser.nextToken();

            if ("cisInfo".equals(name) && parser.currentToken() == JsonToken.START_OBJECT)
                return parseObject(parser, columns);
        }
        return result;
    }

    private List<Object[]> parseObject(JsonParser parser, List<String> columns) {
        var data = new CisInfoData();
        List<Object[]> result = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var name = parser.currentName();
            if (name == null) continue;

            parser.nextToken();

            switch (name) {
                case "requestedCis":
                case "cis":
                    data.setCis(parser.getValueAsString());
                    break;
                case "gtin":
                    data.setGtin(parser.getValueAsString());
                    break;
                case "productName":
                    data.setProductName(parser.getValueAsString());
                    break;
                case "productGroup":
                    data.setProductGroup(parser.getValueAsString());
                    break;
                case "productGroupId":
                    data.setProductGroupId(parser.getValueAsString());
                    break;
                case "brand":
                    data.setBrand(parser.getValueAsString());
                    break;
                case "tnVedEaes":
                    data.setTnVedEaes(parser.getValueAsString());
                    break;
                case "tnVedEaesGroup":
                    data.setTnVedEaesGroup(parser.getValueAsString());
                    break;
                case "manufacturerName":
                    data.setManufacturerName(parser.getValueAsString());
                    break;
                case "manufacturerInn":
                    data.setManufacturerInn(parser.getValueAsString());
                    break;
                case "producerName":
                    data.setProducerName(parser.getValueAsString());
                    break;
                case "producerInn":
                    data.setProducerInn(parser.getValueAsString());
                    break;
                case "ownerName":
                    data.setOwnerName(parser.getValueAsString());
                    break;
                case "ownerInn":
                    data.setOwnerInn(parser.getValueAsString());
                    break;
                case "status":
                    data.setStatus(parser.getValueAsString());
                    break;
                case "statusEx":
                    data.setStatusEx(parser.getValueAsString());
                    break;
                case "withdrawReason":
                    data.setWithdrawReason(parser.getValueAsString());
                    break;
                case "markWithdraw":
                    data.setMarkWithdraw(parser.getValueAsString());
                    break;
                case "isTracking":
                    data.setIsTracking(parser.getValueAsString());
                    break;
                case "isMultipleSales":
                    data.setIsMultipleSales(parser.getValueAsString());
                    break;
                case "cisTrackingType":
                    data.setCisTrackingType(parser.getValueAsString());
                    break;
                case "packageType":
                    data.setPackageType(parser.getValueAsString());
                    break;
                case "generalPackageType":
                    data.setGeneralPackageType(parser.getValueAsString());
                    break;
                case "emissionType":
                    data.setEmissionType(parser.getValueAsString());
                    break;
                case "emissionDate":
                    data.setEmissionDate(parser.getValueAsString());
                    break;
                case "applicationDate":
                    data.setApplicationDate(parser.getValueAsString());
                    break;
                case "introducedDate":
                    data.setIntroducedDate(parser.getValueAsString());
                    break;
                case "producedDate":
                    data.setProducedDate(parser.getValueAsString());
                    break;
                case "certDoc":
                    if (parser.currentToken() == JsonToken.START_ARRAY) data.setCertDocs(parseCertDocs(parser));
                    break;
                default:
                    parser.skipChildren();
                    break;
            }
        }

        if (!data.getCertDocs().isEmpty()) {
            for (CertDocData cert : data.getCertDocs()) {
                result.add(toRowArray(data, cert, columns));
            }
        } else if (data.getCis() != null) {
            result.add(toRowArray(data, null, columns));
        }
        return result;
    }

    private List<CertDocData> parseCertDocs(JsonParser parser) {
        List<CertDocData> result = new ArrayList<>();

        if (parser.currentToken() != JsonToken.START_ARRAY) return result;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() == JsonToken.START_OBJECT) {
                var cert = new CertDocData();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    var name = parser.currentName();
                    if (name == null) continue;

                    parser.nextToken();

                    switch (name) {
                        case "type":
                            cert.setType(parser.getValueAsString());
                            break;
                        case "number":
                            cert.setNumber(parser.getValueAsString());
                            break;
                        case "date":
                            cert.setDate(parser.getValueAsString());
                            break;
                        case "statusGroup":
                            cert.setStatusGroup(parser.getValueAsString());
                            break;
                        case "indx":
                            cert.setIndx(parser.getValueAsString());
                            break;
                        default:
                            parser.skipChildren();
                            break;
                    }
                }
                result.add(cert);
            }
        }
        return result;
    }

    private Object[] toRowArray(CisInfoData data, CertDocData cert, List<String> columns) {
        var row = new Object[columns.size()];

        for (int i = 0; i < columns.size(); i++) row[i] = EXTRACTORS.get(columns.get(i)).extract(data, cert);

        return row;
    }
}