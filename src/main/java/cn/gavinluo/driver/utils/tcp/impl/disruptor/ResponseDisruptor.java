package cn.gavinluo.driver.utils.tcp.impl.disruptor;

import cn.gavinluo.driver.utils.executor.NameThreadFactory;
import cn.gavinluo.driver.utils.tcp.impl.disruptor.handler.ChannelCacheResponseEventHandler;
import cn.gavinluo.driver.utils.tcp.impl.disruptor.handler.LogExceptionHandler;
import cn.gavinluo.driver.utils.tcp.impl.message.FutureRequest;
import cn.gavinluo.driver.utils.tcp.impl.message.FutureResponse;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.EventLoopGroup;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ResponseDisruptor {
    public Disruptor<SequenceId> responseDisruptor;

    @SuppressWarnings({"unchecked"})
    public ResponseDisruptor(EventLoopGroup blockingServiceExecutor, String name, int ringBufferSize, int workHandlerNum,
                             Function<Integer, FutureResponse> getFutureResponse, Consumer<Integer> finalizeFutureResponse,
                             Function<Integer, FutureRequest> getFutureRequest, Consumer<Integer> finalizeFutureRequest) {
        this.responseDisruptor = new Disruptor<>(SequenceId::new, ringBufferSize, new NameThreadFactory(() -> name), ProducerType.MULTI, new BlockingWaitStrategy());
        this.responseDisruptor.handleEventsWithWorkerPool(IntStream.rangeClosed(0, workHandlerNum)
                .mapToObj(i -> new ChannelCacheResponseEventHandler(blockingServiceExecutor, getFutureResponse,
                        finalizeFutureResponse,
                        getFutureRequest,
                        finalizeFutureRequest)).toArray(WorkHandler[]::new));
        this.responseDisruptor.setDefaultExceptionHandler(new LogExceptionHandler());
        this.responseDisruptor.start();
    }

    public RingBuffer<SequenceId> getRingBuffer() {
        return responseDisruptor.getRingBuffer();
    }

    public void shutdown() {
        this.responseDisruptor.shutdown();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();
    }
}

