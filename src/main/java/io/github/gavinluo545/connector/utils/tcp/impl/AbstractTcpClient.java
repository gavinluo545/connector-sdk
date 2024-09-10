package io.github.gavinluo545.connector.utils.tcp.impl;

import io.github.gavinluo545.connector.utils.executor.ExecutorFactory;
import io.github.gavinluo545.connector.utils.executor.NameThreadFactory;
import io.github.gavinluo545.connector.utils.executor.ThreadUtils;
import io.github.gavinluo545.connector.utils.tcp.Tcp;
import io.github.gavinluo545.connector.utils.tcp.TcpClient;
import io.github.gavinluo545.connector.utils.tcp.TcpClientConfig;
import io.github.gavinluo545.connector.utils.tcp.impl.message.AbstractFrameMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gavinluo545@gmail.com
 */
@Slf4j
public abstract class AbstractTcpClient<I extends AbstractFrameMessage, O extends AbstractFrameMessage> extends AbstractTcp<I, O, TcpClientConfig<I, O>> implements TcpClient<I, O> {
    protected ScheduledExecutorService autoRecoveryExecutor;

    public AbstractTcpClient(TcpClientConfig<I, O> config) {
        super(config);
        if (config.isAutoReconnect()) {
            autoRecoveryExecutor = ExecutorFactory.newSingleScheduledExecutorService(AbstractTcpClient.class.getCanonicalName(), new NameThreadFactory(() -> String.format("TcpReconnect_%d-%s:%d", System.currentTimeMillis(), config.getIp(), config.getPort())));
        }
    }

    @Override
    public CompletableFuture<Tcp<I, O, TcpClientConfig<I, O>>> bootstrap() {
        CompletableFuture<Tcp<I, O, TcpClientConfig<I, O>>> future = super.bootstrap();
        if (!unFinished(future)) {
            return future;
        }
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMillis())
                    .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false))
                    .option(ChannelOption.SO_SNDBUF, config.getSoSndbufSize())
                    .option(ChannelOption.SO_RCVBUF, config.getSoRcvbufSize())
                    .option(ChannelOption.SO_KEEPALIVE, config.isSoKeepalive())
                    .option(ChannelOption.TCP_NODELAY, config.isTcpNodelay())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.config().setRecvByteBufAllocator(new AdaptiveRecvByteBufAllocator());
                            AbstractTcpClient.this.initChannel(ch.pipeline());
                            ch.pipeline().addLast(new InboundHandler(AbstractTcpClient.this));
                        }
                    })
                    .connect(config.getIp(), config.getPort())
                    .addListener((ChannelFuture f) -> {
                        if (f.isSuccess()) {
                            channel = f.channel();
                            log.info("tcp客户端启动成功 channelId={} {}:{}", channel.id().asShortText(), config.getIp(), config.getPort());
                            future.complete(AbstractTcpClient.this);
                        } else {
                            log.error("tcp客户端启动失败 {}:{}", config.getIp(), config.getPort(), f.cause());
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
    public void channelInactive(ChannelHandlerContext ctx) {
        super.channelInactive(ctx);
        if (config.isAutoReconnect()) {
            long recoveryInterval = Math.min(TimeUnit.SECONDS.toMillis(config.getAutoReconnectIntervalSeconds()), 1);
            long maximumRecoveryTime = Math.min(TimeUnit.MINUTES.toMillis(config.getAutoReconnectMaxMinutes()), 0);
            long maxRetryCount = Math.max(maximumRecoveryTime / recoveryInterval, 1);
            autoRecoveryExecutor.execute(() -> {
                AtomicInteger reconnectionCount = new AtomicInteger(0);
                for (; ; ) {
                    if (isConnected()) {
                        break;
                    }
                    if (maximumRecoveryTime != 0) {
                        int retryCount = reconnectionCount.incrementAndGet();
                        if (retryCount > maxRetryCount) {
                            log.error("tcp客户端重连 {} 次数超过最大限制,将停止自动恢复,恢复间隔 {} 毫秒,最长尝试恢复时长 {} 毫秒", retryCount - 1, recoveryInterval, maximumRecoveryTime);
                            break;
                        }
                    }
                    try {
                        shutdown().get();
                        bootstrap().get();
                        log.info("tcp客户端重连成功 {}", channel);
                        break;
                    } catch (Exception ex) {
                        log.error("tcp客户端重连失败", ex);
                        if (ex instanceof InterruptedException) {
                            break;
                        }
                    }
                    try {
                        Thread.sleep(recoveryInterval);
                    } catch (InterruptedException e) {
                        log.error("tcp客户端重连中断");
                        break;
                    }
                }
            });
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (autoRecoveryExecutor != null) {
            ThreadUtils.shutdownThreadPool(autoRecoveryExecutor);
        }
    }
}
