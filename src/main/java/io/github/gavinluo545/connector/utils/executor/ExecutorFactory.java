package io.github.gavinluo545.connector.utils.executor;

import java.util.concurrent.*;


/**
 * ExecutorFactory
 *
 * @author gavinluo545@gmail.com
 */
public final class ExecutorFactory {
    public static final String DEFAULT_NAMESPACE = ExecutorFactory.class.getSimpleName();
    private static final ThreadPoolManager THREAD_POOL_MANAGER = ThreadPoolManager.getInstance();

    /**
     * Create a new single executor service with input thread factory and register to manager.
     *
     * @param group         group name
     * @param threadFactory thread factory
     * @return new single executor service
     */
    public static ExecutorService newSingleExecutorService(final String group, final ThreadFactory threadFactory) {
        ExecutorService executorService = Executors.newFixedThreadPool(1, threadFactory);
        THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
        return executorService;
    }

    /**
     * Create a new fixed executor service with input thread factory and register to manager.
     *
     * @param group         group name
     * @param nThreads      thread number
     * @param threadFactory thread factory
     * @return new fixed executor service
     */
    public static ExecutorService newFixedExecutorService(final String group, final int nThreads, final ThreadFactory threadFactory) {
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads, threadFactory);
        THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
        return executorService;
    }

    /**
     * Create a new single scheduled executor service with input thread factory and register to manager.
     *
     * @param group         group name
     * @param threadFactory thread factory
     * @return new single scheduled executor service
     */
    public static ScheduledExecutorService newSingleScheduledExecutorService(final String group, final ThreadFactory threadFactory) {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, threadFactory);
        THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
        return executorService;
    }

    /**
     * Create a new scheduled executor service with input thread factory and register to manager.
     *
     * @param group         group name
     * @param nThreads      thread number
     * @param threadFactory thread factory
     * @return new scheduled executor service
     */
    public static ScheduledExecutorService newScheduledExecutorService(final String group, final int nThreads, final ThreadFactory threadFactory, final RejectedExecutionHandler rejectedExecutionHandler) {
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(nThreads, threadFactory, rejectedExecutionHandler);
        THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
        return executorService;
    }

    public static ThreadPoolExecutor newCustomerThreadExecutor(final String group, final int coreThreads, final int maxThreads, final long keepAliveTimeMs, BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThreads, maxThreads, keepAliveTimeMs, TimeUnit.MILLISECONDS, workQueue, threadFactory == null ? Thread::new : threadFactory, handler == null ? new ThreadPoolExecutor.AbortPolicy() : handler);
        THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executor);
        return executor;
    }
}
