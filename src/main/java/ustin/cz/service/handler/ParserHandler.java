package ustin.cz.service.handler;

import tools.jackson.core.JsonParser;
import ustin.cz.component.ReportType;

import java.util.List;

public interface ParserHandler {

    ReportType getType();

    List<Object[]> parse(JsonParser parser, List<String> columns);

}
