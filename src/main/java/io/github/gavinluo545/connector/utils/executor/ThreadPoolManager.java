package io.github.gavinluo545.connector.utils.executor;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ThreadPoolManager
 *
 * @author gavinluo545@gmail.com
 */
@Slf4j
public final class ThreadPoolManager {

    private Map<String, Map<String, Set<ExecutorService>>> resourcesManager;

    private final Map<String, Object> lockers = new ConcurrentHashMap<>(8);

    private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();

    private static final AtomicBoolean CLOSED = new AtomicBoolean(false);

    static {
        INSTANCE.init();
        ThreadUtils.addShutdownHook(ThreadPoolManager::shutdown);
    }

    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }

    private ThreadPoolManager() {
    }

    private void init() {
        resourcesManager = new ConcurrentHashMap<>(8);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> log.error("线程 {} 未捕获未知异常", t.getName(), e));
    }

    /**
     * Register the thread pool resources with the resource manager.
     *
     * @param namespace namespace name
     * @param group     group name
     * @param executor  {@link ExecutorService}
     */
    public void register(String namespace, String group, ExecutorService executor) {
        if (!resourcesManager.containsKey(namespace)) {
            synchronized (this) {
                lockers.put(namespace, new Object());
            }
        }
        final Object monitor = lockers.get(namespace);
        synchronized (monitor) {
            Map<String, Set<ExecutorService>> map = resourcesManager.get(namespace);
            if (map == null) {
                map = new HashMap<>(8);
                map.put(group, new HashSet<>());
                map.get(group).add(executor);
                resourcesManager.put(namespace, map);
                return;
            }
            if (!map.containsKey(group)) {
                map.put(group, new HashSet<>());
            }
            map.get(group).add(executor);
        }
    }

    /**
     * Cancel the uniform lifecycle management for all threads under this resource.
     *
     * @param namespace namespace name
     * @param group     group name
     */
    public void deregister(String namespace, String group) {
        if (resourcesManager.containsKey(namespace)) {
            final Object monitor = lockers.get(namespace);
            synchronized (monitor) {
                resourcesManager.get(namespace).remove(group);
            }
        }
    }

    /**
     * Undoing the uniform lifecycle management of {@link ExecutorService} under this resource.
     *
     * @param namespace namespace name
     * @param group     group name
     * @param executor  {@link ExecutorService}
     */
    public void deregister(String namespace, String group, ExecutorService executor) {
        if (resourcesManager.containsKey(namespace)) {
            final Object monitor = lockers.get(namespace);
            synchronized (monitor) {
                final Map<String, Set<ExecutorService>> subResourceMap = resourcesManager.get(namespace);
                if (subResourceMap.containsKey(group)) {
                    subResourceMap.get(group).remove(executor);
                }
            }
        }
    }

    /**
     * Destroys all thread pool resources under this namespace.
     *
     * @param namespace namespace
     */
    public void destroy(final String namespace) {
        final Object monitor = lockers.get(namespace);
        if (monitor == null) {
            return;
        }
        synchronized (monitor) {
            Map<String, Set<ExecutorService>> subResource = resourcesManager.get(namespace);
            if (subResource == null) {
                return;
            }
            for (Map.Entry<String, Set<ExecutorService>> entry : subResource.entrySet()) {
                for (ExecutorService executor : entry.getValue()) {
                    ThreadUtils.shutdownThreadPool(executor);
                }
            }
            resourcesManager.get(namespace).clear();
            resourcesManager.remove(namespace);
        }
    }

    /**
     * This namespace destroys all thread pool resources under the grouping.
     *
     * @param namespace namespace
     * @param group     group
     */
    public void destroy(final String namespace, final String group) {
        final Object monitor = lockers.get(namespace);
        if (monitor == null) {
            return;
        }
        synchronized (monitor) {
            Map<String, Set<ExecutorService>> subResource = resourcesManager.get(namespace);
            if (subResource == null) {
                return;
            }
            Set<ExecutorService> waitDestroy = subResource.get(group);
            for (ExecutorService executor : waitDestroy) {
                ThreadUtils.shutdownThreadPool(executor);
            }
            resourcesManager.get(namespace).remove(group);
        }
    }

    /**
     * Shutdown thread pool manager.
     */
    public static void shutdown() {
        if (!CLOSED.compareAndSet(false, true)) {
            return;
        }
        Set<String> namespaces = INSTANCE.resourcesManager.keySet();
        namespaces.forEach(INSTANCE::destroy);
    }

}
