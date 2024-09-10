package io.github.gavinluo545.connector.utils.tcp.message;

public interface FrameMessage extends BinaryCodec, MessageId {
    Header getHeader();

    Principal getPrincipal();

    Footer getFooter();

    boolean hasMessageId();

}
