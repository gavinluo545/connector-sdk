package io.github.gavinluo545.connector.utils.tcp.impl.segmentedcache;

import io.github.gavinluo545.connector.utils.tcp.impl.TcpQPS;
import io.github.gavinluo545.connector.utils.tcp.impl.message.ResponseEvent;

public class UnknownMessageSegmentedCache extends SegmentedCache<Integer, ResponseEvent> {

    public UnknownMessageSegmentedCache(int segmentsCount) {
        super(segmentsCount);
    }

    @Override
    public void remove(Integer key) {
        super.remove(key);
        TcpQPS.unknownMessageSegmentedCacheRemoveQpsIncr();
    }

    @Override
    public void put(Integer key, ResponseEvent value, long expireTimestamp, Runnable whenExpire) {
        super.put(key, new ExpiringValue(value, expireTimestamp,whenExpire));
        TcpQPS.unknownMessageSegmentedCacheAddQpsIncr();
    }
}

