package ustin.cz.excel;

import lombok.Data;

import java.util.Set;

@Data
public class ColumnSelectionDto {
    private Set<String> selectedColumns;

    public Set<String> getSelectedColumns() {
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            return ColumnNames.getAllColumnNames();
        }
        return selectedColumns;
    }
}