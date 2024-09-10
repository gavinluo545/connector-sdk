package cn.gavinluo.driver.connection;

import cn.gavinluo.driver.utils.executor.ExecutorFactory;
import cn.gavinluo.driver.utils.executor.NameThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

@Slf4j
public class AutoRecoveryManager {

    protected ExecutorService executorService;
    protected PriorityBlockingQueue<AutoRecoveryTask> autoRecoveryTasks;

    public static class InstanceHolder {
        public static final AutoRecoveryManager DEFAULT = new AutoRecoveryManager(Integer.parseInt(System.getProperty("autoRecoveryTasksQueueCapacity", "1024")));
    }

    private AutoRecoveryManager(int capacity) {
        this.executorService = ExecutorFactory.newSingleExecutorService(AutoRecoveryManager.class.getCanonicalName(), new NameThreadFactory(AutoRecoveryManager.class::getSimpleName));
        this.autoRecoveryTasks = new PriorityBlockingQueue<>(capacity, (o1, o2) -> (int) (o1.getNextRunTime() - o2.getNextRunTime()));
        startAutoRecovery();
    }

    private void startAutoRecovery() {
        log.info("启动采集器重连管理器");
        executorService.execute(() -> {
            for (; ; ) {
                AutoRecoveryTask jobItem;
                if (autoRecoveryTasks.isEmpty()) {
                    try {
                        jobItem = autoRecoveryTasks.take();
                        if (!autoRecoveryTasks.offer(jobItem)) {
                            log.error("offer autoRecoveryTask failed");
                        }
                    } catch (InterruptedException e) {
                        log.error("采集点位数据任务已中断");
                        break;
                    }
                } else {
                    jobItem = autoRecoveryTasks.peek();
                }

                try {
                    Thread.sleep(TimeUnit.NANOSECONDS.toMillis(Math.max(0, jobItem.getNextRunTime() - System.nanoTime())));
                } catch (InterruptedException e) {
                    log.error("采集器重连任务已中断");
                    break;
                }

                autoRecoveryTasks.forEach(e -> e.getGatherTagsDataFutures().forEach((k, v) -> {
                    if (v.isDone() || v.isCancelled() || v.isCompletedExceptionally()) {
                        e.getGatherTagsDataFutures().remove(k);
                    } else {
                        Integer recoveryTimeout = e.getConnector().connection.getRecoveryTimeout();
                        if ((k + TimeUnit.MILLISECONDS.toNanos(recoveryTimeout)) < System.nanoTime()) {
                            log.error("采集器重连任务超时 {} 毫秒", recoveryTimeout);
                            if (v.cancel(true)) {
                                e.getGatherTagsDataFutures().remove(k);
                            }
                        }
                    }
                }));

                List<AutoRecoveryTask> execList = new CopyOnWriteArrayList<>();
                long currentTime = System.nanoTime();

                while (!autoRecoveryTasks.isEmpty() && autoRecoveryTasks.peek().getNextRunTime() <= currentTime) {
                    execList.add(autoRecoveryTasks.poll());
                }

                if (!execList.isEmpty()) {
                    execList.forEach(e -> {
                        e.refreshNextRunTime();
                        Connector connector = e.getConnector();
                        e.exec(CompletableFuture.supplyAsync(() -> {
                            if (connector.state()) {
                                return false;
                            }
                            if (connector.connection.getMaximumRecoveryTime() > 0) {
                                int retryCount = e.getReconnectionCount().incrementAndGet();
                                long maxRetryCount = Math.max(TimeUnit.MINUTES.toNanos(connector.connection.getMaximumRecoveryTime()) / TimeUnit.SECONDS.toNanos(connector.connection.getRecoveryInterval()), 1);
                                if (retryCount > maxRetryCount) {
                                    log.error("自动恢复尝试 {} 次,停止自动恢复,恢复间隔 {} 秒,最长恢复尝试时长 {} 分", retryCount - 1, connector.connection.getRecoveryInterval(), connector.connection.getMaximumRecoveryTime());
                                    e.setShutdown(true);
                                    return false;
                                }
                            }
                            return true;
                        }).thenCompose((Function<Boolean, CompletableFuture<Connector>>) needRecovery -> {
                            if (needRecovery) {
                                return connector.doOnEstablishBefore().thenCompose(Connector::establish).whenComplete((connector2, throwable) -> {
                                    if (throwable == null) {
                                        connector.doOnEstablishSuccess();
                                    } else {
                                        connector.doOnEstablishError(throwable);
                                    }
                                });
                            }
                            return CompletableFuture.completedFuture(connector);
                        }));
                    });
                    execList.removeIf(AutoRecoveryTask::isShutdown);
                    if (!execList.isEmpty()) {
                        //下次执行
                        autoRecoveryTasks.addAll(execList);
                    }
                }
            }
        });
    }

    public void startAutoRecovery(Connector connector) {
        autoRecoveryTasks.add(new AutoRecoveryTask(connector, System.nanoTime()));
    }

    public void stopAutoRecovery(String connectionId) {
        log.info("停止采集器重连任务，连接:{}", connectionId);
        autoRecoveryTasks.stream().filter(e -> e.getConnector().getConnection().getConnectionId().equals(connectionId)).forEach(e -> e.getGatherTagsDataFutures().forEach((k, v) -> v.cancel(true)));
    }


}
