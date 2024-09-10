package cn.gavinluo.driver.connection.mock;

import cn.gavinluo.driver.connection.message.ExampleRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ExampleRequestMessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ExampleRequestMessage exampleMessage = new ExampleRequestMessage();
        exampleMessage.decode(in);
        out.add(exampleMessage);
    }
}
