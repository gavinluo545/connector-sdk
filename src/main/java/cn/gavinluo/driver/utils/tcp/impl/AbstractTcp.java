package cn.gavinluo.driver.utils.tcp.impl;

import cn.gavinluo.driver.utils.executor.ExecutorFactory;
import cn.gavinluo.driver.utils.executor.NameThreadFactory;
import cn.gavinluo.driver.utils.executor.ThreadUtils;
import cn.gavinluo.driver.utils.tcp.AbstractTcpConfig;
import cn.gavinluo.driver.utils.tcp.Tcp;
import cn.gavinluo.driver.utils.tcp.impl.disruptor.RequestDisruptor;
import cn.gavinluo.driver.utils.tcp.impl.disruptor.ResponseDisruptor;
import cn.gavinluo.driver.utils.tcp.impl.disruptor.UnknownMessageDisruptor;
import cn.gavinluo.driver.utils.tcp.impl.message.AbstractFrameMessage;
import cn.gavinluo.driver.utils.tcp.impl.message.FutureRequest;
import cn.gavinluo.driver.utils.tcp.impl.message.FutureResponse;
import cn.gavinluo.driver.utils.tcp.impl.message.ResponseEvent;
import cn.gavinluo.driver.utils.tcp.impl.segmentedcache.RequestSegmentedCache;
import cn.gavinluo.driver.utils.tcp.impl.segmentedcache.ResponseSegmentedCache;
import cn.gavinluo.driver.utils.tcp.impl.segmentedcache.UnknownMessageSegmentedCache;
import cn.gavinluo.driver.utils.tcp.message.FrameMessage;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

@Slf4j
@Getter
public abstract class AbstractTcp<I extends AbstractFrameMessage, O extends AbstractFrameMessage, Config extends AbstractTcpConfig<I, O>> implements Tcp<I, O, Config> {

    protected static final Map<String/*serverId-clientId clientId-clientId*/, Channel> clientChannles = new ConcurrentHashMap<>();
    protected static final EventLoopGroup bossGroup = new NioEventLoopGroup(Integer.parseInt(System.getProperty("shareNettyBossGroupThreadNum", String.valueOf(Runtime.getRuntime().availableProcessors()))), new NameThreadFactory(() -> "ShareNettyBossGroup"));
    protected static final EventLoopGroup workerGroup = new NioEventLoopGroup(Integer.parseInt(System.getProperty("shareNettyWorkerGroupThreadNum", String.valueOf(Runtime.getRuntime().availableProcessors() * 2))), new NameThreadFactory(() -> "ShareNettyWorkerGroup"));
    protected static final RequestSegmentedCache requestSegmentedCache = new RequestSegmentedCache(Integer.parseInt(System.getProperty("shareRequestSegmentedCount", String.valueOf(32))));
    protected static final ResponseSegmentedCache responseSegmentedCache = new ResponseSegmentedCache(Integer.parseInt(System.getProperty("shareResponseSegmentedCount", String.valueOf(32))));
    protected static final UnknownMessageSegmentedCache unknownMessageSegmentedCache = new UnknownMessageSegmentedCache(Integer.parseInt(System.getProperty("shareUnknownMessageSegmentedCount", String.valueOf(32))));
    protected static final RequestDisruptor requestDisruptor = new RequestDisruptor(workerGroup, "ShareRequestDisruptor", Integer.parseInt(System.getProperty("shareRequestDisruptorRingBufferSize", String.valueOf(65536))), Integer.parseInt(System.getProperty("shareRequestWorkHandlerNum", String.valueOf(Runtime.getRuntime().availableProcessors()))), clientChannles::get, requestSegmentedCache::get, requestSegmentedCache::remove);
    protected static final ResponseDisruptor responseDisruptor = new ResponseDisruptor(workerGroup, "ShareResponseDisruptor", Integer.parseInt(System.getProperty("shareResponseDisruptorRingBufferSize", String.valueOf(65536))), Integer.parseInt(System.getProperty("shareResponceWorkHandlerNum", String.valueOf(Runtime.getRuntime().availableProcessors()))), responseSegmentedCache::get, responseSegmentedCache::remove, requestSegmentedCache::get, requestSegmentedCache::remove);
    protected static final UnknownMessageDisruptor unknownMessageDisruptor = new UnknownMessageDisruptor(workerGroup, "ShareUnknownMessageDisruptor", Integer.parseInt(System.getProperty("shareUnknownMessageDisruptorRingBufferSize", String.valueOf(65536))), Integer.parseInt(System.getProperty("shareUnknownMessageWorkHandlerNum", String.valueOf(Runtime.getRuntime().availableProcessors()))), clientChannles::get, unknownMessageSegmentedCache::get, unknownMessageSegmentedCache::remove);

