package cn.gavinluo.driver.connection.mock;

import cn.gavinluo.driver.connection.message.*;
import cn.gavinluo.driver.utils.tcp.TcpClientConfig;
import cn.gavinluo.driver.utils.tcp.impl.AbstractTcpClient;
import cn.hutool.core.util.RandomUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MockTcpClient extends AbstractTcpClient<ExampleResponseMessage, ExampleRequestMessage> {
    public MockTcpClient(TcpClientConfig<ExampleResponseMessage, ExampleRequestMessage> config) {
        super(config);
    }

    @Override
    public MessageToByteEncoder<ExampleResponseMessage> newMessageToByteEncoder() {
        return ExampleResponseMessageEncoder.INSTANCE;
    }

    @Override
    public ByteToMessageDecoder newBasedFrameDecoder() {
//        return new DelimiterBasedFrameDecoder(65536, false, true, Unpooled.wrappedBuffer(new byte[]{'#', '#'}));
        return new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ExampleRequestMessage msg) {
        handleMessage(ctx.channel(), msg);
    }

    public void handleMessage(Channel channel, ExampleRequestMessage msg) {
//        int randomed = RandomUtil.randomInt(0, 10);
//        if (randomed <= 3) {
//            //不响应 让服务端等待响应超时
//            log.info("不响应 让服务端等待响应超时");
//            return;
//        }

        ExampleResponseMessage responseMessage = getExampleResponseMessage(msg);
//        if (randomed == 9) {
//            //模拟非标报文
//            if (isConnected()) {
//                log.info("模拟非标报文");
//            }
//            ((ExampleResponsePrincipal) responseMessage.getPrincipal()).setMockEx(true);
//        }
        if (isConnected()) {
//            if (randomed == 4) {
//                channel.eventLoop().execute(()->{
//                    try {
//                        Thread.sleep(3000);
//                    } catch (InterruptedException e) {
//                        return;
//                    }
//                    //超时响应
//                    log.info("超时响应 让服务端主动上报");
//                    channel.writeAndFlush(responseMessage);
//                });
//            }
            channel.writeAndFlush(responseMessage);
        }
    }

    private static ExampleResponseMessage getExampleResponseMessage(ExampleRequestMessage msg) {
        ConcurrentHashMap<Integer, Integer> ioas = ((ExampleRequestPrincipal) msg.getPrincipal()).getIoas();
        ConcurrentHashMap<Integer, Integer> ioaValues = new ConcurrentHashMap<>();
        if (((ExampleRequestPrincipal) msg.getPrincipal()).getOperation() == 0x01) {
            ioas.forEach((ioa, value) -> ioaValues.put(ioa, 0));
        } else if (((ExampleRequestPrincipal) msg.getPrincipal()).getOperation() == 0x00) {
            ioas.forEach((ioa, value) -> ioaValues.put(ioa, RandomUtil.randomInt()));
        } else if (((ExampleRequestPrincipal) msg.getPrincipal()).getOperation() == 0x02) {
            ((ExampleRequestPrincipal) msg.getPrincipal()).getIoas().clear();
        }
        return new ExampleResponseMessage(msg.getMessageId(), ((ExampleHeader) msg.getHeader()).getDeviceId(), ((ExampleRequestPrincipal) msg.getPrincipal()).getOperation(), ioaValues);
    }

    @Override
    public ByteToMessageDecoder newByteToMessageDecoder() {
        return new ExampleRequestMessageDecoder();
    }

}

