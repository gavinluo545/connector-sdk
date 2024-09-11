package io.github.gavinluo545.connector.utils.tcp.impl.message;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import lombok.Data;

@Data
public class FutureResponse {
    private final FrameMessage message;
    private final int futureRequestSequenceId;

    public FutureResponse(FrameMessage message, int futureRequestSequenceId) {
        this.message = message;
        this.futureRequestSequenceId = futureRequestSequenceId;
    }

}
