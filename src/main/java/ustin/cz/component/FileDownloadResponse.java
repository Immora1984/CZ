package ustin.cz.component;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDownloadResponse {
    private String filename;
    private byte[] content;
}