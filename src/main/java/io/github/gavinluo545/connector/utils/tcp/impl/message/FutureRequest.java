package io.github.gavinluo545.connector.utils.tcp.impl.message;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import lombok.Data;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Data
public class FutureRequest {
    private String channelId;
    private final FrameMessage message;
    private CompletableFuture<FrameMessage> future;
    private BiConsumer<FrameMessage, Throwable> callback;
    private Byte sendRequestTimeoutSenconds = 1;
    private Byte waitResponseTimeoutSenconds = 2;

    public FutureRequest(String channelId, FrameMessage message) {
        this.channelId = channelId;
        this.message = message;
    }

    public FutureRequest(String channelId, FrameMessage message, CompletableFuture<FrameMessage> future, byte waitResponseTimeoutSenconds, byte sendRequestTimeoutSenconds) {
        this.channelId = channelId;
        this.message = message;
        this.future = future;
        this.sendRequestTimeoutSenconds = sendRequestTimeoutSenconds;
        this.waitResponseTimeoutSenconds = waitResponseTimeoutSenconds;
    }

    public FutureRequest(String channelId, FrameMessage message, BiConsumer<FrameMessage, Throwable> callback, byte waitResponseTimeoutSenconds, byte sendRequestTimeoutSenconds) {
        this.channelId = channelId;
        this.message = message;
        this.callback = callback;
        this.sendRequestTimeoutSenconds = sendRequestTimeoutSenconds;
        this.waitResponseTimeoutSenconds = waitResponseTimeoutSenconds;
    }
}
