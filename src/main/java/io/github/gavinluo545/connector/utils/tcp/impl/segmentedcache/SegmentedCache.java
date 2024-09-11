package io.github.gavinluo545.connector.utils.tcp.impl.segmentedcache;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.lang.Assert;
import io.github.gavinluo545.connector.utils.executor.ExecutorFactory;
import io.github.gavinluo545.connector.utils.executor.NameThreadFactory;
import io.github.gavinluo545.connector.utils.executor.ThreadUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public abstract class SegmentedCache<K, V> {
    protected final ConcurrentHashMap<K, ExpiringValue>[] segments;
    protected final int segmentsCount;
    protected ScheduledExecutorService expireExecutor;

    @SuppressWarnings({"unchecked"})
    public SegmentedCache(int segmentsCount) {
        Assert.isTrue(segmentsCount > 0, "segmentsCount 必须大于 0");
        this.segmentsCount = segmentsCount;
        this.segments = new ConcurrentHashMap[segmentsCount];
        IntStream.range(0, segmentsCount).forEach(i -> segments[i] = new ConcurrentHashMap<>());
        expireExecutor = ExecutorFactory.newSingleScheduledExecutorService(SegmentedCache.class.getCanonicalName(), new NameThreadFactory(() -> "SegmentedCacheExpire"));
        expireExecutor.scheduleWithFixedDelay(() -> {
            for (ConcurrentHashMap<K, ExpiringValue> segment : segments) {
                long now = SystemClock.now();
                segment.entrySet().stream().filter(entry -> now > entry.getValue().getExpireTimestamp()).forEach(entry -> {
                    remove(entry.getKey());
                    Optional.ofNullable(entry.getValue().getWhenExpire()).ifPresent(Runnable::run);
                });
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    public abstract void put(K key, V value, long expireTimestamp, Runnable whenExpire);

    public void put(K key, ExpiringValue value) {
        Assert.notNull(key);
        Assert.notNull(value);
        int segmentIndex = Math.abs(key.hashCode() % segmentsCount);
        segments[segmentIndex].put(key, value);
    }

    public V get(K key) {
        Assert.notNull(key);
        int segmentIndex = Math.abs(key.hashCode() % segmentsCount);
        return Optional.ofNullable(segments[segmentIndex].get(key)).map(ExpiringValue::getValue).orElse(null);
    }

    public void remove(K key) {
        Assert.notNull(key);
        int segmentIndex = Math.abs(key.hashCode() % segmentsCount);
        segments[segmentIndex].remove(key);
    }

    public void clear() {
        IntStream.range(0, segmentsCount).forEach(i -> segments[i].clear());
    }

    public long count() {
        return Arrays.stream(segments).mapToLong(ConcurrentHashMap::size).sum();
    }

    public void shutdown() {
        clear();
        ThreadUtils.shutdownThreadPool(expireExecutor);
    }

    @Data
    @AllArgsConstructor
    public class ExpiringValue {
        protected final V value;
        protected final long expireTimestamp;
        protected final Runnable whenExpire;
    }
}