package ustin.cz.component;

import lombok.Data;

import java.util.Set;

@Data
public class ColumnSelection {

    private Set<String> selectedColumns;

    public Set<String> getSelectedColumns() {
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            return ColumnNames.getAllColumnNames();
        }
        return selectedColumns;
    }
}