package io.github.gavinluo545.connector.connection.message;

import io.github.gavinluo545.connector.utils.tcp.message.Header;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExampleHeader implements Header {

    private int messageId;
    private int deviceId;

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte('@');
        buf.writeByte('@');
        buf.writeInt(messageId);
        buf.writeInt(deviceId);
    }

    @Override
    public void decode(ByteBuf buf) {
        buf.readByte();
        buf.readByte();
        messageId = buf.readInt();
        deviceId = buf.readInt();
    }

}
