package io.github.gavinluo545.connector.utils.tcp.impl.segmentedcache;

import io.github.gavinluo545.connector.utils.tcp.impl.TcpQPS;
import io.github.gavinluo545.connector.utils.tcp.impl.message.FutureRequest;

public class RequestSegmentedCache extends SegmentedCache<Integer, FutureRequest> {

    public RequestSegmentedCache(int segmentsCount) {
        super(segmentsCount);
    }

    @Override
    public void put(Integer key, FutureRequest value, long expireTimestamp, Runnable whenExpire) {
        super.put(key, new ExpiringValue(value, expireTimestamp, whenExpire));
        TcpQPS.requestSegmentedCacheAddQpsIncr();
    }

    @Override
    public void remove(Integer key) {
        super.remove(key);
        TcpQPS.requestSegmentedCacheRemoveQpsIncr();
    }
}

