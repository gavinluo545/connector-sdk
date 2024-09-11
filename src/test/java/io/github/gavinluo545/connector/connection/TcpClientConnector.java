package io.github.gavinluo545.connector.connection;

import io.github.gavinluo545.connector.connection.impl.AbstractTcpClientConnector;
import io.github.gavinluo545.connector.connection.impl.HeartbeatConfig;
import io.github.gavinluo545.connector.connection.message.ExampleRequestMessage;
import io.github.gavinluo545.connector.connection.message.ExampleResponseMessage;
import io.github.gavinluo545.connector.connection.message.ExampleResponsePrincipal;
import io.github.gavinluo545.connector.connection.model.Connection;
import io.github.gavinluo545.connector.connection.model.Tag;
import io.github.gavinluo545.connector.connection.model.TagData;
import io.github.gavinluo545.connector.connection.model.TagWrite;
import io.github.gavinluo545.connector.utils.JsonUtil;
import io.github.gavinluo545.connector.utils.tcp.TcpClientConfig;
import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class TcpClientConnector extends AbstractTcpClientConnector<ExampleRequestMessage, ExampleResponseMessage> {
    private final AtomicInteger reqId = new AtomicInteger(0);
    private final Integer deviceKey;
    private final Map<Integer, Tag> fastTagMap;

    public TcpClientConnector(Connection connection, List<Tag> tags, AbstractTagsDataReporter tagsDataReporter) {
        super(connection, tags, tagsDataReporter);
        this.deviceKey = Integer.parseInt(connection.getConnectionParamsMap().get("deviceKey").toString());
        fastTagMap = tags.stream().collect(Collectors.toMap(tag -> Integer.parseInt(tag.getAttributes().get("ioa").toString()), t -> t));
    }

    @Override
    public String getIp() {
        ObjectNode objectNode = JsonUtil.parseObject(connection.getConnectionParams());
        return objectNode.get("ip").asText();
    }

    @Override
    public int getPort() {
        ObjectNode objectNode = JsonUtil.parseObject(connection.getConnectionParams());
        return objectNode.get("port").asInt();
    }


    @Override
    public TcpClient newTcp(TcpClientConfig<ExampleRequestMessage, ExampleResponseMessage> config) {
        return new TcpClient(config);
    }

    @Override
    public void unknownMessageReceiveProcess(ExampleResponseMessage message) {
        if (((ExampleResponsePrincipal) message.getPrincipal()).getOperation() == 0x02) {
            //心跳包
            return;
        }
        //请求后超时响应的  主动上报的
        List<TagData> tagDatas = new ArrayList<>();
        Map<Integer, Integer> ioaValues = ((ExampleResponsePrincipal) message.getPrincipal()).getIoas();
        long millis = System.currentTimeMillis();
        ioaValues.forEach((ioa, value) -> {
            Tag tag = getTag(ioa);
            if (tag != null) {
                tagDatas.add(TagData.readResponse(tag.getTagId(), value, StatusCode.OK, millis));
            }
        });
        if (!tagDatas.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("主动上报:connectionId={} deviceKey={} tagDatas={}", connection.getConnectionId(), deviceKey, tagDatas.size());
            }
            tagsDataReporter.report(tagDatas);
        }
    }

    @Override
    public List<ExampleRequestMessage> getChannelsForRead(List<Tag> tags) {
        List<List<Tag>> partition = ListUtil.partition(tags, 1000);
        return partition.stream().map(this::buildRequestForRead).collect(Collectors.toCollection(LinkedList::new));
    }

    public ExampleRequestMessage buildRequestForRead(List<Tag> tags) {
        ConcurrentHashMap<Integer, Integer> ioas = tags.stream().collect(Collectors.toMap(tag -> (Integer) (tag.getAttributes().get("ioa")), tag -> 0, (a, b) -> b, () -> new ConcurrentHashMap<>(tags.size())));
        return new ExampleRequestMessage(reqId.incrementAndGet() % 65536, deviceKey, 0, ioas);
    }

    @Override
    public List<TagData> readResponseProcess(ExampleResponseMessage responseMessage) {
        List<TagData> tagDatas = new ArrayList<>();
        Map<Integer, Integer> ioaValues = ((ExampleResponsePrincipal) responseMessage.getPrincipal()).getIoas();
        long millis = System.currentTimeMillis();
        ioaValues.forEach((ioa, value) -> {
            Tag tag = getTag(ioa);
            if (tag != null) {
                tagDatas.add(TagData.readResponse(tag.getTagId(), value, StatusCode.OK, millis));
            }
        });
        return tagDatas;
    }

    @Override
    public List<ExampleRequestMessage> getChannelsForWrite(List<TagWrite> tagWrites) {
        List<List<TagWrite>> partition = ListUtil.partition(tagWrites, 1000);
        return partition.stream().map(this::buildRequestForWirte).collect(Collectors.toCollection(LinkedList::new));
    }

    public ExampleRequestMessage buildRequestForWirte(List<TagWrite> tagWrites) {
        ConcurrentHashMap<Integer, Integer> ioas = tagWrites.stream().collect(Collectors.toMap(tagWrite -> (Integer) tagWrite.getTag().getAttributes().get("ioa"), tagWrite -> (Integer) tagWrite.getValue(), (a, b) -> b, () -> new ConcurrentHashMap<>(tagWrites.size())));
        return new ExampleRequestMessage(reqId.incrementAndGet() % 65536, deviceKey, 1, ioas);
    }

    @Override
    public List<TagData> writeReponseProcess(ExampleResponseMessage responseMessage) {
        List<TagData> tagDatas = new ArrayList<>();
        Map<Integer, Integer> ioaValues = ((ExampleResponsePrincipal) responseMessage.getPrincipal()).getIoas();
        ioaValues.forEach((ioa, value) -> {
            Tag tag = getTag(ioa);
            if (tag != null) {
                tagDatas.add(TagData.writeResponse(tag.getTagId(), value == 0 ? StatusCode.OK : StatusCode.ERR));
            }
        });
        return tagDatas;
    }

    @Override
    public HeartbeatConfig<ExampleRequestMessage> getHeartbeatConfig() {
        return new HeartbeatConfig<>(false, 30, channel -> new ExampleRequestMessage(0, deviceKey, 0x02, new ConcurrentHashMap<>()));
    }

    private Tag getTag(Integer ioa) {
        return fastTagMap.get(ioa);
    }
}