    protected static final int ST_NOT_STARTED = 1;
    protected static final int ST_STARTED = 2;
    protected static final int ST_SHUTTING_DOWN = 3;
    protected static final int ST_SHUTDOWN = 4;
    @SuppressWarnings("rawtypes")
    protected static final AtomicIntegerFieldUpdater<AbstractTcp> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AbstractTcp.class, "state");

    protected final Config config;
    private volatile int state = ST_NOT_STARTED;
    protected final ClosedChannelException closedChannelException = new ClosedChannelException();
    protected final Map<String, ScheduledExecutorService> heartbeats = new ConcurrentHashMap<>();
    protected CompletableFuture<Tcp<I, O, Config>> shutdownFuture = new CompletableFuture<>();

    protected Channel channel;

    protected AtomicReference<FutureRequest> oneRequest;

    protected RateLimiterRegistry requestRateLimiterRegistry;
    protected RateLimiterRegistry responseRateLimiterRegistry;

    public AbstractTcp(Config tcpConfig) {
        config = tcpConfig;
        if (!config.isParcelRequest()) {
            oneRequest = new AtomicReference<>();
        }
    }

    public abstract MessageToByteEncoder<I> newMessageToByteEncoder();

    public abstract ByteToMessageDecoder newBasedFrameDecoder();

    public abstract ByteToMessageDecoder newByteToMessageDecoder();

    public boolean stateUpdate(int state) {
        int oldState = this.state;
        return STATE_UPDATER.compareAndSet(this, oldState, state);
    }

    public void initChannel(ChannelPipeline pipeline) {
        pipeline.addLast(newMessageToByteEncoder());
        pipeline.addLast(newBasedFrameDecoder());
        pipeline.addLast(newByteToMessageDecoder());
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    public CompletableFuture<Tcp<I, O, Config>> bootstrap() {
        CompletableFuture<Tcp<I, O, Config>> future = new CompletableFuture<>();
        if (state == ST_SHUTTING_DOWN) {
            future.completeExceptionally(new IllegalStateException("服务器正在关闭，请等待关闭完成后再开始"));
            return future;
        }
        if (state == ST_STARTED) {
            return CompletableFuture.completedFuture(this);
        }
        try {
            shutdownFuture = new CompletableFuture<>();

            requestRateLimiterRegistry = RateLimiterRegistry.of(RateLimiterConfig.custom().limitRefreshPeriod(Duration.ofMillis(1)).limitForPeriod(config.getRequestRateLimiterLimitForSeconds()).timeoutDuration(Duration.ofMillis(25)).build());
            responseRateLimiterRegistry = RateLimiterRegistry.of(RateLimiterConfig.custom().limitRefreshPeriod(Duration.ofMillis(1)).limitForPeriod(config.getResponseRateLimiterLimitForSeconds()).timeoutDuration(Duration.ofMillis(25)).build());

            requestRateLimiterRegistry.rateLimiter("default", RateLimiterConfig.custom().limitRefreshPeriod(Duration.ofMillis(1)).limitForPeriod(config.getResponseRateLimiterLimitForSeconds()).timeoutDuration(Duration.ofMillis(25)).build());
            responseRateLimiterRegistry.rateLimiter("default", RateLimiterConfig.custom().limitRefreshPeriod(Duration.ofMillis(1)).limitForPeriod(config.getResponseRateLimiterLimitForSeconds()).timeoutDuration(Duration.ofMillis(25)).build());

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<Tcp<I, O, Config>> shutdown() {
        if (state == ST_SHUTDOWN) {
            return CompletableFuture.completedFuture(this);
        } else if (state == ST_SHUTTING_DOWN) {
            return shutdownFuture;
        } else if (state == ST_STARTED) {
            if (!stateUpdate(ST_SHUTTING_DOWN)) {
                //已经开始
                return shutdownFuture;
            }
        } else {
            //未启动
            return CompletableFuture.completedFuture(this);
        }

        if (isActive(channel)) {
            channel.close();
        }
        List<String> removes = new CopyOnWriteArrayList<>();
        clientChannles.forEach((cid, clientChannel) -> {
            if (cid.startsWith(channel.id().asShortText())) {
                if (isActive(clientChannel)) {
                    clientChannel.close();
                    removes.add(cid);
                }
            }
        });
        removes.forEach(clientChannles::remove);
        String channelString = channel == null ? "" : channel.toString();
        channel = null;
        stateUpdate(ST_SHUTDOWN);

        heartbeats.forEach((k, v) -> ThreadUtils.shutdownThreadPool(v));
        requestRateLimiterRegistry.getAllRateLimiters().asJava().stream().map(RateLimiter::getName).forEach(requestRateLimiterRegistry::remove);
        requestRateLimiterRegistry = null;
        responseRateLimiterRegistry.getAllRateLimiters().asJava().stream().map(RateLimiter::getName).forEach(responseRateLimiterRegistry::remove);
        responseRateLimiterRegistry = null;
        shutdownFuture.complete(this);
        log.info("Channel关闭成功:{}", channelString);

        return shutdownFuture;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.info("Channel上线:{}", channel);
        String channelId = channel.id().asShortText();
        clientChannles.putIfAbsent(String.format("%s-%s", getChannel().id().asShortText(), channelId), ctx.channel());
        if (config.isParcelRequest()) {
            requestRateLimiterRegistry.rateLimiter(channelId);
            responseRateLimiterRegistry.rateLimiter(channelId);
        }
        startHeartbeat(channel);
        try {
            Optional.ofNullable(config.getChannelActiveListener()).ifPresent(c -> {
                workerGroup.execute(() -> {
                    try {
                        c.accept(channel);
                    } catch (Exception ignored) {
                    }
                });
            });
        } catch (Exception er) {
            log.error("Channel上线监听执行错误:{}", channelId, er);
        }
    }

    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.info("Channel下线:{}", channel);
        String channelId = channel.id().asShortText();
        stopHeartbeat(channel);
        if (config.isParcelRequest()) {
            requestRateLimiterRegistry.remove(channelId);
            responseRateLimiterRegistry.remove(channelId);
        }
        clientChannles.remove(String.format("%s-%s", getChannel().id().asShortText(), channelId));
        try {
            Optional.ofNullable(config.getChannelInActiveListener()).ifPresent(c -> {
                workerGroup.execute(() -> {
                    try {
                        c.accept(channel);
                    } catch (Exception ignored) {
                    }
                });
            });
        } catch (Exception er) {
            log.error("Channel inactive监听执行错误:{}", channelId, er);
        } finally {
            if (channel.isActive()) {
                channel.close();
            }
        }
    }

    @Override
    public void startHeartbeat(Channel channel) {
        if (config.isEnableHeartbeat()) {
            ScheduledExecutorService heartbeatScheduled = ExecutorFactory.newSingleScheduledExecutorService(AbstractTcp.class.getCanonicalName(), new NameThreadFactory(() -> String.format("TcpChannelHeartbeat_%s", channel.id().asShortText())));
            heartbeatScheduled.scheduleAtFixedRate(() -> {
                if (isConnected()) {
                    AbstractFrameMessage frameMessage;
                    try {
                        frameMessage = config.getHeartbeatMessageFunc().apply(channel);
                    } catch (Exception e) {
                        log.error("构建心跳包错误:{}", e.getMessage());
                        return;
                    }
                    ChannelPromise promise = channel.newPromise();
                    RequestTimeoutTimer.InstanceHolder.DEFAULT.requestTimeoutTimer.newTimeout(t -> {
                        if (t.isCancelled()) {
                            return;
                        }
                        promise.cancel(true);
                    }, 500, TimeUnit.MILLISECONDS);
                    promise.addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            Throwable cause = future.cause();
                            log.error("心跳包 {} {}", cause.getClass(), cause.getMessage());
                        }
                    });
                    channel.writeAndFlush(frameMessage, promise);
                }
            }, 0, Math.min(config.getHeartbeatIntervalSeconds(), 1), TimeUnit.SECONDS);
            heartbeats.putIfAbsent(channel.id().asShortText(), heartbeatScheduled);
        }
    }

    @Override
    public void stopHeartbeat(Channel channel) {
        if (config.isEnableHeartbeat()) {
            ScheduledExecutorService scheduledExecutorService = heartbeats.get(channel.id().asShortText());
            if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
                ThreadUtils.shutdownThreadPool(scheduledExecutorService);
                heartbeats.remove(channel.id().asShortText());
                log.info("{} Stop heartbeat successful", channel.id().asShortText());
            }
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, O msg) {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        RateLimiter rateLimiter = responseRateLimiterRegistry.find(channelId).orElse(responseRateLimiterRegistry.rateLimiter("default"));
        boolean acquirePermission = rateLimiter.acquirePermission();

        boolean hasMessageId = config.isHasMessageId();
        if (!acquirePermission) {
            if (hasMessageId) {
                //获取请求 然后拒绝
                int id = getSequenceId(channelId, msg);
                FutureRequest futureRequest = getRequest(id);
                if (futureRequest != null) {
                    rejectRequest(futureRequest, id, rateLimiter);
                    return;
                }
            }
            log.error("被流控拒绝:channelId={} messageId={}", channelId, msg.getMessageId());
        } else {
            if (hasMessageId) {
                //获取请求 然后响应
                int id = getSequenceId(channelId, msg);
                FutureRequest futureRequest = getRequest(id);
                if (futureRequest != null) {
                    boolean tryPublishEvent = responseDisruptor.getRingBuffer().tryPublishEvent(((event, sequence) -> {
                        event.setSequenceId(id);
                        responseSegmentedCache.put(id, new FutureResponse(msg, id, config.getResponseUnusedTimeoutSenconds()));
                    }));
                    if (!tryPublishEvent) {
                        requestSegmentedCache.remove(id);
                        responseSegmentedCache.remove(id);
                    }
                    return;
                }
            }
            //找不到请求 没有消息id
            publishUnknowMessage(msg, channelId);
        }
    }

    public void rejectRequest(FutureRequest futureRequest, int id, RateLimiter rateLimiter) {
        BiConsumer<FrameMessage, Throwable> callback = futureRequest.getCallback();
        if (callback != null) {
            workerGroup.execute(() -> {
                try {
                    callback.accept(null, RequestNotPermitted.createRequestNotPermitted(rateLimiter));
                } catch (Exception ignored) {
                } finally {
                    requestSegmentedCache.remove(id);
                }
            });
            return;
        }
        try {
            CompletableFuture<FrameMessage> future = futureRequest.getFuture();
            if (future != null && unFinished(future)) {
                future.completeExceptionally(RequestNotPermitted.createRequestNotPermitted(rateLimiter));
            }
        } finally {
            requestSegmentedCache.remove(id);
        }
    }

    public FutureRequest getRequest(int id) {
        if (!config.isParcelRequest()) {
            return oneRequest.get();
        } else {
            return requestSegmentedCache.get(id);
        }
    }

    @SuppressWarnings("unchecked")
    public void publishUnknowMessage(O msg, String channelId) {
        int sequenceId = getSequenceId(channelId, msg);
        boolean tryPublishEvent = unknownMessageDisruptor.getRingBuffer().tryPublishEvent(((event, sequence) -> {
            event.setSequenceId(sequenceId);
            unknownMessageSegmentedCache.put(sequenceId, new ResponseEvent(String.format("%s-%s", getChannel().id().asShortText(), channelId), msg, (channel, frameMessage) -> config.getUnknownMessageListener().accept(channel, (O) frameMessage), config.getUnknownMeesageUnusedTimeoutSenconds()));
        }));
        if (!tryPublishEvent) {
            unknownMessageSegmentedCache.remove(sequenceId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        log.error("Channel异常:{}", channelId, cause);
        Optional.ofNullable(config.getExceptionCaughtListener()).ifPresent(c -> {
            workerGroup.execute(() -> {
                try {
                    c.accept(channel, cause);
                } catch (Exception ignored) {
                }
            });
        });
        ctx.channel().close();
    }

    public boolean isActive(Channel channel) {
        return channel != null && channel.isActive();
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<O> sendSyncRequest(Channel channel, I request) {
        CompletableFuture<O> future = new CompletableFuture<>();
        CompletableFuture<FrameMessage> wrapFuture = new CompletableFuture<FrameMessage>().whenComplete((frameMessage, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                future.complete((O) frameMessage);
            }
        });
        if (!isActive(channel)) {
            future.completeExceptionally(closedChannelException);
            return future;
        }
        String channelId = channel.id().asShortText();
        String cid = String.format("%s-%s", getChannel().id().asShortText(), channelId);
        if (!config.isParcelRequest()) {
            if (oneRequest.get() != null) {
                future.completeExceptionally(new RejectedExecutionException("不支持并发请求，并且当前有一个未完成的请求"));
            } else {
                oneRequest.set(new FutureRequest(cid, request, wrapFuture, config.getWaitResponseTimeoutSenconds(), config.getSendRequestTimeoutSenconds()));
            }
            return future;
        }
        if (!config.isHasMessageId()) {
            future.completeExceptionally(new RejectedExecutionException("如果并发没有 messsageId，该方法不支持等待响应，因此使用 sendRequestNoNeedResponse 执行 unknownMessageListener 报告"));
            return future;
        }
        try {
            RateLimiter rateLimiter = requestRateLimiterRegistry.find(channelId).orElse(requestRateLimiterRegistry.rateLimiter("default"));
            if (rateLimiter.acquirePermission()) {
                int sequenceId = getSequenceId(channelId, request);
                boolean tryPublishEvent = requestDisruptor.getRingBuffer().tryPublishEvent(((event, sequence) -> {
                    event.setSequenceId(sequenceId);
                    requestSegmentedCache.put(sequenceId, new FutureRequest(cid, request, wrapFuture, config.getWaitResponseTimeoutSenconds(), config.getSendRequestTimeoutSenconds()));
                }));
                if (!tryPublishEvent) {
                    requestSegmentedCache.remove(sequenceId);
                }
            } else {
                throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sendRequestSyncCallback(Channel channel, I request, BiConsumer<O, Throwable> callback) throws Exception {
        try {
            if (!isActive(channel)) {
                throw closedChannelException;
            }
            BiConsumer<FrameMessage, Throwable> wrapCallback = (frameMessage, throwable) -> callback.accept((O) frameMessage, throwable);
            String channelId = channel.id().asShortText();
            String cid = String.format("%s-%s", getChannel().id().asShortText(), channelId);
            if (!config.isParcelRequest()) {
                if (oneRequest.get() != null) {
                    throw new RejectedExecutionException("不支持并发请求，并且当前有一个未完成的请求");
                } else {
                    oneRequest.set(new FutureRequest(cid, request, wrapCallback, config.getWaitResponseTimeoutSenconds(), config.getSendRequestTimeoutSenconds()));
                }
                return;
            }
            if (!config.isHasMessageId()) {
                callback.accept(null, new RejectedExecutionException("如果并发没有 messsageId，该方法不支持等待响应，因此使用 sendRequestNoNeedResponse 执行 unknownMessageListener 报告"));
                return;
            }
            RateLimiter rateLimiter = requestRateLimiterRegistry.find(channelId).orElse(requestRateLimiterRegistry.rateLimiter("default"));
            if (rateLimiter.acquirePermission()) {
                int sequenceId = getSequenceId(channelId, request);
                boolean tryPublishEvent = requestDisruptor.getRingBuffer().tryPublishEvent(((event, sequence) -> {
                    event.setSequenceId(sequenceId);
                    requestSegmentedCache.put(sequenceId, new FutureRequest(cid, request, wrapCallback, config.getWaitResponseTimeoutSenconds(), config.getSendRequestTimeoutSenconds()));
                }));
                if (!tryPublishEvent) {
                    requestSegmentedCache.remove(sequenceId);
                }
            } else {
                throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
            }
        } catch (Exception e) {
            workerGroup.execute(() -> {
                try {
                    callback.accept(null, e);
                } catch (Exception ignored) {
                }
            });
        }
    }

    @Override
    public void sendRequestNoNeedResponse(Channel channel, I request) throws Exception {
        if (!isActive(channel)) {
            throw closedChannelException;
        }
        String channelId = channel.id().asShortText();
        String cid = String.format("%s-%s", getChannel().id().asShortText(), channelId);
        if (!config.isParcelRequest()) {
            if (oneRequest.get() != null) {
                throw new RejectedExecutionException("不支持并发请求，并且当前有一个未完成的请求");
            } else {
                oneRequest.set(new FutureRequest(cid, request));
            }
            return;
        }

        RateLimiter rateLimiter = requestRateLimiterRegistry.find(channelId).orElse(requestRateLimiterRegistry.rateLimiter("default"));
        if (rateLimiter.acquirePermission()) {
            int sequenceId = getSequenceId(channelId, request);
            boolean tryPublishEvent = requestDisruptor.getRingBuffer().tryPublishEvent(((event, sequence) -> {
                event.setSequenceId(sequenceId);
                requestSegmentedCache.put(sequenceId, new FutureRequest(cid, request));
            }));
            if (!tryPublishEvent) {
                requestSegmentedCache.remove(sequenceId);
            }
        }
        throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
    }

    public int getSequenceId(String channelId, FrameMessage request) {
        return config.getSequenceIdFunc().apply(channelId, request);
    }

    public boolean unFinished(CompletableFuture<?> completableFuture) {
        return !completableFuture.isCancelled() && !completableFuture.isDone() && !completableFuture.isCompletedExceptionally();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown().get();
    }

    @Override
    public String toString() {
        return config.getIp() + ":" + config.getPort() + " state " + state;
    }

    public class InboundHandler extends SimpleChannelInboundHandler<O> {

        public final AbstractTcp<I, O, Config> tcp;

        public InboundHandler(AbstractTcp<I, O, Config> tcp) {
            this.tcp = tcp;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            try {
                tcp.channelActive(ctx);
            } catch (Exception e) {
                log.error("Channel上线", e);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            try {
                tcp.channelInactive(ctx);
            } catch (Exception e) {
                log.error("Channel下线", e);
            }
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, O msg) throws Exception {
            try {
                tcp.channelRead0(ctx, msg);
            } catch (Exception e) {
                log.error("Channel读错误", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            try {
                tcp.exceptionCaught(ctx, cause);
            } catch (Exception e) {
                log.error("Channel未知异常", e);
            }
        }
    }
}
