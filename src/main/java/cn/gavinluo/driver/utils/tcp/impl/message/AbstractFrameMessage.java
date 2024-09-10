package cn.gavinluo.driver.utils.tcp.impl.message;

import cn.gavinluo.driver.utils.tcp.message.Footer;
import cn.gavinluo.driver.utils.tcp.message.FrameMessage;
import cn.gavinluo.driver.utils.tcp.message.Header;
import cn.gavinluo.driver.utils.tcp.message.Principal;
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
