package cn.gavinluo.driver.connection;

import cn.gavinluo.driver.utils.executor.ExecutorFactory;
import cn.gavinluo.driver.utils.executor.NameThreadFactory;
import cn.gavinluo.driver.utils.executor.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class TagReportQPS {
    private final AtomicLong success = new AtomicLong();
    private ScheduledExecutorService executorService;

    private volatile boolean enable = true;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private TagReportQPS() {
        this.enabled();
    }

    private static class InstanceHolder {
        private static final TagReportQPS INSTANCE = new TagReportQPS();
    }

    public static void enable() {
        InstanceHolder.INSTANCE.enabled();
    }

    public static void diable() {
        InstanceHolder.INSTANCE.diabled();
    }

    private void diabled() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("TagReportQPS diable.");
            enable = false;
            ThreadUtils.shutdownThreadPool(executorService);
            executorService = null;
        }
    }

    private void enabled() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("TagReportQPS enable.");
            enable = true;
            executorService = ExecutorFactory.newSingleScheduledExecutorService("TagReportQPS", new NameThreadFactory(() -> "TagReportQPS"));
            executorService.scheduleWithFixedDelay(() -> {
                        long successed = success.getAndSet(0) / 10;
                        log.info("TagReportQPS:{}/s", successed);
                    }, 0, 10,
                    TimeUnit.SECONDS);
        }
    }

    public static void successAdd(long add) {
        if (InstanceHolder.INSTANCE.enable) {
            InstanceHolder.INSTANCE.success.addAndGet(add);
        }
    }

}
