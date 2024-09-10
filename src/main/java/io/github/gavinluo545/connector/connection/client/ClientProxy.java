package io.github.gavinluo545.connector.connection.client;

import io.github.gavinluo545.connector.connection.model.Connection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ClientProxy<T> {

    public void start(Connection connection) throws Exception {
        String connectionId = connection.getConnectionId();
        T client = select(connectionId);
        if (client == null) {
            try {
                client = create(connection);
                ClientManager.register(connectionId, client);
                connect(client);
                log.info("启动成功: {}", client);
            } catch (Exception e) {
                log.error("启动失败: {}", connection, e);
                throw e;
            }
        } else {
            stop(connectionId);
            client = create(connection);
            ClientManager.register(connectionId, client);
            if (!isConnected(client)) {
                try {
                    connect(client);
                } catch (Exception e) {
                    log.error("重连成功: {}", connection, e);
                    throw e;
                }
            }
        }
    }

    public void stop(String connectionId) throws Exception {
        T client = select(connectionId);
        if (client != null) {
            try {
                if (isConnected(client)) {
                    disconnect(client);
                }
                ClientManager.unregister(connectionId);
                log.info("停止成功: {}", client);
            } catch (Exception e) {
                log.error("停止失败: {}", client, e);
                throw e;
            }
        }
    }

    public abstract T create(Connection connection) throws Exception;

    @SuppressWarnings("unchecked")
    public T select(String connectionId) {
        return (T) ClientManager.retrieve(connectionId);
    }

    public abstract void connect(T client) throws Exception;

    public abstract void disconnect(T client) throws Exception;

    public abstract boolean isConnected(T client);
}
