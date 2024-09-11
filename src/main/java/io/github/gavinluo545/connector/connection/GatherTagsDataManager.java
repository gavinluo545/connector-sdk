package io.github.gavinluo545.connector.connection;

import io.github.gavinluo545.connector.connection.model.Tag;
import io.github.gavinluo545.connector.connection.model.TagData;
import io.github.gavinluo545.connector.utils.executor.ExecutorFactory;
import io.github.gavinluo545.connector.utils.executor.NameThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@SuppressWarnings({"unused"})
@Slf4j
public class GatherTagsDataManager {

    protected ExecutorService executorService;
    protected PriorityBlockingQueue<GatherTagsDataTask> gatherTagsDataTasks;
    protected ExecutorService proactiveReportingExecutorService;

    public static class InstanceHolder {
        public static final GatherTagsDataManager DEFAULT = new GatherTagsDataManager();
    }

    private GatherTagsDataManager() {
        this(Integer.parseInt(System.getProperty("gatherTagsDataTasksQueueCapacity", "1024")),
                Integer.parseInt(System.getProperty("proactiveReportingCoreThreads", String.valueOf(Runtime.getRuntime().availableProcessors()))),
                Integer.parseInt(System.getProperty("proactiveReportingMaxThreads", String.valueOf(Runtime.getRuntime().availableProcessors() * 2))),
                Integer.parseInt(System.getProperty("proactiveReportingQueueCapacity", "1024")),
                Integer.parseInt(System.getProperty("proactiveReportingKeepAliveTime", "200")));
    }

    private GatherTagsDataManager(int capacity, int proactiveReportingCoreThreads, int proactiveReportingMaxThreads,
                                  int proactiveReportingQueueCapacity, int proactiveReportingKeepAliveTime) {
        this.executorService = ExecutorFactory.newSingleExecutorService(GatherTagsDataManager.class.getCanonicalName(), new NameThreadFactory(GatherTagsDataManager.class::getSimpleName));
        this.proactiveReportingExecutorService = ExecutorFactory.newCustomerThreadExecutor(GatherTagsDataManager.class.getCanonicalName(),
                proactiveReportingCoreThreads, proactiveReportingMaxThreads, proactiveReportingKeepAliveTime, new ArrayBlockingQueue<>(proactiveReportingQueueCapacity),
                new NameThreadFactory(() -> "TagReadUseProactiveReporting"),
                (r, executor) -> log.error("采集任务数太多，拒绝本次采集任务"));
        this.gatherTagsDataTasks = new PriorityBlockingQueue<>(capacity, (o1, o2) -> (int) (o1.getNextRunTime() - o2.getNextRunTime()));
        startGatherTagsData();
    }

    private void startGatherTagsData() {
        log.info("启动采集管理器");
        executorService.execute(() -> {
            for (; ; ) {
                GatherTagsDataTask jobItem;
                if (gatherTagsDataTasks.isEmpty()) {
                    try {
                        jobItem = gatherTagsDataTasks.take();
                        if (!gatherTagsDataTasks.offer(jobItem)) {
                            log.error("offer gatherTagsDataTask failed");
                        }
                    } catch (InterruptedException e) {
                        log.error("采集点位数据任务已中断");
                        break;
                    }
                } else {
                    jobItem = gatherTagsDataTasks.peek();
                }

                //执行下批之前先把之前的超时
                gatherTagsDataTasks.forEach(e ->
                        e.getGatherTagsDataFutures().forEach((k, v) -> {
                            if (v.isDone() || v.isCancelled() || v.isCompletedExceptionally()) {
                                e.getGatherTagsDataFutures().remove(k);
                            } else {
                                Integer collectionTimeout = e.connector.connection.getCollectionTimeout();
                                if ((k + TimeUnit.MILLISECONDS.toNanos(collectionTimeout)) < System.nanoTime()) {
                                    log.error("采集点位数据任务超时 {} 毫秒", collectionTimeout);
                                    if (v.cancel(true)) {
                                        e.getGatherTagsDataFutures().remove(k);
                                    }
                                }
                            }
                        }));

                List<GatherTagsDataTask> execList = new CopyOnWriteArrayList<>();
                long currentTime = System.nanoTime();

                while (!gatherTagsDataTasks.isEmpty() && gatherTagsDataTasks.peek().getNextRunTime() <= currentTime) {
                    execList.add(gatherTagsDataTasks.poll());
                }

                //执行之前先休眠
                try {
                    GatherTagsDataTask gatherTagsDataTask = execList.isEmpty() ? gatherTagsDataTasks.peek() : execList.get(0);
                    gatherTagsDataTask = gatherTagsDataTask == null ? jobItem : gatherTagsDataTask;
                    long millis = TimeUnit.NANOSECONDS.toMillis(Math.max(0, gatherTagsDataTask.getNextRunTime() - System.nanoTime()));
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    log.error("采集点位数据任务已中断");
                    break;
                }

                if (!execList.isEmpty()) {
                    execList.forEach(e -> {
                        e.refreshNextRunTime();
                        Connector connector = e.connector;
                        if (connector.tagReadResultsUseProactiveReporting()) {
                            e.exec(CompletableFuture.runAsync(() -> {
                                try {
                                    connector.tagReadUseProactiveReporting(e.tags());
                                } catch (Exception ex) {
                                    log.error("采集点位数据任务失败", ex);
                                }
                            }, proactiveReportingExecutorService));
                        } else {
                            e.exec(connector.tagRead(e.tags())
                                    .thenAccept(tagsData -> {
                                        List<TagData> list = tagsData.stream()
                                                .filter(tagData -> tagData.getQ() == StatusCode.OK || tagData.getV() == null)
                                                .collect(Collectors.toList());
                                        connector.tagsDataReporter.report(list);
                                    })
                                    .exceptionally(throwable -> {
                                        log.error("采集点位数据任务失败", throwable);
                                        return null;
                                    })
                            );
                        }
                    });
                    execList.removeIf(e -> e.isShutdown);
                    if (!execList.isEmpty()) {
                        gatherTagsDataTasks.addAll(execList);
                    }
                }
            }
        });
    }

