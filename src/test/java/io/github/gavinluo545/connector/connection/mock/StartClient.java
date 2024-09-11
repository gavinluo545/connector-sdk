package io.github.gavinluo545.connector.connection.mock;


import io.github.gavinluo545.connector.connection.impl.HeartbeatConfig;
import io.github.gavinluo545.connector.connection.message.ExampleRequestMessage;
import io.github.gavinluo545.connector.connection.message.ExampleResponseMessage;
import io.github.gavinluo545.connector.connection.model.Connection;
import io.github.gavinluo545.connector.connection.model.ConnectionTags;
import io.github.gavinluo545.connector.connection.model.Tag;
import io.github.gavinluo545.connector.utils.JsonUtil;
import io.github.gavinluo545.connector.utils.tcp.TcpClientConfig;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class StartClient {
    public static void main(String[] args) throws InterruptedException {
        String utf8String = FileUtil.readUtf8String(System.getProperty("user.dir") + "/ServerConnectorConnectionTagsMap.json");
        List<ConnectionTags> connectionTagsList = JsonUtil.parseArray(utf8String, ConnectionTags.class);
        utf8String = null;
        AtomicInteger count = new AtomicInteger(0);
        for (ConnectionTags entry : connectionTagsList) {
            Connection connection = entry.getConnection();
            List<Tag> tags = entry.getTags();
            Map<Object, List<Tag>> deviceKeyTags = tags.stream().collect(Collectors.groupingBy(tag -> tag.getAttributes().get("deviceKey")));
            int port = Integer.parseInt(connection.getConnectionParamsMap().get("port").toString());
            deviceKeyTags.forEach((deviceKey, tagList) -> {
                TcpClientConfig<ExampleResponseMessage, ExampleRequestMessage> config = new TcpClientConfig<>("127.0.0.1", port,
                        (channelId, integerFrameMessage) -> Objects.hash(channelId, integerFrameMessage.getMessageId()));
                HeartbeatConfig<ExampleResponseMessage> heartbeatConfig = new HeartbeatConfig<>(true, 30,
                        channel -> new ExampleResponseMessage(0, Integer.parseInt(deviceKey.toString()), 0x02, new ConcurrentHashMap<>()));
                config.setEnableHeartbeat(heartbeatConfig.isEnableHeartbeat());
                config.setHeartbeatIntervalSeconds(heartbeatConfig.getHeartbeatIntervalSeconds());
                config.setHeartbeatMessageFunc(heartbeatConfig.getHeartbeatMessageFunc());
                MockTcpClient mockTcpClient = new MockTcpClient(config);
                mockTcpClient.bootstrap();
                count.incrementAndGet();
            });
        }
        log.info("启动tcp客户端数量:{}", count.get());
        while (true) {
            Thread.sleep(15000);
        }
    }
}
