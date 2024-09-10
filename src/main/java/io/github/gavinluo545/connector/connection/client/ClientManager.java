package io.github.gavinluo545.connector.connection.client;

import lombok.experimental.UtilityClass;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class ClientManager {
    private final static Map<Serializable, Object> CLIENT_MAP = new ConcurrentHashMap<>(100, 0.75f, 32);

    public static Map<Serializable, Object> listAll() {
        return CLIENT_MAP;
    }

    public static void register(Serializable key, Object client) {
        CLIENT_MAP.put(key, client);
    }

    public static void unregister(Serializable key) {
        CLIENT_MAP.remove(key);
    }

    public static Object retrieve(Serializable channelId) {
        return CLIENT_MAP.get(channelId);
    }
}
