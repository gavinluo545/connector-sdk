package io.github.gavinluo545.connector.connection.impl;

import io.github.gavinluo545.connector.connection.AbstractTagsDataReporter;
import io.github.gavinluo545.connector.connection.model.Connection;
import io.github.gavinluo545.connector.connection.model.Tag;
import io.github.gavinluo545.connector.connection.model.TagData;
import io.github.gavinluo545.connector.connection.model.TagWrite;
import io.github.gavinluo545.connector.utils.tcp.TcpClientConfig;
import io.github.gavinluo545.connector.utils.tcp.impl.message.AbstractFrameMessage;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public abstract class AbstractTcpClientConnector<I extends AbstractFrameMessage, O extends AbstractFrameMessage> extends AbstractTcpConnector<I, O, TcpClientConfig<I, O>> {
    private TcpClientConfig<I, O> config = new TcpClientConfig<>(getIp(), getPort(),
            (channelId, frameMessage) -> Objects.hash(channelId, frameMessage.getMessageId()));

    public AbstractTcpClientConnector(Connection connection, List<Tag> tags, AbstractTagsDataReporter tagsDataReporter) {
        super(connection, tags, tagsDataReporter);
        config.setAutoReconnect(connection.getAutoRecovery());
        config.setAutoReconnectIntervalSeconds(connection.getRecoveryInterval());
        config.setAutoReconnectMaxMinutes(connection.getMaximumRecoveryTime());
        config.setSendRequestTimeoutSenconds(sendRequestTimeoutSeconds());
        config.setWaitResponseTimeoutSenconds(waitResponseTimeoutSeconds());
        HeartbeatConfig<I> heartbeatConfig = getHeartbeatConfig();
        if (heartbeatConfig == null) {
            config.setEnableHeartbeat(false);
        } else {
            config.setEnableHeartbeat(heartbeatConfig.isEnableHeartbeat());
            config.setHeartbeatIntervalSeconds(heartbeatConfig.getHeartbeatIntervalSeconds());
            config.setHeartbeatMessageFunc(heartbeatConfig.getHeartbeatMessageFunc());
        }
        config.setUnknownMessageListener(this::onEvent);
        config.setChannelActiveListener(channel -> log.info("设备上线 connectionId={} channelId={}", connection.getConnectionId(), channel.id().asShortText()));
        config.setChannelInActiveListener(channel -> log.info("设备离线 connectionId={} channelId={}", connection.getConnectionId(), channel.id().asShortText()));
        config.setExceptionCaughtListener((channel, throwable) -> log.error("收到未知格式报文，导致断开连接 connectionId={} channelId={}", connection.getConnectionId(), channel.id().asShortText(), throwable));

    }

    @Override
    public void onEvent(Channel channel, O message) {
        try {
            unknownMessageReceiveProcess(message);
        } catch (Exception ex) {
            log.error("未知消息接收处理错误 connectionId={} channelId={}", connection.getConnectionId(), channel.id().asShortText(), ex);
        }
    }

    public abstract void unknownMessageReceiveProcess(O message);

    public abstract List<I> getChannelsForRead(List<Tag> tags);

    public abstract List<I> getChannelsForWrite(List<TagWrite> tagWrites);

    @Override
    public boolean tagReadResultsUseProactiveReporting() {
        return true;
    }

    @Override
    public void tagReadUseProactiveReporting(List<Tag> tags) {
        if (state()) {
            List<I> lists = getChannelsForRead(tags);
            lists.forEach(message -> {
                if (!state()) {
                    return;
                }

                try {
                    tcp.sendRequestSyncCallback(tcp.getChannel(), message, (o, throwable) -> {
                        if (throwable != null) {
                            log.error("读点异常 connectionId={}", connection.getConnectionId(), throwable);
                        } else {
                            unknownMessageReceiveProcess(o);
                        }
                    });
                } catch (Exception e) {
                    log.error("读点异常 connectionId={}", connection.getConnectionId(), e);
                }
            });
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public CompletableFuture<List<TagData>> tagRead(List<Tag> tags) {
        if (state()) {
            List<I> lists = getChannelsForRead(tags);
            CompletableFuture<List<TagData>>[] futures = lists.stream().map(message -> {
                if (!state()) {
                    return connectorClosed;
                }

                return tcp.sendSyncRequest(tcp.getChannel(), message).thenApply(this::readResponseProcess);
            }).toArray(CompletableFuture[]::new);

            return CompletableFuture.allOf(futures).thenApply(unused -> Arrays.stream(futures).flatMap(future -> future.join().stream()).collect(Collectors.toList()));
        }
        return connectorClosed;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public CompletableFuture<List<TagData>> tagWrite(List<TagWrite> tagWrites) {
        if (state()) {
            List<I> lists = getChannelsForWrite(tagWrites);
            CompletableFuture<List<TagData>>[] futures = lists.stream().map(message -> {
                if (!state()) {
                    return connectorClosed;
                }

                return tcp.sendSyncRequest(tcp.getChannel(), message).thenApply(this::writeReponseProcess);
            }).toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures).thenApply(unused -> Arrays.stream(futures).flatMap(future -> future.join().stream()).collect(Collectors.toList()));
        }
        return connectorClosed;
    }

    @Override
    public void autoRecovery() {
        //使用tcp内部的重连
    }

}
