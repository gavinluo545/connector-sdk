package io.github.gavinluo545.connector.utils.tcp.impl.disruptor.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import io.github.gavinluo545.connector.utils.tcp.impl.RequestTimeoutTimer;
import io.github.gavinluo545.connector.utils.tcp.impl.TcpQPS;
import io.github.gavinluo545.connector.utils.tcp.impl.disruptor.SequenceId;
import io.github.gavinluo545.connector.utils.tcp.impl.message.FutureRequest;
import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Timeout;
import io.netty.util.Timer;

import java.nio.channels.ClosedChannelException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChannelCacheRequestEventHandler implements EventHandler<SequenceId>, WorkHandler<SequenceId> {
    private final Function<String, Channel> getChannelFunc;
    private final Function<Integer, FutureRequest> getFutureRequest;
    private final Consumer<Integer> finalizeFutureRequest;
    private final Function<String/*serverId-clientId clientId-clientId*/, Byte/*timeoutSenconds*/> sendRequestTimeoutSencondsGet;
    private static final ClosedChannelException closedChannelException = new ClosedChannelException();
    private final EventLoopGroup blockingServiceExecutor;

    public ChannelCacheRequestEventHandler(EventLoopGroup blockingServiceExecutor,
                                           Function<String/*serverId-clientId clientId-clientId*/, Byte/*timeoutSenconds*/> sendRequestTimeoutSencondsGet,
                                           Function<String, Channel> getChannelFunc,
                                           Function<Integer, FutureRequest> getFutureRequest, Consumer<Integer> finalizeFutureRequest) {
        this.blockingServiceExecutor = blockingServiceExecutor;
        this.sendRequestTimeoutSencondsGet = sendRequestTimeoutSencondsGet;
        this.getChannelFunc = getChannelFunc;
        this.getFutureRequest = getFutureRequest;
        this.finalizeFutureRequest = finalizeFutureRequest;
    }

    @Override
    public void onEvent(SequenceId event, long sequence, boolean endOfBatch) throws Exception {
        long start = System.nanoTime();
        executeRequest(event);
        long end = System.nanoTime();
        TcpQPS.channelCacheRequestEventHandlerAvgMicroAdd(end - start);
        TcpQPS.channelCacheRequestEventHandlerCountIncr();
    }

    @Override
    public void onEvent(SequenceId event) throws Exception {
        long start = System.nanoTime();
        executeRequest(event);
        long end = System.nanoTime();
        TcpQPS.channelCacheRequestEventHandlerAvgMicroAdd(end - start);
        TcpQPS.channelCacheRequestEventHandlerCountIncr();
    }

    public void executeRequest(SequenceId sequenceId) {
        int id = sequenceId.getSequenceId();
        FutureRequest futureRequest = getFutureRequest.apply(id);
        if (futureRequest == null) {
            return;
        }
        String channelId = futureRequest.getChannelId();
        FrameMessage message = futureRequest.getMessage();

        Channel channel = getChannelFunc.apply(channelId);
        if (channel == null || !channel.isActive()) {
            failed(futureRequest, id, closedChannelException);
            return;
        }

        ChannelPromise promise = channel.newPromise();
        Timer requestTimeoutTimer = RequestTimeoutTimer.InstanceHolder.DEFAULT.requestTimeoutTimer;

        Timeout sendTimeout = requestTimeoutTimer.newTimeout(timeout -> {
            if (timeout.isCancelled()) {
                return;
            }
            promise.cancel(true);
        }, Optional.ofNullable(sendRequestTimeoutSencondsGet.apply(channelId)).orElse((byte) 1), TimeUnit.SECONDS);

        promise.addListener((ChannelFutureListener) channelFuture -> {
            if (!channelFuture.isSuccess()) {
                failed(futureRequest, id, channelFuture.cause());
            } else {
                TcpQPS.requestQpsIncr();
                sendTimeout.cancel();
            }
        });

        channel.writeAndFlush(message, promise);
    }

    public boolean unFinished(CompletableFuture<?> completableFuture) {
        return !completableFuture.isCancelled() && !completableFuture.isDone() && !completableFuture.isCompletedExceptionally();
    }

    public void failed(FutureRequest futureRequest, int id, Throwable throwable) {
        CompletableFuture<FrameMessage> returnFuture = futureRequest.getFuture();
        BiConsumer<FrameMessage, Throwable> callback = futureRequest.getCallback();
        if (returnFuture != null && unFinished(returnFuture)) {
            try {
                returnFuture.completeExceptionally(throwable);
            } finally {
                finalizeFutureRequest.accept(id);
            }
        }
        if (callback != null) {
            blockingServiceExecutor.execute(() -> {
                try {
                    callback.accept(null, throwable);
                } catch (Exception ignored) {
                } finally {
                    finalizeFutureRequest.accept(id);
                }
            });
        }
    }
}

