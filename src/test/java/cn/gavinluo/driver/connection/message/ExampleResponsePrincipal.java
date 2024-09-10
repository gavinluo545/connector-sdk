package cn.gavinluo.driver.connection.message;

import cn.gavinluo.driver.utils.tcp.message.Principal;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

@Data
public class ExampleResponsePrincipal implements Principal {

    protected int operation;
    protected boolean mockEx;
    protected final ConcurrentHashMap<Integer, Integer> ioas = new ConcurrentHashMap<>();

    public void put(ConcurrentHashMap<Integer, Integer> ioas) {
        this.ioas.putAll(ioas);
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte(operation);
        if (operation != 0x02) {
            if (mockEx) {
                buf.writeByte(0x55);
            }
            buf.writeInt(ioas.size());
            ioas.forEach((ioa, value) -> {
                buf.writeInt(ioa);
                buf.writeInt(value);
            });
        }
    }

    @Override
    public void decode(ByteBuf buf) {
        operation = buf.readByte();
        if (operation != 0x02) {
            int count = buf.readInt();
            for (int i = 0; i < count; i++) {
                int ioa = buf.readInt();
                int value = buf.readInt();
                ioas.put(ioa, value);
            }
        }
    }

}
