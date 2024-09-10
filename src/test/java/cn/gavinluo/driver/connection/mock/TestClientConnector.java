package cn.gavinluo.driver.connection.mock;

import cn.gavinluo.driver.connection.AbstractTagsDataReporter;
import cn.gavinluo.driver.connection.Connector;
import cn.gavinluo.driver.connection.StatusCode;
import cn.gavinluo.driver.connection.TcpClientConnector;
import cn.gavinluo.driver.connection.message.ExampleRequestMessage;
import cn.gavinluo.driver.connection.message.ExampleResponseMessage;
import cn.gavinluo.driver.connection.model.*;
import cn.gavinluo.driver.utils.JsonUtil;
import cn.gavinluo.driver.utils.tcp.TcpClientConfig;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TestClientConnector {

    public static void main(String[] args) throws Exception {
        String utf8String = FileUtil.readUtf8String(System.getProperty("user.dir") + "/ClientConnectorConnectionTagsMap.json");
        List<ConnectionTags> connectionTagsList = JsonUtil.parseArray(utf8String, ConnectionTags.class);
        utf8String = null;
        Map<String, Connector> connectors = new HashMap<>();
        AbstractTagsDataReporter tagsDataReporter = new AbstractTagsDataReporter() {
            @Override
            public void reportAction(List<TagData> results) {
                long count = results.stream().filter(x -> x.getQ() == StatusCode.OK).count();
                long errorCount = results.size() - count;
//                log.info("上报平台 success={} failed={}", count, errorCount);
            }
        };
        for (ConnectionTags entry : connectionTagsList) {
            Connection connection = entry.getConnection();
            List<Tag> tags = entry.getTags();
            TcpClientConnector tcpClientConnector = new TcpClientConnector(connection, tags, tagsDataReporter);
            TcpClientConfig<ExampleRequestMessage, ExampleResponseMessage> config = tcpClientConnector.getConfig();
            Connector connector = tcpClientConnector.start();
            connectors.put(connection.getConnectionId(), connector);
        }
        log.info("启动tcp客户端采集器数量:{}", connectors.size());
        //模拟写请求
        while (true) {
            Thread.sleep(15000);
            for (Map.Entry<Connection, List<Tag>> entry : MockConnectionData.connectionTagsMap.entrySet()) {
                List<Tag> tags = entry.getValue();
                Connection connection = entry.getKey();
                List<TagWrite> tagWrites = tags.stream().map(tag -> new TagWrite(tag, RandomUtil.randomInt())).collect(Collectors.toList());
                try {
                    List<TagData> tagData = connectors.get(connection.getConnectionId()).tagWrite(tagWrites).get();
                    log.info("写入结果 {}", tagData.size());
                } catch (Exception ex) {
                    log.error("写入异常", ex);
                }
            }
        }
    }
}
