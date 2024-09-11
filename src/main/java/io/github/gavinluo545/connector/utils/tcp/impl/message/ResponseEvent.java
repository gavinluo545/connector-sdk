package io.github.gavinluo545.connector.utils.tcp.impl.message;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import lombok.Data;

@Data
public class ResponseEvent {
    private String channelId;
    private FrameMessage frameMessage;

    public ResponseEvent(String channelId, FrameMessage frameMessage) {
        this.channelId = channelId;
        this.frameMessage = frameMessage;
    }
}
