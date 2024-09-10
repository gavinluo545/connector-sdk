package cn.gavinluo.driver.utils.tcp;

import cn.gavinluo.driver.utils.tcp.message.FrameMessage;
import io.netty.channel.Channel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TcpConfig<I extends FrameMessage, O extends FrameMessage> {

    /**
     * ip
     */
    String getIp();

    /**
     * port
     */
    Integer getPort();

    /**
     * 是否有消息id
     */
    boolean isHasMessageId();

    /**
     * 支持并发请求
     */
    boolean isParcelRequest();

    /**
     * 指定是否启用TCP保活机制
     * 对应于套接字选项中的SO_KEEPALIVE，该参数用于设置TCP连接，当设置该选项以后，连接会测试链接的状态，
     * 这个选项用于可能长时间没有数据交流的连接。当设置该选项以后，如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文。
     */
    boolean isSoKeepalive();

    /**
     * 在TCP通信中，数据通常被缓冲并等待一段时间，以便将多个小数据包合并成一个更大的数据包，以减少网络开销。这种缓冲的行为可以提高网络的利用率，
     * 但也会引入一些延迟，因为数据需要等待其他数据以形成更大的数据包。
     * <p>
     * 通过启用TCP_NODELAY选项，可以禁用这种缓冲，从而允许小数据包立即传输。这对某些应用程序非常重要，特别是需要低延迟的应用程序，
     * 如实时音视频通信、在线游戏等。当TCP_NODELAY被启用时，数据将立即发送，而不需要等待缓冲区中的其他数据。
     */
    boolean isTcpNodelay();

    /**
     * 直接设置底层操作系统的TCP接收缓冲区的大小。这个缓冲区是由操作系统管理的，Netty通过这个选项来调整TCP层的接收窗口大小，以优化网络性能
     */
    int getSoRcvbufSize();

    /**
     * 直接设置底层操作系统的TCP发送缓冲区的大小。这个缓冲区是由操作系统管理的，Netty通过这个选项来调整TCP层的发送窗口大小，以优化网络性能
     */
    int getSoSndbufSize();

    /**
     * 请求超时
     */
    byte getSendRequestTimeoutSenconds();

    /**
     * 等待响应超时
     */
    byte getWaitResponseTimeoutSenconds();

    byte getResponseUnusedTimeoutSenconds();

    byte getUnknownMeesageUnusedTimeoutSenconds();

    /**
     * channel上线监听
     */
    Consumer<Channel> getChannelActiveListener();

    /**
     * channel下线监听
     */
    Consumer<Channel> getChannelInActiveListener();

    /**
     * 非请求收到消息的监听
     */
    BiConsumer<Channel, O> getUnknownMessageListener();

    /**
     * 业务异常监听
     */
    BiConsumer<Channel, Throwable> getExceptionCaughtListener();

    int getRequestRateLimiterLimitForSeconds();

    int getResponseRateLimiterLimitForSeconds();

    boolean isEnableHeartbeat();

    Integer getHeartbeatIntervalSeconds();

    Function<Channel, I> getHeartbeatMessageFunc();

}
