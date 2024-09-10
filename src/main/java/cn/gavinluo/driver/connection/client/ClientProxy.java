package cn.gavinluo.driver.connection.client;

import cn.gavinluo.driver.connection.model.Connection;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端代理抽象类，用于管理和控制客户端连接的生命周期。
 * 泛型参数 T 表示客户端对象的类型。
 *
 * @author gavinluo7@foxmail.com
 */
@Slf4j
public abstract class ClientProxy<T> {

    /**
     * 启动客户端连接。
     *
     * @param connection 要连接的 Connection 对象。
     */
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

    /**
     * 停止客户端连接。
     *
     * @param connectionId 要停止的连接的 ID。
     */
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

    /**
     * 创建客户端对象。
     *
     * @param connection 要连接的 Connection 对象。
     * @return 创建的客户端对象。
     * @throws Exception 如果创建过程中发生异常。
     */
    public abstract T create(Connection connection) throws Exception;

    /**
     * 从管理器中选择客户端对象。
     *
     * @param connectionId 连接的 ID。
     * @return 选中的客户端对象。
     */
    @SuppressWarnings("unchecked")
    public T select(String connectionId) {
        return (T) ClientManager.retrieve(connectionId);
    }

    /**
     * 连接客户端。
     *
     * @param client 要连接的客户端对象。
     * @throws Exception 如果连接过程中发生异常。
     */
    public abstract void connect(T client) throws Exception;

    /**
     * 断开客户端连接。
     *
     * @param client 要断开连接的客户端对象。
     * @throws Exception 如果断开连接过程中发生异常。
     */
    public abstract void disconnect(T client) throws Exception;

    /**
     * 检查客户端是否已连接。
     *
     * @param client 要检查的客户端对象。
     * @return 如果客户端已连接，则返回 true；否则返回 false。
     */
    public abstract boolean isConnected(T client);
}
