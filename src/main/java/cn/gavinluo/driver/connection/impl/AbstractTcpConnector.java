package cn.gavinluo.driver.connection.impl;

import cn.gavinluo.driver.connection.AbstractTagsDataReporter;
import cn.gavinluo.driver.connection.Connector;
import cn.gavinluo.driver.connection.model.Connection;
import cn.gavinluo.driver.connection.model.Tag;
import cn.gavinluo.driver.connection.model.TagData;
import cn.gavinluo.driver.utils.tcp.AbstractTcpConfig;
import cn.gavinluo.driver.utils.tcp.impl.AbstractTcp;
import cn.gavinluo.driver.utils.tcp.impl.message.AbstractFrameMessage;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractTcpConnector<I extends AbstractFrameMessage, O extends AbstractFrameMessage, Config extends AbstractTcpConfig<I, O>> extends Connector {

    protected AbstractTcp<I, O, Config> tcp;

    public AbstractTcpConnector(Connection connection, List<Tag> tags, AbstractTagsDataReporter tagsDataReporter) {
        super(connection, tags, tagsDataReporter);
    }

    public abstract void onEvent(Channel channel, O message);

    public byte sendRequestTimeoutSeconds() {
        return 3;
    }

    public byte waitResponseTimeoutSeconds() {
        return 5;
    }

    public abstract String getIp();

    public abstract int getPort();

    public abstract boolean hasMessageId();

    public abstract List<TagData> readResponseProcess(O responseMessage);

    public abstract List<TagData> writeReponseProcess(O responseMessage);

    public abstract HeartbeatConfig<I> getHeartbeatConfig();

    public abstract AbstractTcp<I, O, Config> newTcp(Config config);

    public abstract Config getConfig();

    @Override
    public CompletableFuture<Connector> establish() {
        CompletableFuture<Connector> future = new CompletableFuture<>();
        terminate().thenAccept(connector -> {
            tcp = newTcp(getConfig());
            try {
                tcp.bootstrap().whenComplete((tcpServer, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {
                        future.complete(AbstractTcpConnector.this);
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Connector> terminate() {
        CompletableFuture<Connector> future = new CompletableFuture<>();
        if (tcp != null) {
            tcp.shutdown().whenComplete((a, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                } else {
                    tcp = null;
                    future.complete(AbstractTcpConnector.this);
                }
            });
        } else {
            future.complete(this);
        }
        return future;
    }

    @Override
    public boolean state() {
        return tcp != null && tcp.isConnected();
    }


}
