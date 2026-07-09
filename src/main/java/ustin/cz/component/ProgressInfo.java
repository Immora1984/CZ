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
public class ProgressInfo implements Message {
    private RequestStatus status;
    private int currentBatch;
    private int totalBatches;
    private String errorDetails;
    private UUID taskId;
}