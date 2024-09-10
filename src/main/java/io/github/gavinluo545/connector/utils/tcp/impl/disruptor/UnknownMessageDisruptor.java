package io.github.gavinluo545.connector.utils.tcp.impl.disruptor;

import io.github.gavinluo545.connector.utils.executor.NameThreadFactory;
import io.github.gavinluo545.connector.utils.tcp.impl.disruptor.handler.LogExceptionHandler;
import io.github.gavinluo545.connector.utils.tcp.impl.disruptor.handler.UnknownMessageEventHandler;
import io.github.gavinluo545.connector.utils.tcp.impl.message.ResponseEvent;
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

public class UnknownMessageDisruptor {
    public final Disruptor<SequenceId> unknownMessageDisruptor;

    @SuppressWarnings("unchecked")
    public UnknownMessageDisruptor(EventLoopGroup blockingServiceExecutor, String name, int ringBufferSize, int workHandlerNum,
                                   Function<String, Channel> getChannelFunc,
                                   Function<Integer, ResponseEvent> getResponseEvent,
                                   Consumer<Integer> finalizeResponseEvent) {
        this.unknownMessageDisruptor = new Disruptor<>(SequenceId::new, ringBufferSize, new NameThreadFactory(() -> name), ProducerType.MULTI, new BlockingWaitStrategy());
        this.unknownMessageDisruptor.handleEventsWithWorkerPool(IntStream.rangeClosed(0, workHandlerNum)
                .mapToObj(i -> new UnknownMessageEventHandler(blockingServiceExecutor, getChannelFunc,
                        getResponseEvent, finalizeResponseEvent)).toArray(WorkHandler[]::new));
        this.unknownMessageDisruptor.setDefaultExceptionHandler(new LogExceptionHandler());
        this.unknownMessageDisruptor.start();
    }

    public RingBuffer<SequenceId> getRingBuffer() {
        return unknownMessageDisruptor.getRingBuffer();
    }

    public void shutdown() {
        this.unknownMessageDisruptor.shutdown();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();
    }
}

