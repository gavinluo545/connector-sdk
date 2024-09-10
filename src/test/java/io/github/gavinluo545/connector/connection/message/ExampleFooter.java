package io.github.gavinluo545.connector.connection.message;

import io.github.gavinluo545.connector.utils.tcp.message.Footer;
import io.netty.buffer.ByteBuf;

public class ExampleFooter implements Footer {

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte('#');
        buf.writeByte('#');
    }

    @Override
    public void decode(ByteBuf buf) {
        buf.readByte();
        buf.readByte();
    }

}
