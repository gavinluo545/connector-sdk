package io.github.gavinluo545.connector.utils.tcp;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface Tcp<I extends FrameMessage, O extends FrameMessage, Config extends TcpConfig<I, O>> {
    Config getConfig();

    Channel getChannel();

    CompletableFuture<Tcp<I, O, Config>> bootstrap();

    boolean isConnected();

    CompletableFuture<Tcp<I, O, Config>> shutdown();

    CompletableFuture<O> sendSyncRequest(Channel channel, I request);

    void sendRequestSyncCallback(Channel channel, I request, BiConsumer<O, Throwable> callback) throws Exception;

    void sendRequestNoNeedResponse(Channel channel, I request) throws Exception;

    void channelActive(ChannelHandlerContext ctx);

    void channelInactive(ChannelHandlerContext ctx);

    void channelRead0(ChannelHandlerContext ctx, O msg);

    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause);

    void startHeartbeat(Channel channel);

    void stopHeartbeat(Channel channel);
}
