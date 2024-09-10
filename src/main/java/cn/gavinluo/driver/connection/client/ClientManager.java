package cn.gavinluo.driver.connection.client;

import lombok.experimental.UtilityClass;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端管理器，用于注册、注销和检索客户端对象。
 *
 * @author gavinluo7@foxmail.com
 */
@UtilityClass
public class ClientManager {
    /**
     * 客户端对象映射，使用线程安全的 ConcurrentHashMap 进行存储。
     * 键为连接的唯一标识（通常是连接ID），值为客户端对象。
     */
    private final static Map<Serializable, Object> CLIENT_MAP = new ConcurrentHashMap<>(100, 0.75f, 32);

    /**
     * 列出所有客户端对象的映射。
     *
     * @return 包含所有客户端对象的映射。
     */
    public static Map<Serializable, Object> listAll() {
        return CLIENT_MAP;
    }

    /**
     * 注册客户端对象。
     *
     * @param key    连接的唯一标识（通常是连接ID）。
     * @param client 要注册的客户端对象。
     */
    public static void register(Serializable key, Object client) {
        CLIENT_MAP.put(key, client);
    }

    /**
     * 注销客户端对象。
     *
     * @param key 连接的唯一标识（通常是连接ID）。
     */
    public static void unregister(Serializable key) {
        CLIENT_MAP.remove(key);
    }

    /**
     * 根据连接的唯一标识（通常是连接ID）检索客户端对象。
     *
     * @param channelId 连接的唯一标识（通常是连接ID）。
     * @return 检索到的客户端对象，如果没有则返回 null。
     */
    public static Object retrieve(Serializable channelId) {
        return CLIENT_MAP.get(channelId);
    }
}
