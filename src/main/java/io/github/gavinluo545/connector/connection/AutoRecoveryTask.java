package io.github.gavinluo545.connector.connection;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class AutoRecoveryTask {
    private final Connector connector;
    private long nextRunTime;
    @Setter
    private volatile boolean shutdown;
    protected Map<Long, CompletableFuture<?>> gatherTagsDataFutures;
    private final AtomicInteger reconnectionCount = new AtomicInteger(0);

    public AutoRecoveryTask(Connector connector, long nextRunTime) {
        this.connector = connector;
        this.nextRunTime = nextRunTime;
        this.gatherTagsDataFutures = new ConcurrentHashMap<>();
    }

    public void exec(CompletableFuture<?> future) {
        gatherTagsDataFutures.put(System.nanoTime(), future);
    }

    public void refreshNextRunTime() {
        nextRunTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(connector.connection.getRecoveryInterval());
    }

}
