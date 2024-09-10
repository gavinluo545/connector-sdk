package cn.gavinluo.driver.connection;

import cn.gavinluo.driver.connection.model.Connection;
import cn.gavinluo.driver.connection.model.Tag;
import cn.gavinluo.driver.connection.model.TagData;
import cn.gavinluo.driver.connection.model.TagWrite;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 表示连接器的抽象类，提供连接和数据采集的基本功能。
 * 具体的连接器类应该继承这个抽象类并实现相关方法以适应具体设备。
 *
 * @author gavinluo7@foxmail.com
 */
@Getter
@Slf4j
public abstract class Connector {
    /**
     * 设备连接信息
     */
    protected final Connection connection;
    protected ConcurrentHashMap<String, Tag> tagMap;
    protected final AbstractTagsDataReporter tagsDataReporter;

    /**
     * 构造方法，初始化连接器。
     *
     * @param connection 设备连接信息
     * @param tags       点位信息列表
     */
    public Connector(Connection connection, List<Tag> tags, AbstractTagsDataReporter tagsDataReporter) {
        this.connection = connection;
        tags.forEach(tag -> tag.setConnectionId(connection.getConnectionId()));
        tagMap = tags.stream().collect(Collectors.toMap(Tag::getTagId, Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        this.tagsDataReporter = tagsDataReporter;
    }

    public Connector start() throws Exception {
        doOnEstablishBefore()
                .thenCompose(Connector::establish)
                .whenComplete((c, throwable) -> {
                    try {
                        if (throwable == null) {
                            doOnEstablishSuccess();
                        } else {
                            doOnEstablishError(throwable);
                        }
                    } catch (Exception e) {
                        log.error("连接错误: {}", connection, e);
                    } finally {
                        try {
                            autoRecovery();
                        } catch (Exception e) {
                            log.error("调用自动恢复错误: {}", connection, e);
                        }
                    }
                }).get();
        return this;
    }

    /**
     * 销毁连接器，释放资源。
     */
    public void stop() throws Exception {
        Consumer<?> consumer = (Consumer<Object>) o -> {
            log.info("销毁连接器: {}", connection);
            AutoRecoveryManager.InstanceHolder.DEFAULT.stopAutoRecovery(connection.getConnectionId());
        };

        if (state()) {
            doOnTerminateBefore()
                    .thenCompose(Connector::terminate)
                    .whenComplete((c, throwable) -> {
                        if (throwable != null) {
                            doOnTerminateError(throwable);
                        } else {
                            doOnTerminateSuccess();
                            consumer.accept(null);
                        }
                    }).get();
        } else {
            consumer.accept(null);
        }
    }

    /**
     * 连接建立前执行的操作。
     *
     * @return 本连接器实例
     */
    public CompletableFuture<Connector> doOnEstablishBefore() {
        return CompletableFuture.completedFuture(this);
    }

    /**
     * 建立连接的抽象方法，由具体的连接器实现。
     *
     * @return 建立连接后的本连接器实例
     */
    public abstract CompletableFuture<Connector> establish();

    /**
     * 连接建立成功后执行的操作。
     */
    public void doOnEstablishSuccess() {
        startGatherTagsData();
    }

    /**
     * 连接建立失败后执行的操作。
     *
     * @param throwable 连接失败的异常
     */
    public void doOnEstablishError(Throwable throwable) {
        log.error("连接建立失败: {}", connection, throwable);
    }

    /**
     * 自动重连任务。
     */
    public void autoRecovery() {
        if (connection.getAutoRecovery()) {
            return;
        }
        AutoRecoveryManager.InstanceHolder.DEFAULT.startAutoRecovery(this);
    }

    /**
     * 连接销毁前执行的操作。
     *
     * @return 本连接器实例
     */
    public CompletableFuture<Connector> doOnTerminateBefore() {
        return CompletableFuture.completedFuture(this);
    }

    /**
     * 断开连接的抽象方法，由具体的连接器实现。
     *
     * @return 断开连接后的本连接器实例
     */
    public abstract CompletableFuture<Connector> terminate();

    /**
     * 连接销毁成功后执行的操作。
     */
    public void doOnTerminateSuccess() {
        stopGatherTagsData();
    }

    /**
     * 连接销毁失败后执行的操作。
     *
     * @param throwable 连接销毁失败的异常
     */
    public void doOnTerminateError(Throwable throwable) {
        log.error("连接销毁失败后执行的操作: {}", connection.getConnectionId(), throwable);
    }

    /**
     * 获取连接器的连接状态。
     *
     * @return true表示连接正常，false表示连接断开
     */
    public abstract boolean state();

    /**
     * 点位读结果通过主动上报响应
     *
     * @return 默认true
     */
    public boolean tagReadResultsUseProactiveReporting() {
        return false;
    }

    /**
     * 读取点位信息，点位读结果通过主动上报响应。
     *
     * @param tags 待读取的点位信息列表
     */
    public boolean tagReadUseProactiveReporting(List<Tag> tags) {
        return true;
    }

    public static final CompletableFuture<List<TagData>> connectorClosed = new CompletableFuture<>();

    static {
        connectorClosed.completeExceptionally(new ConnectorClosedException());
    }

    /**
     * 读取点位信息，返回模拟的点位读取响应。
     *
     * @param tags 待读取的点位信息列表
     * @return 模拟的点位读取响应
     */
    public abstract CompletableFuture<List<TagData>> tagRead(List<Tag> tags);

    /**
     * 写入点位信息，返回模拟的点位写入响应。
     *
     * @param tagWrites 待写入的点位信息列表
     * @return 模拟的点位写入响应
     */
    public abstract CompletableFuture<List<TagData>> tagWrite(List<TagWrite> tagWrites);

    /**
     * 是否并行采集
     *
     * @return 默认并行采集，对于半双工设备应该为false
     */
    public boolean isParallelCollect() {
        return true;
    }

    /**
     * 启动采集点位数据。
     */
    public void startGatherTagsData() {
        GatherTagsDataManager.InstanceHolder.DEFAULT.startGatherTagsData(this);
    }

    /**
     * 停止采集点位数据任务。
     */
    public void stopGatherTagsData() {
        GatherTagsDataManager.InstanceHolder.DEFAULT.stopGatherTagsData(this.connection.getConnectionId());
    }
}
