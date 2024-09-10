package io.github.gavinluo545.connector.connection.impl;

import io.github.gavinluo545.connector.utils.tcp.impl.message.AbstractFrameMessage;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.function.Function;

@Data
@AllArgsConstructor
public class HeartbeatConfig<I extends AbstractFrameMessage> {
    private boolean enableHeartbeat = false;
    private Integer heartbeatIntervalSeconds = 30;
    private Function<Channel, I> heartbeatMessageFunc = channel -> null;
}
