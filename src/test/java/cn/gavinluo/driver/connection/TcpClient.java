package cn.gavinluo.driver.connection;

import cn.gavinluo.driver.connection.message.ExampleRequestMessage;
import cn.gavinluo.driver.connection.message.ExampleResponseMessage;
import cn.gavinluo.driver.utils.tcp.TcpClientConfig;
import cn.gavinluo.driver.utils.tcp.impl.AbstractTcpClient;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

public class TcpClient extends AbstractTcpClient<ExampleRequestMessage, ExampleResponseMessage> {
    public TcpClient(TcpClientConfig<ExampleRequestMessage, ExampleResponseMessage> config) {
        super(config);
    }

    @Override
    public MessageToByteEncoder<ExampleRequestMessage> newMessageToByteEncoder() {
        return ExampleRequestMessageEncoder.INSTANCE;
    }

    @Override
    public ByteToMessageDecoder newBasedFrameDecoder() {
//        return new DelimiterBasedFrameDecoder(65536, false, true, Unpooled.wrappedBuffer(new byte[]{'#', '#'}));
        return new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4);
    }

    @Override
    public ByteToMessageDecoder newByteToMessageDecoder() {
        return new ExampleResponseMessageDecoder();
    }

}
