package cn.gavinluo.driver.connection;

import cn.gavinluo.driver.connection.message.ExampleResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ExampleResponseMessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ExampleResponseMessage exampleMessage = new ExampleResponseMessage();
        exampleMessage.decode(in);
        out.add(exampleMessage);
    }
}
