package cn.gavinluo.driver.utils.tcp.impl.segmentedcache;

import cn.gavinluo.driver.utils.tcp.impl.TcpQPS;
import cn.gavinluo.driver.utils.tcp.impl.message.ResponseEvent;
import cn.hutool.core.date.SystemClock;

import java.util.concurrent.TimeUnit;

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
    public void put(Integer key, ResponseEvent value) {
        super.put(key, new ExpiringValue(value, SystemClock.now() + TimeUnit.SECONDS.toMillis(value.getUnknownMeesageUnusedTimeoutSenconds())));
        TcpQPS.unknownMessageSegmentedCacheAddQpsIncr();
    }
}

