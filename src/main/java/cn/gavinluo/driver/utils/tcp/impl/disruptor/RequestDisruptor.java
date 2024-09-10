package cn.gavinluo.driver.utils.tcp.impl.disruptor;

import cn.gavinluo.driver.utils.executor.NameThreadFactory;
import cn.gavinluo.driver.utils.tcp.impl.disruptor.handler.ChannelCacheRequestEventHandler;
import cn.gavinluo.driver.utils.tcp.impl.disruptor.handler.LogExceptionHandler;
import cn.gavinluo.driver.utils.tcp.impl.message.FutureRequest;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class RequestDisruptor {
    public final Disruptor<SequenceId> requestDisruptor;

    @SuppressWarnings({"unchecked"})
    public RequestDisruptor(EventLoopGroup blockingServiceExecutor, String name, int ringBufferSize, int workHandlerNum,
                            Function<String, Channel> getChannelFunc,
                            Function<Integer, FutureRequest> getFutureRequest,
                            Consumer<Integer> finalizeFutureRequest) {
        this.requestDisruptor = new Disruptor<>(SequenceId::new, ringBufferSize, new NameThreadFactory(() -> name), ProducerType.MULTI, new BlockingWaitStrategy());
        this.requestDisruptor.handleEventsWithWorkerPool(IntStream.rangeClosed(0, workHandlerNum)
                .mapToObj(i -> new ChannelCacheRequestEventHandler(blockingServiceExecutor,
                        getChannelFunc, getFutureRequest, finalizeFutureRequest)).toArray(WorkHandler[]::new));
        this.requestDisruptor.setDefaultExceptionHandler(new LogExceptionHandler());
        this.requestDisruptor.start();
    }

    public void shutdown() {
        this.requestDisruptor.shutdown();
    }

    public RingBuffer<SequenceId> getRingBuffer() {
        return requestDisruptor.getRingBuffer();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();
    }
}
