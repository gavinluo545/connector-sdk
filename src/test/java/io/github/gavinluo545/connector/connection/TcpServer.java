package io.github.gavinluo545.connector.connection;

import io.github.gavinluo545.connector.connection.message.ExampleRequestMessage;
import io.github.gavinluo545.connector.connection.message.ExampleResponseMessage;
import io.github.gavinluo545.connector.utils.tcp.TcpServerConfig;
import io.github.gavinluo545.connector.utils.tcp.impl.AbstractTcpServer;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

public class TcpServer extends AbstractTcpServer<ExampleRequestMessage, ExampleResponseMessage> {
    public TcpServer(TcpServerConfig<ExampleRequestMessage, ExampleResponseMessage> config) {
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
