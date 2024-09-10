package io.github.gavinluo545.connector.utils.tcp.impl.disruptor.handler;

import io.github.gavinluo545.connector.utils.tcp.impl.RequestTimeoutTimer;
import io.github.gavinluo545.connector.utils.tcp.impl.TcpQPS;
import io.github.gavinluo545.connector.utils.tcp.impl.disruptor.SequenceId;
import io.github.gavinluo545.connector.utils.tcp.impl.message.FutureRequest;
import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Timeout;
import io.netty.util.Timer;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChannelCacheRequestEventHandler implements EventHandler<SequenceId>, WorkHandler<SequenceId> {
    private final Function<String, Channel> getChannelFunc;
    private final Function<Integer, FutureRequest> getFutureRequest;
    private final Consumer<Integer> finalizeFutureRequest;
    private static final ClosedChannelException closedChannelException = new ClosedChannelException();
    private final EventLoopGroup blockingServiceExecutor;

    public ChannelCacheRequestEventHandler(EventLoopGroup blockingServiceExecutor,
                                           Function<String, Channel> getChannelFunc,
                                           Function<Integer, FutureRequest> getFutureRequest, Consumer<Integer> finalizeFutureRequest) {
        this.blockingServiceExecutor = blockingServiceExecutor;
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
        CompletableFuture<FrameMessage> returnFuture = futureRequest.getFuture();
        BiConsumer<FrameMessage, Throwable> callback = futureRequest.getCallback();
        Byte sendRequestTimeoutSenconds = futureRequest.getSendRequestTimeoutSenconds();
        Byte waitResponseTimeoutSenconds = futureRequest.getWaitResponseTimeoutSenconds();

        Channel channel = getChannelFunc.apply(channelId);
        if (channel == null || !channel.isActive()) {
            try {
                if (returnFuture != null && unFinished(returnFuture)) {
                    returnFuture.completeExceptionally(closedChannelException);
                }
                if (callback != null) {
                    blockingServiceExecutor.execute(() -> {
                        try {
                            callback.accept(null, closedChannelException);
                        } catch (Exception ignored) {
                        }
                    });
                }
                return;
            } finally {
                finalizeFutureRequest.accept(id);
            }
        }

        ChannelPromise promise = channel.newPromise();
        Timeout waitTimeout;
        Timer requestTimeoutTimer = RequestTimeoutTimer.InstanceHolder.DEFAULT.requestTimeoutTimer;
        if (returnFuture != null) {
            waitTimeout = requestTimeoutTimer.newTimeout(timeout -> {
                if (timeout.isCancelled()) {
                    return;
                }
                if (!promise.isSuccess()) {
                    promise.cancel(true);
                } else {
                    //等待超时
                    finalizeFutureRequest.accept(id);
                }
            }, waitResponseTimeoutSenconds, TimeUnit.SECONDS);
        } else {
            waitTimeout = null;
        }
        Timeout sendTimeout = requestTimeoutTimer.newTimeout(timeout -> {
            if (timeout.isCancelled()) {
                return;
            }
            if (waitTimeout != null) {
                waitTimeout.cancel();
            }
            promise.cancel(true);
        }, sendRequestTimeoutSenconds, TimeUnit.SECONDS);

        promise.addListener((ChannelFutureListener) channelFuture -> {
            if (!channelFuture.isSuccess()) {
                try {
                    if (returnFuture != null && unFinished(returnFuture)) {
                        returnFuture.completeExceptionally(channelFuture.cause());
                    }
                    if (callback != null) {
                        blockingServiceExecutor.execute(() -> {
                            try {
                                callback.accept(null, channelFuture.cause());
                            } catch (Exception ignored) {
                            }
                        });
                    }
                } finally {
                    finalizeFutureRequest.accept(id);
                }
            } else {
                TcpQPS.requestQpsIncr();
                sendTimeout.cancel();
                if (waitTimeout != null) {
                    waitTimeout.cancel();
                }
            }
        });

        channel.writeAndFlush(message, promise);
    }

    public boolean unFinished(CompletableFuture<?> completableFuture) {
        return !completableFuture.isCancelled() && !completableFuture.isDone() && !completableFuture.isCompletedExceptionally();
    }
}

