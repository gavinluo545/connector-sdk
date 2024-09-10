package io.github.gavinluo545.connector.connection;

import io.github.gavinluo545.connector.connection.message.ExampleRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ExampleRequestMessageEncoder extends MessageToByteEncoder<ExampleRequestMessage> {

    public final static ExampleRequestMessageEncoder INSTANCE = new ExampleRequestMessageEncoder();

    public ExampleRequestMessageEncoder() {
        super(false);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ExampleRequestMessage msg, ByteBuf out) throws Exception {
        msg.encode(out);
    }
}
