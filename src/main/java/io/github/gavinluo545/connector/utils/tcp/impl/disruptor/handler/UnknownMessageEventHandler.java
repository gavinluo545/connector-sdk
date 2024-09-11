package io.github.gavinluo545.connector.utils.tcp.impl.disruptor.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import io.github.gavinluo545.connector.utils.tcp.impl.TcpQPS;
import io.github.gavinluo545.connector.utils.tcp.impl.disruptor.SequenceId;
import io.github.gavinluo545.connector.utils.tcp.impl.message.ResponseEvent;
import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class UnknownMessageEventHandler implements EventHandler<SequenceId>, WorkHandler<SequenceId> {
    private final Function<String, Channel> getChannelFunc;
    private final Function<Integer, ResponseEvent> getResponseEvent;
    private final Function<String/*serverId-clientId clientId-clientId*/, BiConsumer<Channel, FrameMessage> /*unknownMessageListener*/> unknownMessageListenerGet;
    private final Consumer<Integer> finalizeResponseEvent;
    private final EventLoopGroup blockingServiceExecutor;

    public UnknownMessageEventHandler(EventLoopGroup blockingServiceExecutor, Function<String/*serverId-clientId clientId-clientId*/, BiConsumer<Channel, FrameMessage> /*unknownMessageListener*/> unknownMessageListenerGet,
                                      Function<String, Channel> getChannelFunc, Function<Integer, ResponseEvent> getResponseEvent, Consumer<Integer> finalizeResponseEvent) {
        this.unknownMessageListenerGet = unknownMessageListenerGet;
        this.blockingServiceExecutor = blockingServiceExecutor;
        this.getChannelFunc = getChannelFunc;
        this.getResponseEvent = getResponseEvent;
        this.finalizeResponseEvent = finalizeResponseEvent;
    }

    @Override
    public void onEvent(SequenceId event, long sequence, boolean endOfBatch) throws Exception {
        long start = System.nanoTime();
        executeResponse(event);
        long end = System.nanoTime();
        TcpQPS.unknownMessageEventHandlerAvgMicroAdd(end - start);
        TcpQPS.unknownMessageEventHandlerCountIncr();
    }

    @Override
    public void onEvent(SequenceId event) throws Exception {
        long start = System.nanoTime();
        executeResponse(event);
        long end = System.nanoTime();
        TcpQPS.unknownMessageEventHandlerAvgMicroAdd(end - start);
        TcpQPS.unknownMessageEventHandlerCountIncr();
    }

    public void executeResponse(SequenceId sequenceId) {
        int id = sequenceId.getSequenceId();
        ResponseEvent event = getResponseEvent.apply(id);
        if (event == null) {
            return;
        }
        BiConsumer<Channel, FrameMessage> unknownMessageListener = unknownMessageListenerGet.apply(event.getChannelId());
        if (unknownMessageListener == null) {
            return;
        }
        try {
            FrameMessage frameMessage = event.getFrameMessage();
            Channel channel = getChannelFunc.apply(event.getChannelId());
            blockingServiceExecutor.execute(() -> {
                try {
                    unknownMessageListener.accept(channel, frameMessage);
                } catch (Exception ignored) {
                }
            });
        } finally {
            finalizeResponseEvent.accept(id);
            TcpQPS.responseQpsIncr();
        }
    }
}

