package cn.gavinluo.driver.utils.tcp.message;

import io.netty.buffer.ByteBuf;

/**
 * 字节编码解码
 *
 * @author gavinluo7@foxmail.com
 */
public interface BinaryCodec {

    /**
     * 编码
     *
     * @param buf 需要编入的ByteBuf
     */
    default void encode(ByteBuf buf){};

    /**
     * 解码
     *
     * @param buf 待编码ByteBuf
     */
    default void decode(ByteBuf buf){};

}
