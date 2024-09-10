package cn.gavinluo.driver.utils.tcp.impl;

import cn.gavinluo.driver.utils.executor.ExecutorFactory;
import cn.gavinluo.driver.utils.executor.NameThreadFactory;
import cn.gavinluo.driver.utils.executor.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class TcpQPS {
    private final AtomicLong channelCacheRequestEventHandlerAvgMicro = new AtomicLong();
    private final AtomicLong channelCacheResponseEventHandlerAvgMicro = new AtomicLong();
    private final AtomicLong unknownMessageEventHandlerAvgMicro = new AtomicLong();
    private final AtomicLong channelCacheRequestEventHandlerCount = new AtomicLong(1);
    private final AtomicLong channelCacheResponseEventHandlerCount = new AtomicLong(1);
    private final AtomicLong unknownMessageEventHandlerCount = new AtomicLong(1);
    private final AtomicLong requestSegmentedCacheAddQps = new AtomicLong();
    private final AtomicLong responseSegmentedCacheAddQps = new AtomicLong();
    private final AtomicLong unknownMessageSegmentedCacheAddQps = new AtomicLong();
    private final AtomicLong requestSegmentedCacheRemoveQps = new AtomicLong();
    private final AtomicLong responseSegmentedCacheRemoveQps = new AtomicLong();
    private final AtomicLong unknownMessageSegmentedCacheRemoveQps = new AtomicLong();
    private final AtomicLong requestQps = new AtomicLong();
    private final AtomicLong responseQps = new AtomicLong();
    private ScheduledExecutorService executorService;

    private volatile boolean enable = true;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private TcpQPS() {
        this.enabled();
    }

    private static class InstanceHolder {
        private static final TcpQPS INSTANCE = new TcpQPS();
    }

    public static void enable() {
        InstanceHolder.INSTANCE.enabled();
    }

    public static void diable() {
        InstanceHolder.INSTANCE.diabled();
    }

    private void diabled() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("TcpQPS diable.");
            enable = false;
            ThreadUtils.shutdownThreadPool(executorService);
            executorService = null;
        }
    }

    private void enabled() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("TcpQPS enable.");
            enable = true;
            executorService = ExecutorFactory.newSingleScheduledExecutorService("SystemTcpQPS", new NameThreadFactory(() -> "SystemTcpQPS"));
            executorService.scheduleWithFixedDelay(() -> {
                        long channelCacheRequestEventHandlerAvgMicroTen = TimeUnit.NANOSECONDS.toMicros(channelCacheRequestEventHandlerAvgMicro.getAndSet(0)) / channelCacheRequestEventHandlerCount.getAndSet(1);
                        long channelCacheResponseEventHandlerAvgMicroTen = TimeUnit.NANOSECONDS.toMicros(channelCacheResponseEventHandlerAvgMicro.getAndSet(0)) / channelCacheResponseEventHandlerCount.getAndSet(1);
                        long unknownMessageEventHandlerAvgMicroTen = TimeUnit.NANOSECONDS.toMicros(unknownMessageEventHandlerAvgMicro.getAndSet(0)) / unknownMessageEventHandlerCount.getAndSet(1);
                        long requestSegmentedCacheAddQpsTen = requestSegmentedCacheAddQps.getAndSet(0) / 100;
                        long responseSegmentedCacheAddQpsTen = responseSegmentedCacheAddQps.getAndSet(0) / 100;
                        long unknownMessageSegmentedCacheAddQpsTen = unknownMessageSegmentedCacheAddQps.getAndSet(0) / 100;
                        long requestSegmentedCacheRemoveQpsTen = requestSegmentedCacheRemoveQps.getAndSet(0) / 100;
                        long responseSegmentedCacheRemoveQpsTen = responseSegmentedCacheRemoveQps.getAndSet(0) / 100;
                        long unknownMessageSegmentedCacheRemoveQpsTen = unknownMessageSegmentedCacheRemoveQps.getAndSet(0) / 100;
                        long requestQpsTen = requestQps.getAndSet(0) / 100;
                        long responseQpsTen = responseQps.getAndSet(0) / 100;
                        log.info(
                                "\nchannelCacheRequestEventHandlerAvgMicro:{}/micro \n" +
                                        "channelCacheResponseEventHandlerAvgMicro:{}/micro \n" +
                                        "unknownMessageEventHandlerAvgMicro:{}/micro \n" +
                                        "requestSegmentedCacheAddQps:{}/s \n" +
                                        "responseSegmentedCacheAddQps:{}/s \n" +
                                        "unknownMessageSegmentedCacheAddQps:{}/s \n" +
                                        "requestSegmentedCacheRemoveQps:{}/s \n" +
                                        "responseSegmentedCacheRemoveQps:{}/s \n" +
                                        "unknownMessageSegmentedCacheRemoveQps:{}/s \n" +
                                        "requestQps:{}/s \n" +
                                        "responseQps:{}/s",
                                channelCacheRequestEventHandlerAvgMicroTen,
                                channelCacheResponseEventHandlerAvgMicroTen,
                                unknownMessageEventHandlerAvgMicroTen,
                                requestSegmentedCacheAddQpsTen,
                                responseSegmentedCacheAddQpsTen,
                                unknownMessageSegmentedCacheAddQpsTen,
                                requestSegmentedCacheRemoveQpsTen,
                                responseSegmentedCacheRemoveQpsTen,
                                unknownMessageSegmentedCacheRemoveQpsTen,
                                requestQpsTen,
                                responseQpsTen
                        );
                    }, 0, 100,
                    TimeUnit.SECONDS);
        }
    }

    public static void channelCacheRequestEventHandlerAvgMicroAdd(long add) {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.channelCacheRequestEventHandlerAvgMicro.addAndGet(add);
        }
    }

    public static void channelCacheResponseEventHandlerAvgMicroAdd(long add) {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.channelCacheResponseEventHandlerAvgMicro.addAndGet(add);
        }
    }

    public static void unknownMessageEventHandlerAvgMicroAdd(long add) {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.unknownMessageEventHandlerAvgMicro.addAndGet(add);
        }
    }

    public static void channelCacheRequestEventHandlerCountIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.channelCacheRequestEventHandlerCount.incrementAndGet();
        }
    }

    public static void channelCacheResponseEventHandlerCountIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.channelCacheResponseEventHandlerCount.incrementAndGet();
        }
    }

    public static void unknownMessageEventHandlerCountIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.unknownMessageEventHandlerCount.incrementAndGet();
        }
    }

    public static void requestSegmentedCacheAddQpsIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.requestSegmentedCacheAddQps.incrementAndGet();
        }
    }

    public static void responseSegmentedCacheAddQpsIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.responseSegmentedCacheAddQps.incrementAndGet();
        }
    }

    public static void unknownMessageSegmentedCacheAddQpsIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.unknownMessageSegmentedCacheAddQps.incrementAndGet();
        }
    }

    public static void requestSegmentedCacheRemoveQpsIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.requestSegmentedCacheRemoveQps.incrementAndGet();
        }
    }

    public static void responseSegmentedCacheRemoveQpsIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.responseSegmentedCacheRemoveQps.incrementAndGet();
        }
    }

    public static void unknownMessageSegmentedCacheRemoveQpsIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.unknownMessageSegmentedCacheRemoveQps.incrementAndGet();
        }
    }

    public static void requestQpsIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.requestQps.incrementAndGet();
        }
    }

    public static void responseQpsIncr() {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.responseQps.incrementAndGet();
        }
    }
}
