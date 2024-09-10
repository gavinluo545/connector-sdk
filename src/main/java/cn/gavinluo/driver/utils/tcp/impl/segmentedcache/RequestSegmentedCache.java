package cn.gavinluo.driver.utils.tcp.impl.segmentedcache;

import cn.gavinluo.driver.utils.tcp.impl.TcpQPS;
import cn.gavinluo.driver.utils.tcp.impl.message.FutureRequest;
import cn.hutool.core.date.SystemClock;

import java.util.concurrent.TimeUnit;

public class RequestSegmentedCache extends SegmentedCache<Integer, FutureRequest> {

    public RequestSegmentedCache(int segmentsCount) {
        super(segmentsCount);
    }

    @Override
    public void put(Integer key, FutureRequest value) {
        super.put(key, new ExpiringValue(value, SystemClock.now() + TimeUnit.SECONDS.toMillis(value.getSendRequestTimeoutSenconds() + value.getWaitResponseTimeoutSenconds())));
        TcpQPS.requestSegmentedCacheAddQpsIncr();
    }

    @Override
    public void remove(Integer key) {
        super.remove(key);
        TcpQPS.requestSegmentedCacheRemoveQpsIncr();
    }
}

