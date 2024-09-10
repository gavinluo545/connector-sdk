package io.github.gavinluo545.connector.utils.tcp.impl;

import io.github.gavinluo545.connector.utils.tcp.Tcp;
import io.github.gavinluo545.connector.utils.tcp.TcpServer;
import io.github.gavinluo545.connector.utils.tcp.TcpServerConfig;
import io.github.gavinluo545.connector.utils.tcp.impl.message.AbstractFrameMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AbstractTcpServer.java
 *
 * @author gavinluo545@gmail.com
 */
@Slf4j
public abstract class AbstractTcpServer<I extends AbstractFrameMessage, O extends AbstractFrameMessage> extends AbstractTcp<I, O, TcpServerConfig<I, O>> implements TcpServer<I, O> {

    public AbstractTcpServer(TcpServerConfig<I, O> config) {
        super(config);
    }

    @Override
    public CompletableFuture<Tcp<I, O, TcpServerConfig<I, O>>> bootstrap() {
        CompletableFuture<Tcp<I, O, TcpServerConfig<I, O>>> future = super.bootstrap();
        if (!unFinished(future)) {
            return future;
        }
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false))
                    .option(ChannelOption.SO_BACKLOG, config.getSoBacklogSize())
                    .option(ChannelOption.SO_RCVBUF, config.getSoRcvbufSize())
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.config().setRecvByteBufAllocator(new AdaptiveRecvByteBufAllocator());
                            AbstractTcpServer.this.initChannel(ch.pipeline());
                            ch.pipeline().addLast(new InboundHandler(AbstractTcpServer.this));
                        }
                    })
                    .childOption(ChannelOption.TCP_NODELAY, config.isTcpNodelay())
                    .childOption(ChannelOption.SO_KEEPALIVE, config.isSoKeepalive());

            bootstrap.bind(config.getPort()).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    channel = f.channel();
                    log.info("Tcp server start success, port:{}", config.getPort());
                    future.complete(AbstractTcpServer.this);
                } else {
                    log.info("Tcp server start failed port:{}", config.getPort(), f.cause());
                    future.completeExceptionally(f.cause());
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        } finally {
            stateUpdate(ST_STARTED);
        }
        return future;
    }

    @Override
    public List<Channel> getClientChannels() {
        if (channel == null) {
            return new CopyOnWriteArrayList<>();
        }
        List<Channel> channels = new CopyOnWriteArrayList<>();
        for (Map.Entry<String, Channel> entry : clientChannles.entrySet()) {
            if (entry.getKey().startsWith(getChannel().id().asShortText())) {
                channels.add(entry.getValue());
            }
        }
        return channels;
    }
}
