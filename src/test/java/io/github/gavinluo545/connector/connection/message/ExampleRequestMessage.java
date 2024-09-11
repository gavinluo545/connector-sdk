package io.github.gavinluo545.connector.connection.message;

import io.github.gavinluo545.connector.utils.tcp.impl.message.AbstractFrameMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class ExampleRequestMessage extends AbstractFrameMessage {

    public ExampleRequestMessage(int messageId, int deviceKey, int operation, ConcurrentHashMap<Integer, Integer> ioas) {
        ((ExampleHeader) getHeader()).setMessageId(messageId);
        ((ExampleHeader) getHeader()).setDeviceId(deviceKey);
        ((ExampleRequestPrincipal) getPrincipal()).setOperation(operation);
        ((ExampleRequestPrincipal) getPrincipal()).setIoas(ioas);
        ExampleFooter footer = (ExampleFooter) getFooter();
    }

    @Override
    public void encode(ByteBuf buf) {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            super.encode(buffer);
            buf.writeInt(buffer.readableBytes());
            buf.writeBytes(buffer);
        } finally {
            if (buffer.refCnt() > 0) {
                ReferenceCountUtil.release(buffer);
            }
        }
    }

    @Override
    public ExampleHeader newHeader() {
        return new ExampleHeader();
    }

    @Override
    public ExampleRequestPrincipal newPrincipal() {
        return new ExampleRequestPrincipal();
    }

    @Override
    public ExampleFooter newFooter() {
        return new ExampleFooter();
    }

    @Override
    public Integer getMessageId() {
        return ((ExampleHeader) getHeader()).getMessageId();
    }

}
