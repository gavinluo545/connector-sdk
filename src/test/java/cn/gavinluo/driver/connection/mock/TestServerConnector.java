package cn.gavinluo.driver.connection.mock;

import cn.gavinluo.driver.connection.AbstractTagsDataReporter;
import cn.gavinluo.driver.connection.Connector;
import cn.gavinluo.driver.connection.StatusCode;
import cn.gavinluo.driver.connection.TcpServerConnector;
import cn.gavinluo.driver.connection.message.ExampleRequestMessage;
import cn.gavinluo.driver.connection.message.ExampleResponseMessage;
import cn.gavinluo.driver.connection.model.*;
import cn.gavinluo.driver.utils.JsonUtil;
import cn.gavinluo.driver.utils.tcp.TcpServerConfig;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TestServerConnector {

    public static void main(String[] args) throws Exception {
        String utf8String = FileUtil.readUtf8String(System.getProperty("user.dir") + "/ServerConnectorConnectionTagsMap.json");
        List<ConnectionTags> connectionTagsList = JsonUtil.parseArray(utf8String, ConnectionTags.class);
        utf8String = null;
        List<Connector> connectors = new ArrayList<>();
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
            TcpServerConnector tcpServerConnector = new TcpServerConnector(connection, tags, tagsDataReporter);
            TcpServerConfig<ExampleRequestMessage, ExampleResponseMessage> config = tcpServerConnector.getConfig();
            Connector connector = tcpServerConnector.start();
            Thread.sleep(500);
            connectors.add(connector);
        }
        log.info("启动tcp服务端采集器数量:{}", connectors.size());
        //模拟写请求
        while (true) {
            Thread.sleep(15000);
            long sum = connectors.stream().mapToLong(x -> ((TcpServerConnector) x).getDeviceKeyToChannelMap().values().stream().filter(c -> c.getChannel().isActive()).count()).sum();
            log.error("客户端在线数量:{}", sum);
            for (Map.Entry<Connection, List<Tag>> entry : MockConnectionData.connectionTagsMap.entrySet()) {
                Connection connection = entry.getKey();
                List<Tag> tags = entry.getValue();
                int start = RandomUtil.randomInt(0, tags.size());
                int end = RandomUtil.randomInt(start, tags.size());
                if ((end - start) > 100) {
                    end = end - 100;
                }
                List<Tag> writeTags = ListUtil.sub(tags, start, end);
                List<TagWrite> tagWrites = writeTags.stream().map(tag -> new TagWrite(tag, RandomUtil.randomInt())).collect(Collectors.toList());
                try {
                    String connectionId = connection.getConnectionId();
                    List<TagData> tagData = connectors.stream().filter(x -> x.getConnection().getConnectionId().equals(connectionId)).findAny().get()
                            .tagWrite(tagWrites).get();
                    long count = tagData.stream().filter(x -> x.getQ() == StatusCode.OK).count();
                    long errorCount = tagData.size() - count;
                    if (errorCount > 0) {
                        log.info("写入结果 connectionId={} results success={} failed={}", connectionId, count, errorCount);
                    }
                } catch (Exception ex) {
                    log.error("写入异常", ex);
                }
            }
        }
    }

}
