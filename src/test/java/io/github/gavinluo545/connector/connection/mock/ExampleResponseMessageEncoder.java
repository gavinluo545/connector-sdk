package io.github.gavinluo545.connector.connection.mock;

import io.github.gavinluo545.connector.connection.message.ExampleResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ExampleResponseMessageEncoder extends MessageToByteEncoder<ExampleResponseMessage> {
    public final static ExampleResponseMessageEncoder INSTANCE = new ExampleResponseMessageEncoder();

    public ExampleResponseMessageEncoder() {
        super(false);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ExampleResponseMessage msg, ByteBuf out) throws Exception {
        msg.encode(out);
    }

}
