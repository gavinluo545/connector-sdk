package cn.gavinluo.driver.utils.tcp.message;

public interface FrameMessage extends BinaryCodec, MessageId {
    /**
     * 获取头部
     *
     * @return 头部
     */
    Header getHeader();

    /**
     * 获取主体
     *
     * @return 主体
     */
    Principal getPrincipal();

    /**
     * 获取尾部
     *
     * @return 尾部
     */
    Footer getFooter();

    /**
     * 判断是否有消息ID
     *
     * @return 是否是否有消息ID
     */
    boolean hasMessageId();

}
