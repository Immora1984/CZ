package ustin.cz.component;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Data
public class ColumnSelection {

    @Getter
    @Setter
    private Set<String> selectedColumns = ColumnNames.getAllColumnNames();

}