package io.github.gavinluo545.connector.utils.tcp;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import io.netty.channel.Channel;

import java.util.List;

/**
 * TcpServer.java
 *
 * @author gavinluo545@gmail.com
 */
public interface TcpServer<I extends FrameMessage, O extends FrameMessage> extends Tcp<I, O, TcpServerConfig<I, O>> {
    /**
     * 获取服务端配置
     *
     * @return 配置
     */
    TcpServerConfig<I, O> getConfig();

    /**
     * 获取客户端
     *
     * @return 客户端
     */
    List<Channel> getClientChannels();

}
