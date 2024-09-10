package io.github.gavinluo545.connector.utils.tcp;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;

public interface TcpClient<I extends FrameMessage, O extends FrameMessage> extends Tcp<I,O,TcpClientConfig<I,O>> {

    /**
     * 获取客户端配置
     *
     * @return 配置
     */
    TcpClientConfig<I,O> getConfig();

}
