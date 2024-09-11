package io.github.gavinluo545.connector.utils.tcp;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import io.netty.channel.Channel;
import lombok.Data;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Data
public class AbstractTcpConfig<I extends FrameMessage, O extends FrameMessage> implements TcpConfig<I, O> {

    /**
     * ip
     */
    private final String ip;
    /**
     * port
     */
    private final Integer port;
    /**
     * 指定是否启用TCP保活机制
     * 对应于套接字选项中的SO_KEEPALIVE，该参数用于设置TCP连接，当设置该选项以后，连接会测试链接的状态，
     * 这个选项用于可能长时间没有数据交流的连接。当设置该选项以后，如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文。
     */
    private boolean soKeepalive = true;
    /**
     * 在TCP通信中，数据通常被缓冲并等待一段时间，以便将多个小数据包合并成一个更大的数据包，以减少网络开销。这种缓冲的行为可以提高网络的利用率，
     * 但也会引入一些延迟，因为数据需要等待其他数据以形成更大的数据包。
     * <p>
     * 通过启用TCP_NODELAY选项，可以禁用这种缓冲，从而允许小数据包立即传输。这对某些应用程序非常重要，特别是需要低延迟的应用程序，
     * 如实时音视频通信、在线游戏等。当TCP_NODELAY被启用时，数据将立即发送，而不需要等待缓冲区中的其他数据。
     */
    private boolean tcpNodelay = true;
    /**
     * 直接设置底层操作系统的TCP接收缓冲区的大小。这个缓冲区是由操作系统管理的，Netty通过这个选项来调整TCP层的接收窗口大小，以优化网络性能
     */
    private int soRcvbufSize = 8192;
    /**
     * 直接设置底层操作系统的TCP发送缓冲区的大小。这个缓冲区是由操作系统管理的，Netty通过这个选项来调整TCP层的发送窗口大小，以优化网络性能
     */
    private int soSndbufSize = 8192;

    /**
     * 请求超时
     */
    byte sendRequestTimeoutSenconds = 1;
    /**
     * 等待响应超时
     */
    byte waitResponseTimeoutSenconds = 2;
    byte responseUnusedTimeoutSenconds = 5;
    byte unknownMeesageUnusedTimeoutSenconds = 5;
    /**
     * channel上线监听
     */
    private Consumer<Channel> channelActiveListener;
    /**
     * channel下线监听
     */
    private Consumer<Channel> channelInActiveListener;
    /**
     * 非请求收到消息的监听
     */
    private BiConsumer<Channel, O> unknownMessageListener;
    /**
     * 业务异常监听
     */
    private BiConsumer<Channel, Throwable> exceptionCaughtListener;
    private boolean enableHeartbeat = false;
    private Integer heartbeatIntervalSeconds = 30;
    private Function<Channel, I> heartbeatMessageFunc = channel -> null;
    private BiFunction<String, FrameMessage, Integer> sequenceIdFunc;

    int requestRateLimiterLimitForSeconds = 10;
    int responseRateLimiterLimitForSeconds = 10;

    public AbstractTcpConfig(String ip, Integer port,
                             BiFunction<String, FrameMessage, Integer> sequenceIdFunc) {
        this.ip = ip;
        this.port = port;
        this.sequenceIdFunc = sequenceIdFunc;
    }
}
