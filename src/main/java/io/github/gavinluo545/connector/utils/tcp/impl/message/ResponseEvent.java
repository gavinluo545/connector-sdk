package io.github.gavinluo545.connector.utils.tcp.impl.message;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import io.netty.channel.Channel;
import lombok.Data;

import java.util.function.BiConsumer;

@Data
public class ResponseEvent {
    private String channelId;
    private FrameMessage frameMessage;
    private BiConsumer<Channel, FrameMessage> unknownMessageListener;
    private final byte unknownMeesageUnusedTimeoutSenconds;

    public ResponseEvent(String channelId, FrameMessage frameMessage, BiConsumer<Channel, FrameMessage> unknownMessageListener, byte unknownMeesageUnusedTimeoutSenconds) {
        this.channelId = channelId;
        this.frameMessage = frameMessage;
        this.unknownMessageListener = unknownMessageListener;
        this.unknownMeesageUnusedTimeoutSenconds = unknownMeesageUnusedTimeoutSenconds;
    }
}
