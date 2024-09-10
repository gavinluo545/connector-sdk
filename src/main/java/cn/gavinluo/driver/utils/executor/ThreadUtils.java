package cn.gavinluo.driver.utils.executor;


import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ThreadUtils
 *
 * @author gavinluo7@foxmail.com
 */
@Slf4j
public final class ThreadUtils {

    private static final int THREAD_MULTIPLER = 2;

    /**
     * Wait.
     *
     * @param object load object
     */
    public static void objectWait(Object object) {
        try {
            object.wait();
        } catch (InterruptedException ignore) {
            Thread.interrupted();
        }
    }

    /**
     * Sleep.
     *
     * @param millis sleep millisecond
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void countDown(CountDownLatch latch) {
        Objects.requireNonNull(latch, "latch");
        latch.countDown();
    }

    /**
     * Await count down latch.
     *
     * @param latch count down latch
     */
    public static void latchAwait(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Await count down latch with timeout.
     *
     * @param latch count down latch
     * @param time  timeout time
     * @param unit  time unit
     */
    public static void latchAwait(CountDownLatch latch, long time, TimeUnit unit) {
        try {
            latch.await(time, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Through the number of cores, calculate the appropriate number of threads; 1.5-2 times the number of CPU cores.
     *
     * @return thread count
     */
    public static int getSuitableThreadCount() {
        return getSuitableThreadCount(THREAD_MULTIPLER);
    }

    /**
     * Through the number of cores, calculate the appropriate number of threads.
     *
     * @param threadMultiple multiple time of cores
     * @return thread count
     */
    public static int getSuitableThreadCount(int threadMultiple) {
        final int coreCount = Runtime.getRuntime().availableProcessors();
        int workerCount = 1;
        while (workerCount < coreCount * threadMultiple) {
            workerCount <<= 1;
        }
        return workerCount;
    }

    public static void shutdownThreadPool(ExecutorService executor) {
        if (executor == null || executor.isShutdown()) {
            return;
        }
        log.info("shutdownThreadPool:{}", executor);
        executor.shutdown();
        int retry = 3;
        while (retry > 0) {
            retry--;
            try {
                if (executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.interrupted();
            } catch (Throwable ignored) {
            }
        }
        executor.shutdownNow();
    }

    /**
     * Add Shutdown Hook
     *
     * @param runnable runnable
     */
    public static void addShutdownHook(Runnable runnable) {
        Runtime.getRuntime().addShutdownHook(new Thread(runnable));
    }
}