    public void startGatherTagsData(Connector connector) {
        connector.tagMap.values().stream().filter(e -> e.getCollectionPeriod() != null && e.getCollectionType() != 1).filter(tag -> tag.getRwType() != 1).collect(tagMapCollector(connector))
                .forEach(((collectPeriod, tagList) -> gatherTagsDataTasks.add(new GatherTagsDataTask(connector, System.nanoTime(), collectPeriod))));
    }

    public void stopGatherTagsData(String connectionId) {
        log.info("停止采集点位数据任务，连接:{}", connectionId);
        gatherTagsDataTasks.stream().filter(e -> e.connector.getConnection().getConnectionId().equals(connectionId))
                .forEach(e -> {
                    e.isShutdown = true;
                    e.getGatherTagsDataFutures().forEach((k, v) -> v.cancel(true));
                });
    }


    public void dynamicallyAddGatherTags(Connector connector, List<Tag> tags) {
        tags.stream().map(Tag::getTagId).forEach(connector.tagMap::remove);
        tags.forEach(tag -> connector.tagMap.put(tag.getTagId(), tag));
        tags.stream().filter(e -> e.getCollectionPeriod() != null && e.getCollectionType() != 1).filter(tag -> tag.getRwType() != 1).collect(tagMapCollector(connector))
                .forEach(((collectPeriod, tagList) -> {
                    if (gatherTagsDataTasks.stream().noneMatch(gatherTagsDataTask -> gatherTagsDataTask.collectPeriod == collectPeriod)) {
                        gatherTagsDataTasks.add(new GatherTagsDataTask(connector, System.nanoTime(), collectPeriod));
                    }
                }));

    }

    public void dynamicallyRemoveGatherTags(Connector connector, List<String> tagIds) {
        tagIds.forEach(connector.tagMap::remove);
        List<Integer> removeTasks = gatherTagsDataTasks.stream().filter(e -> e.tags().isEmpty()).map(e -> {
            e.isShutdown = true;
            return e.collectPeriod;
        }).collect(Collectors.toList());
        removeTasks.forEach(collectPeriod -> gatherTagsDataTasks.removeIf(gatherTagsDataTask -> gatherTagsDataTask.collectPeriod == collectPeriod));
    }

    public static Collector<Tag, ?, Map<Integer, List<Tag>>> tagMapCollector(Connector connector) {
        Collector<Tag, ?, Map<Integer, List<Tag>>> tagMapCollector;
        //并行采集按周期
        tagMapCollector = Collectors.groupingBy(Tag::getCollectionPeriod);
        return tagMapCollector;
    }

    @Getter
    public static class GatherTagsDataTask {
        /**
         * 采集器
         */
        private final Connector connector;
        /**
         * 采集间隔
         */
        private final int collectPeriod;
        /**
         * 下次执行任务的时间
         */
        private long nextRunTime;
        private volatile boolean isShutdown;
        /**
         * 采集任务对应的未来任务列表
         */
        protected Map<Long, CompletableFuture<?>> gatherTagsDataFutures;

        /**
         * 构造方法，初始化采集任务。
         *
         * @param connector     采集器
         * @param nextRunTime   下次执行任务的时间
         * @param collectPeriod 采集周期
         */
        public GatherTagsDataTask(Connector connector, long nextRunTime, int collectPeriod) {
            this.connector = connector;
            this.nextRunTime = nextRunTime;
            this.collectPeriod = collectPeriod;
            this.gatherTagsDataFutures = new ConcurrentHashMap<>();
        }

        /**
         * 执行采集任务。
         *
         * @param future 采集任务的未来任务
         */
        public void exec(CompletableFuture<?> future) {
            gatherTagsDataFutures.put(System.nanoTime(), future);
        }

        /**
         * 刷新下次执行任务的时间。
         */
        public void refreshNextRunTime() {
            nextRunTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(collectPeriod);
        }

        public List<Tag> tags() {
            //从原始获得 因为可能发生变化
            return connector.tagMap.values().stream().filter(e -> e.getCollectionPeriod() != null && e.getCollectionType() != 1).filter(tag -> tag.getRwType() != 1)
                    .collect(tagMapCollector(connector)).get(collectPeriod);
        }

    }
}
