package io.github.gavinluo545.connector.utils.tcp.message;

import io.netty.buffer.ByteBuf;

public interface BinaryCodec {

    default void encode(ByteBuf buf){};

    default void decode(ByteBuf buf){};

}
