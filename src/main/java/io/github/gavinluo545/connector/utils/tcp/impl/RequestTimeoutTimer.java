package io.github.gavinluo545.connector.utils.tcp.impl;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

public class RequestTimeoutTimer {
    public Timer requestTimeoutTimer;

    private RequestTimeoutTimer(Timer requestTimeoutTimer) {
        this.requestTimeoutTimer = requestTimeoutTimer;
    }

    public static class InstanceHolder {
        public static final RequestTimeoutTimer DEFAULT = new RequestTimeoutTimer(new HashedWheelTimer());
    }
}
