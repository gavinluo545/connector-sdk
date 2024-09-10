package io.github.gavinluo545.connector.utils.tcp;

import io.github.gavinluo545.connector.utils.tcp.message.FrameMessage;
import lombok.Getter;
import lombok.Setter;

import java.util.function.BiFunction;

/**
 * @author gavinluo545@gmail.com
 */
@Setter
@Getter
public class TcpClientConfig<I extends FrameMessage, O extends FrameMessage> extends AbstractTcpConfig<I,O> {

    /**
     * 客户端与服务端连接超时时长，单位毫秒
     */
    private Integer connectTimeoutMillis = 3000;
    /**
     * 客户端与服务端断开连接后是否自动重连
     */
    private boolean autoReconnect = true;
    /**
     * 客户端与服务端断开连接后的重连间隔，单位秒
     */
    private Integer autoReconnectIntervalSeconds = 60;
    /**
     * 客户端与服务端断开连接后的最大重连尝试时间，单位分
     */
    private Integer autoReconnectMaxMinutes = 1440;

    public TcpClientConfig(String ip, Integer port, boolean hasMessageId, boolean isParcelRequest,
                           BiFunction<String, FrameMessage, Integer> sequenceIdFunc) {
        super(ip, port, hasMessageId, isParcelRequest, sequenceIdFunc);
    }
}
