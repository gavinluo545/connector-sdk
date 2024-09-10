package io.github.gavinluo545.connector.connection.mock;

import io.github.gavinluo545.connector.connection.impl.HeartbeatConfig;
import io.github.gavinluo545.connector.connection.message.ExampleRequestMessage;
import io.github.gavinluo545.connector.connection.message.ExampleResponseMessage;
import io.github.gavinluo545.connector.connection.model.ConnectionTags;
import io.github.gavinluo545.connector.utils.JsonUtil;
import io.github.gavinluo545.connector.utils.tcp.TcpServerConfig;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class StartServer {

    public static void main(String[] args) throws InterruptedException {
        String utf8String = FileUtil.readUtf8String(System.getProperty("user.dir") + "/ClientConnectorConnectionTagsMap.json");
        List<ConnectionTags> connectionTagsList = JsonUtil.parseArray(utf8String, ConnectionTags.class);
        utf8String = null;
        Map<Integer, MockTcpServer> tcpServers = new HashMap<>();
        Map<Object, List<ConnectionTags>> servers = connectionTagsList.stream().collect(Collectors.groupingBy(connectionTags -> connectionTags.getConnection().getConnectionParamsMap().get("port")));
        for (Map.Entry<Object, List<ConnectionTags>> entry : servers.entrySet()) {
            int port = Integer.parseInt(entry.getKey().toString());
            Object deviceKey = entry.getValue().get(0).getConnection().getConnectionParamsMap().get("deviceKey");
            TcpServerConfig<ExampleResponseMessage, ExampleRequestMessage> config = new TcpServerConfig<>("127.0.0.1", port,
                    MockConnectionData.hasMessageId, MockConnectionData.parcelRequest,
                    (channelId, integerFrameMessage) -> Objects.hash(channelId, integerFrameMessage.getMessageId()));
            config.setChannelActiveListener(channel -> log.info("客户端上线 channelId={}", channel.id().asShortText()));
            config.setChannelInActiveListener(channel -> log.info("客户端下线 channelId={}", channel.id().asShortText()));
            config.setExceptionCaughtListener((channel, throwable) -> log.error("异常 channelId={}", channel.id().asShortText(), throwable));
            HeartbeatConfig<ExampleResponseMessage> heartbeatConfig = new HeartbeatConfig<>(true, 30,
                    channel -> new ExampleResponseMessage(0, Integer.parseInt(deviceKey.toString()), 0x02, new ConcurrentHashMap<>()));
            config.setEnableHeartbeat(heartbeatConfig.isEnableHeartbeat());
            config.setHeartbeatIntervalSeconds(heartbeatConfig.getHeartbeatIntervalSeconds());
            config.setHeartbeatMessageFunc(heartbeatConfig.getHeartbeatMessageFunc());
            MockTcpServer tcpServer = new MockTcpServer(config);
            tcpServer.bootstrap();
            tcpServers.put(port, tcpServer);
        }
        log.info("启动tcp服务端数量:{}", tcpServers.size());
        while (true) {
            Thread.sleep(15000);
        }
    }

}
