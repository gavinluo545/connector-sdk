package io.github.gavinluo545.connector.connection;

import io.github.gavinluo545.connector.connection.model.Connection;
import io.github.gavinluo545.connector.connection.model.Tag;
import io.github.gavinluo545.connector.connection.model.TagData;
import io.github.gavinluo545.connector.connection.model.TagWrite;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Slf4j
public abstract class Connector {
    protected final Connection connection;
    protected ConcurrentHashMap<String, Tag> tagMap;
    protected final AbstractTagsDataReporter tagsDataReporter;

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

    public CompletableFuture<Connector> doOnEstablishBefore() {
        return CompletableFuture.completedFuture(this);
    }

    public abstract CompletableFuture<Connector> establish();

    public void doOnEstablishSuccess() {
        startGatherTagsData();
    }

    public void doOnEstablishError(Throwable throwable) {
        log.error("连接建立失败: {}", connection, throwable);
    }

    public void autoRecovery() {
        if (connection.getAutoRecovery()) {
            return;
        }
        AutoRecoveryManager.InstanceHolder.DEFAULT.startAutoRecovery(this);
    }

    public CompletableFuture<Connector> doOnTerminateBefore() {
        return CompletableFuture.completedFuture(this);
    }

    public abstract CompletableFuture<Connector> terminate();

    public void doOnTerminateSuccess() {
        stopGatherTagsData();
    }

    public void doOnTerminateError(Throwable throwable) {
        log.error("连接销毁失败后执行的操作: {}", connection.getConnectionId(), throwable);
    }

    public abstract boolean state();

    public boolean tagReadResultsUseProactiveReporting() {
        return false;
    }

    public void tagReadUseProactiveReporting(List<Tag> tags) {
    }

    public static final CompletableFuture<List<TagData>> connectorClosed = new CompletableFuture<>();

    static {
        connectorClosed.completeExceptionally(new ConnectorClosedException());
    }

    public abstract CompletableFuture<List<TagData>> tagRead(List<Tag> tags);

    public abstract CompletableFuture<List<TagData>> tagWrite(List<TagWrite> tagWrites);

    public void startGatherTagsData() {
        GatherTagsDataManager.InstanceHolder.DEFAULT.startGatherTagsData(this);
    }

    public void stopGatherTagsData() {
        GatherTagsDataManager.InstanceHolder.DEFAULT.stopGatherTagsData(this.connection.getConnectionId());
    }
}
