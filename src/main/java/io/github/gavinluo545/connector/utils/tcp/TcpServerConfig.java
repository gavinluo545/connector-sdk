package io.github.gavinluo545.connector.utils.tcp;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.function.BiFunction;

@Getter
@Setter
public class TcpServerConfig<I extends FrameMessage, O extends FrameMessage> extends AbstractTcpConfig<I, O> {

    /**
     * 对应的是tcp/ip协议listen函数中的backlog参数，函数listen(int socketfd,int backlog)用来初始化服务端可连接队列，服务端处理客户端连接请求是顺序处理的，
     * 所以同一时间只能处理一个客户端连接，多个客户端来的时候，服务端将不能处理的客户端连接请求放在队列中等待处理，backlog参数指定了队列的大小
     */
    private Integer soBacklogSize = 128;
    /**
     * 负责接收客户端的连接请求
     * 它的线程数通常设置为1，因为它主要负责接收连接请求，不需要太多线程
     * BossGroup中的线程会不断轮询注册在其上的ServerSocketChannel的accept事件，
     * 处理完客户端的连接请求后，会将新的NioSocketChannel注册到WorkerGroup中的某个线程的Selector上.
     */
    private Integer bossGroupThreadSize = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));

    public TcpServerConfig(String ip, Integer port, boolean hasMessageId, boolean parcelRequest,
                           BiFunction<String, FrameMessage, Integer> sequenceIdFunc) {
        super(ip, port, hasMessageId, parcelRequest, sequenceIdFunc);
    }
}
