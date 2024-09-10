package cn.gavinluo.driver.utils.tcp;

import cn.gavinluo.driver.utils.tcp.message.FrameMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface Tcp<I extends FrameMessage, O extends FrameMessage, Config extends TcpConfig<I, O>> {
    /**
     * tcp配置项
     */
    Config getConfig();

    /**
     * 获取当前tcp的channel
     */
    Channel getChannel();

    /**
     * 连接
     *
     * @return 结果Future
     */
    CompletableFuture<Tcp<I, O, Config>> bootstrap();

    /**
     * 是否连接
     */
    boolean isConnected();

    /**
     * 关闭
     */
    CompletableFuture<Tcp<I, O, Config>> shutdown();

    /**
     * 请求
     */
    CompletableFuture<O> sendSyncRequest(Channel channel, I request);

    void sendRequestSyncCallback(Channel channel, I request, BiConsumer<O, Throwable> callback) throws Exception;

    void sendRequestNoNeedResponse(Channel channel, I request) throws Exception;

    /**
     * 上线
     */
    void channelActive(ChannelHandlerContext ctx);

    /**
     * 下线
     */
    void channelInactive(ChannelHandlerContext ctx);

    /**
     * 读处理
     */
    void channelRead0(ChannelHandlerContext ctx, O msg);

    /**
     * 业务异常处理
     */
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause);

    /**
     * 启动心跳
     */
    void startHeartbeat(Channel channel);

    /**
     * 停止心跳
     */
    void stopHeartbeat(Channel channel);
}
