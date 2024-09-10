package io.github.gavinluo545.connector.utils.executor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class NameThreadFactory implements ThreadFactory {

    private final AtomicInteger id = new AtomicInteger(0);

    private static final String DEFAULT_NAME = NameThreadFactory.class.getName();

    final Supplier<String> getThreadName;

    public NameThreadFactory(Supplier<String> getThreadName) {
        this.getThreadName = getThreadName;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = getThreadName.get();
        if (null == threadName || threadName.isEmpty()) {
            threadName = DEFAULT_NAME + "." + id.getAndIncrement();
        } else {
            threadName = !threadName.endsWith(".") ? threadName + "." + id.getAndIncrement() : threadName + id.getAndIncrement();
        }
        Thread thread = new Thread(r, threadName);
        thread.setDaemon(true);
        return thread;
    }

}
