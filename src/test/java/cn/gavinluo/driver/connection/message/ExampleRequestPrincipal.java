package cn.gavinluo.driver.connection.message;

import cn.gavinluo.driver.utils.tcp.message.Principal;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

@Data
public class ExampleRequestPrincipal implements Principal {

    protected int operation;
    protected ConcurrentHashMap<Integer, Integer> ioas=new ConcurrentHashMap<>(0);

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte(operation);
        if (operation != 0x02) {
            buf.writeInt(ioas.size());
            if (operation == 0x01) {
                ioas.forEach((ioa, value) -> {
                    buf.writeInt(ioa);
                    buf.writeInt(value);
                });
            } else if (operation == 0x00) {
                ioas.forEach((ioa, value) -> buf.writeInt(ioa));
            }
        }
    }

    @Override
    public void decode(ByteBuf buf) {
        operation = buf.readByte();
        if (operation != 0x02) {
            int count = buf.readInt();
            if (operation == 0x01) {
                for (int i = 0; i < count; i++) {
                    int ioa = buf.readInt();
                    int value = buf.readInt();
                    ioas.put(ioa, value);
                }
            } else if (operation == 0x00) {
                for (int i = 0; i < count; i++) {
                    int ioa = buf.readInt();
                    ioas.put(ioa, 0);
                }
            }
        }
    }

}
