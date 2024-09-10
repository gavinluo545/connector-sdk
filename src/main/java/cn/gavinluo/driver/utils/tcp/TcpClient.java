package cn.gavinluo.driver.utils.tcp;

import cn.gavinluo.driver.utils.tcp.message.FrameMessage;

/**
 * @author gavinluo7@foxmail.com
 */
public interface TcpClient<I extends FrameMessage, O extends FrameMessage> extends Tcp<I,O,TcpClientConfig<I,O>> {

    /**
     * 获取客户端配置
     *
     * @return 配置
     */
    TcpClientConfig<I,O> getConfig();

}
