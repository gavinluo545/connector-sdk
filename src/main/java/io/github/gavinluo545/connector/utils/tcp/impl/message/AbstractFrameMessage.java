package io.github.gavinluo545.connector.utils.tcp.impl.message;

import io.github.gavinluo545.connector.utils.tcp.message.Footer;
import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import io.github.gavinluo545.connector.utils.tcp.message.Header;
import io.github.gavinluo545.connector.utils.tcp.message.Principal;
import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public abstract class AbstractFrameMessage implements FrameMessage {

    protected Header header;
    protected Principal principal;
    protected Footer footer;

    @Override
    public void encode(ByteBuf buf) {
        this.getHeader().encode(buf);
        this.getPrincipal().encode(buf);
        this.getFooter().encode(buf);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.getHeader().decode(buf);
        this.getPrincipal().decode(buf);
        this.getFooter().decode(buf);
    }

    public abstract Header newHeader();

    public abstract Principal newPrincipal();

    public abstract Footer newFooter();

    @Override
    public Header getHeader() {
        if (header == null) {
            header = newHeader();
        }
        return header;
    }

    @Override
    public Principal getPrincipal() {
        if (principal == null) {
            principal = newPrincipal();
        }
        return principal;
    }

    @Override
    public Footer getFooter() {
        if (footer == null) {
            footer = newFooter();
        }
        return footer;
    }

    @Override
    public String toString() {
        return "AbstractFrameMessage{" +
                "header=" + header +
                ", principal=" + principal +
                ", footer=" + footer +
                '}';
    }
}
