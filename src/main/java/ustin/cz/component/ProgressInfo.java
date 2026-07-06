package ustin.cz.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressInfo {
    private RequestStatus status;
    private int progress;
    private int currentBatch;
    private int totalBatches;
    private String message;
    private UUID fileId;
    private String errorDetails;
    private UUID taskId;
    private String startTime;
    private String endTime;
}