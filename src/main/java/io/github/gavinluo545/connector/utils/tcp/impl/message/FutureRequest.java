package io.github.gavinluo545.connector.utils.tcp.impl.message;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Accessors(chain = true)
@NoArgsConstructor
@Data
public class FutureRequest {
    private String channelId;
    private FrameMessage message;
    private CompletableFuture<FrameMessage> future;
    private BiConsumer<FrameMessage, Throwable> callback;

    public FutureRequest(String channelId, FrameMessage message) {
        this.channelId = channelId;
        this.message = message;
    }

    public FutureRequest(String channelId, FrameMessage message, CompletableFuture<FrameMessage> future) {
        this.channelId = channelId;
        this.message = message;
        this.future = future;
    }

    public FutureRequest(String channelId, FrameMessage message, BiConsumer<FrameMessage, Throwable> callback) {
        this.channelId = channelId;
        this.message = message;
        this.callback = callback;
    }
}
