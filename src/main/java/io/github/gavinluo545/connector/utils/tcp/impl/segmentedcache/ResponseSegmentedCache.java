package io.github.gavinluo545.connector.utils.tcp.impl.segmentedcache;

import io.github.gavinluo545.connector.utils.tcp.impl.TcpQPS;
import io.github.gavinluo545.connector.utils.tcp.impl.message.FutureResponse;

public class ResponseSegmentedCache extends SegmentedCache<Integer, FutureResponse> {

    public ResponseSegmentedCache(int segmentsCount) {
        super(segmentsCount);
    }

    @Override
    public void put(Integer key, FutureResponse value, long expireTimestamp, Runnable whenExpire) {
        super.put(key, new ExpiringValue(value, expireTimestamp, whenExpire));
        TcpQPS.responseSegmentedCacheAddQpsIncr();
    }

    @Override
    public void remove(Integer key) {
        super.remove(key);
        TcpQPS.responseSegmentedCacheRemoveQpsIncr();
    }
}

