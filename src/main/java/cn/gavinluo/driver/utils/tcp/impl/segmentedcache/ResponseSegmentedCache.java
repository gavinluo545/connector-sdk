package cn.gavinluo.driver.utils.tcp.impl.segmentedcache;

import cn.gavinluo.driver.utils.tcp.impl.TcpQPS;
import cn.gavinluo.driver.utils.tcp.impl.message.FutureResponse;
import cn.hutool.core.date.SystemClock;

import java.util.concurrent.TimeUnit;

public class ResponseSegmentedCache extends SegmentedCache<Integer, FutureResponse> {

    public ResponseSegmentedCache(int segmentsCount) {
        super(segmentsCount);
    }

    @Override
    public void put(Integer key, FutureResponse value) {
        super.put(key, new ExpiringValue(value, SystemClock.now() + TimeUnit.SECONDS.toMillis(value.getResponseUnusedTimeoutSenconds())));
        TcpQPS.responseSegmentedCacheAddQpsIncr();
    }

    @Override
    public void remove(Integer key) {
        super.remove(key);
        TcpQPS.responseSegmentedCacheRemoveQpsIncr();
    }
}

