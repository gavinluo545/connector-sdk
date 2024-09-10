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
    /**
     * 采集器
     */
    private final Connector connector;
    /**
     * 下次执行任务的时间
     */
    private long nextRunTime;
    @Setter
    private volatile boolean shutdown;
    /**
     * 未来任务列表
     */
    protected Map<Long, CompletableFuture<?>> gatherTagsDataFutures;
    private final AtomicInteger reconnectionCount = new AtomicInteger(0);

    /**
     * 构造方法，初始化任务。
     *
     * @param connector   采集器
     * @param nextRunTime 下次执行任务的时间
     */
    public AutoRecoveryTask(Connector connector, long nextRunTime) {
        this.connector = connector;
        this.nextRunTime = nextRunTime;
        this.gatherTagsDataFutures = new ConcurrentHashMap<>();
    }

    /**
     * 执行任务。
     *
     * @param future 未来任务
     */
    public void exec(CompletableFuture<?> future) {
        gatherTagsDataFutures.put(System.nanoTime(), future);
    }

    /**
     * 刷新下次执行任务的时间。
     */
    public void refreshNextRunTime() {
        nextRunTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(connector.connection.getRecoveryInterval());
    }

}
