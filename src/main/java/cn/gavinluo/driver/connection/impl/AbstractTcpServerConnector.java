package cn.gavinluo.driver.connection.impl;

import cn.gavinluo.driver.connection.AbstractTagsDataReporter;
import cn.gavinluo.driver.connection.model.Connection;
import cn.gavinluo.driver.connection.model.Tag;
import cn.gavinluo.driver.connection.model.TagData;
import cn.gavinluo.driver.connection.model.TagWrite;
import cn.gavinluo.driver.utils.tcp.TcpServerConfig;
import cn.gavinluo.driver.utils.tcp.impl.message.AbstractFrameMessage;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public abstract class AbstractTcpServerConnector<I extends AbstractFrameMessage, O extends AbstractFrameMessage> extends AbstractTcpConnector<I, O, TcpServerConfig<I, O>> {

    protected final Map<Serializable, ChannelWrap> deviceKeyToChannelMap = new ConcurrentHashMap<>();
    private TcpServerConfig<I, O> config = new TcpServerConfig<>(getIp(), getPort(), hasMessageId(),
            isParallelCollect(),
            (channelId, frameMessage) -> Objects.hash(channelId, frameMessage.getMessageId()));

    public AbstractTcpServerConnector(Connection connection, List<Tag> tags, AbstractTagsDataReporter tagsDataReporter) {
        super(connection, tags, tagsDataReporter);
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
        config.setChannelInActiveListener(channel -> {
            String channelId = channel.id().asShortText();
            Serializable deviceKey = deviceKeyToChannelMap.entrySet().stream().filter(entry -> entry.getValue().getChannel().id().asShortText().equals(channelId)).map(Map.Entry::getKey).findFirst().orElse(null);
            if (deviceKey != null) {
                log.info("设备离线 connectionId={} deviceKey={} channelId={}", connection.getConnectionId(), deviceKey, channelId);
                deviceKeyToChannelMap.remove(deviceKey);
            }
        });
        config.setExceptionCaughtListener((channel, throwable) -> log.error("收到未知格式报文，导致断开连接 connectionId={} channelId={} ", connection.getConnectionId(), channel.id().asShortText(), throwable));
    }

    public abstract void unknownMessageReceiveProcess(Serializable deviceKey, O message);

    @Override
    public void onEvent(Channel channel, O message) {
        try {
            String channelId = channel.id().asShortText();
            Serializable deviceKey = getDeviceKeyFormReceiveMessage(message);
            ChannelWrap oldChannelWrap = deviceKeyToChannelMap.get(deviceKey);
            if (oldChannelWrap != null) {
                Channel wrapChannel = oldChannelWrap.getChannel();
                if (!wrapChannel.isActive() || !wrapChannel.id().asShortText().equals(channelId)) {
                    //新的
                    deviceKeyToChannelMap.remove(deviceKey);
                    deviceKeyToChannelMap.put(deviceKey, new ChannelWrap(channel, deviceKey));
                    logDeviceUp(deviceKey, channelId);
                }
            } else {
                deviceKeyToChannelMap.put(deviceKey, new ChannelWrap(channel, deviceKey));
                logDeviceUp(deviceKey, channelId);
            }
            unknownMessageReceiveProcess(deviceKey, message);
        } catch (Exception ex) {
            log.error("未知消息接收处理错误 connectionId={} channelId={}", connection.getConnectionId(), channel.id().asShortText(), ex);
        }
    }

    public abstract Serializable getDeviceKeyFormReceiveMessage(O message);

    public Serializable getDeviceKeyByChannel(String channelId) {
        for (Map.Entry<Serializable, ChannelWrap> entry : deviceKeyToChannelMap.entrySet()) {
            ChannelWrap value = entry.getValue();
            if (value.getChannel() != null && value.getChannel().id().asShortText().equals(channelId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public abstract Map<ChannelWrap, List<I>> getChannelsForRead(List<Tag> tags);

    public abstract Map<ChannelWrap, List<I>> getChannelsForWrite(List<TagWrite> tagWrites);

    private void logDeviceUp(Serializable deviceKey, String channelId) {
        log.info("设备上线 connectionId={} deviceKey={} channelId={}", connection.getConnectionId(), deviceKey, channelId);
    }

    @Override
    public boolean tagReadResultsUseProactiveReporting() {
        return true;
    }

    @Override
    public boolean tagReadUseProactiveReporting(List<Tag> tags) {
        if (state()) {
            Map<ChannelWrap, List<I>> channels = getChannelsForRead(tags);
            channels.keySet().forEach(channelWrap -> {
                Channel channel = channelWrap.getChannel();
                Serializable deviceKey = channelWrap.getDeviceKey();
                List<I> lists = channels.get(channelWrap);
                lists.forEach(message -> {
                    if (!state() || channel == null || !channel.isActive()) {
                        deviceKeyToChannelMap.remove(deviceKey);
                        return;
                    }
                    try {
                        AtomicLong isDone = new AtomicLong(0);
                        tcp.sendRequestSyncCallback(channel, message, (o, throwable) -> {
                            try {
                                if (throwable != null) {
                                    log.error("读点异常 connectionId={} deviceKey={}", connection.getConnectionId(), deviceKey, throwable);
                                } else {
                                    unknownMessageReceiveProcess(deviceKey, o);
                                }
                            } finally {
                                isDone.set(System.currentTimeMillis());
                            }
                        });
                        if (!isParallelCollect()) {
                            while (isDone.get() == 0) {
                                Thread.sleep(100);
                            }
                            Thread.sleep(connection.getTimeWait());
                        }
                    } catch (Exception e) {
                        log.error("读点异常 connectionId={} deviceKey={}", connection.getConnectionId(), deviceKey, e);
                    }
                });
            });
        }
        return true;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public CompletableFuture<List<TagData>> tagRead(List<Tag> tags) {
        if (state()) {
            Map<ChannelWrap, List<I>> channels = getChannelsForRead(tags);
            CompletableFuture<List<TagData>>[] futures = (CompletableFuture<List<TagData>>[]) channels.keySet().stream().flatMap(channelWrap -> {
                Channel channel = channelWrap.getChannel();
                Serializable deviceKey = channelWrap.getDeviceKey();
                List<I> lists = channels.get(channelWrap);
                return lists.stream().map(message -> {
                    if (!state() || channel == null || !channel.isActive()) {
                        deviceKeyToChannelMap.remove(deviceKey);
                        return connectorClosed;
                    }
                    return tcp.sendSyncRequest(channel, message).thenApply(this::readResponseProcess);
                });
            }).toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures).thenApply(unused -> Arrays.stream(futures).flatMap(future -> future.join().stream()).collect(Collectors.toList()));
        }
        return connectorClosed;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public CompletableFuture<List<TagData>> tagWrite(List<TagWrite> tagWrites) {
        if (state()) {
            Map<ChannelWrap, List<I>> channels = getChannelsForWrite(tagWrites);
            CompletableFuture<List<TagData>>[] futures = (CompletableFuture<List<TagData>>[]) channels.keySet().stream().flatMap(channelWrap -> {
                Channel channel = channelWrap.getChannel();
                Serializable deviceKey = channelWrap.getDeviceKey();
                List<I> lists = channels.get(channelWrap);
                return lists.stream().map(message -> {
                    if (!state() || channel == null || !channel.isActive()) {
                        deviceKeyToChannelMap.remove(deviceKey);
                        return connectorClosed;
                    }
                    return tcp.sendSyncRequest(channel, message).thenApply(this::writeReponseProcess);
                });
            }).toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures).thenApply(unused -> Arrays.stream(futures).flatMap(future -> future.join().stream()).collect(Collectors.toList()));
        }
        return connectorClosed;
    }

    public ChannelWrap getChannel(Serializable deviceKey) {
        for (Map.Entry<Serializable, ChannelWrap> entry : deviceKeyToChannelMap.entrySet()) {
            if (Objects.equals(entry.getValue().getDeviceKey(), deviceKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Data
    @AllArgsConstructor
    public static class ChannelWrap {
        private Channel channel;
        private Serializable deviceKey;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChannelWrap that = (ChannelWrap) o;
            return Objects.equals(channel, that.channel) && Objects.equals(deviceKey, that.deviceKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(channel, deviceKey);
        }
    }
}
