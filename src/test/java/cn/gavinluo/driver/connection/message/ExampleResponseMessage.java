package cn.gavinluo.driver.connection.message;

import cn.gavinluo.driver.utils.tcp.impl.message.AbstractFrameMessage;
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
public class ExampleResponseMessage extends AbstractFrameMessage {

    public ExampleResponseMessage(Integer messageId, Integer deviceKey, Integer operation, ConcurrentHashMap<Integer, Integer> ioas) {
        ((ExampleHeader) getHeader()).setMessageId(messageId);
        ((ExampleHeader) getHeader()).setDeviceId(deviceKey);
        ((ExampleResponsePrincipal) getPrincipal()).setOperation(operation);
        ((ExampleResponsePrincipal) getPrincipal()).put(ioas);
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
    public ExampleResponsePrincipal newPrincipal() {
        return new ExampleResponsePrincipal();
    }

    @Override
    public ExampleFooter newFooter() {
        return new ExampleFooter();
    }

    @Override
    public boolean hasMessageId() {
        return true;
    }

    @Override
    public Integer getMessageId() {
        return ((ExampleHeader) getHeader()).getMessageId();
    }
}
