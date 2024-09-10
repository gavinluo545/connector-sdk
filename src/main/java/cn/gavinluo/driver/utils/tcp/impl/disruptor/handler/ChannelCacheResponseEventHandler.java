package cn.gavinluo.driver.utils.tcp.impl.disruptor.handler;

import cn.gavinluo.driver.utils.tcp.impl.TcpQPS;
import cn.gavinluo.driver.utils.tcp.impl.disruptor.SequenceId;
import cn.gavinluo.driver.utils.tcp.impl.message.FutureRequest;
import cn.gavinluo.driver.utils.tcp.impl.message.FutureResponse;
import cn.gavinluo.driver.utils.tcp.message.FrameMessage;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import io.netty.channel.EventLoopGroup;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChannelCacheResponseEventHandler implements EventHandler<SequenceId>, WorkHandler<SequenceId> {
    private final Function<Integer, FutureResponse> getFutureResponse;
    private final Consumer<Integer> finalizeFutureResponse;
    private final Function<Integer, FutureRequest> getFutureRequest;
    private final Consumer<Integer> finalizeFutureRequest;
    private final EventLoopGroup blockingServiceExecutor;

    public ChannelCacheResponseEventHandler(EventLoopGroup blockingServiceExecutor,
                                            Function<Integer, FutureResponse> getFutureResponse,
                                            Consumer<Integer> finalizeFutureResponse, Function<Integer, FutureRequest> getFutureRequest, Consumer<Integer> finalizeFutureRequest) {
        this.blockingServiceExecutor = blockingServiceExecutor;
        this.getFutureResponse = getFutureResponse;
        this.finalizeFutureResponse = finalizeFutureResponse;
        this.getFutureRequest = getFutureRequest;
        this.finalizeFutureRequest = finalizeFutureRequest;
    }

    @Override
    public void onEvent(SequenceId event, long sequence, boolean endOfBatch) throws Exception {
        long start = System.nanoTime();
        executeResponse(event);
        long end = System.nanoTime();
        TcpQPS.channelCacheResponseEventHandlerAvgMicroAdd(end - start);
        TcpQPS.channelCacheResponseEventHandlerCountIncr();
    }

    @Override
    public void onEvent(SequenceId event) throws Exception {
        long start = System.nanoTime();
        executeResponse(event);
        long end = System.nanoTime();
        TcpQPS.channelCacheResponseEventHandlerAvgMicroAdd(end - start);
        TcpQPS.channelCacheResponseEventHandlerCountIncr();
    }

    public void executeResponse(SequenceId sequenceId) {
        int id = sequenceId.getSequenceId();
        FutureResponse futureResponse = null;
        FutureRequest futureRequest = null;
        try {
            futureResponse = getFutureResponse.apply(id);
            if (futureResponse == null) {
                return;
            }

            FrameMessage message = futureResponse.getMessage();
            int futureRequestSequenceId = futureResponse.getFutureRequestSequenceId();
            futureRequest = getFutureRequest.apply(futureRequestSequenceId);
            if (futureRequest == null) {
                return;
            }
            CompletableFuture<FrameMessage> returnFuture = futureRequest.getFuture();
            BiConsumer<FrameMessage, Throwable> callback = futureRequest.getCallback();
            if (returnFuture != null && unFinished(returnFuture)) {
                returnFuture.complete(message);
            }
            if (callback != null) {
                blockingServiceExecutor.execute(() -> {
                    try {
                        callback.accept(message, null);
                    } catch (Exception ignored) {
                    }
                });
            }
        } finally {
            if (futureResponse != null && futureRequest != null) {
                finalizeFutureRequest.accept(futureResponse.getFutureRequestSequenceId());
            }
            if (futureResponse != null) {
                finalizeFutureResponse.accept(id);
                TcpQPS.responseQpsIncr();
            }
        }
    }

    public boolean unFinished(CompletableFuture<?> completableFuture) {
        return !completableFuture.isCancelled() && !completableFuture.isDone() && !completableFuture.isCompletedExceptionally();
    }
}

