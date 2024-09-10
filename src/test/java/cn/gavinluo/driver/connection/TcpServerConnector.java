package cn.gavinluo.driver.connection;

import cn.gavinluo.driver.connection.impl.AbstractTcpServerConnector;
import cn.gavinluo.driver.connection.impl.HeartbeatConfig;
import cn.gavinluo.driver.connection.message.ExampleHeader;
import cn.gavinluo.driver.connection.message.ExampleRequestMessage;
import cn.gavinluo.driver.connection.message.ExampleResponseMessage;
import cn.gavinluo.driver.connection.message.ExampleResponsePrincipal;
import cn.gavinluo.driver.connection.model.Connection;
import cn.gavinluo.driver.connection.model.Tag;
import cn.gavinluo.driver.connection.model.TagData;
import cn.gavinluo.driver.connection.model.TagWrite;
import cn.gavinluo.driver.utils.JsonUtil;
import cn.gavinluo.driver.utils.tcp.TcpServerConfig;
import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class TcpServerConnector extends AbstractTcpServerConnector<ExampleRequestMessage, ExampleResponseMessage> {

    private final AtomicInteger reqId = new AtomicInteger(0);
    private final Map<Integer, Map<Integer, Tag>> fastTagMap;

    public TcpServerConnector(Connection connection, List<Tag> tags, AbstractTagsDataReporter tagsDataReporter) {
        super(connection, tags, tagsDataReporter);
        fastTagMap = new ConcurrentHashMap<>();
        tags.forEach(tag -> {
            int deviceKey = Integer.parseInt(tag.getAttributes().get("deviceKey").toString());
            int ioa = Integer.parseInt(tag.getAttributes().get("ioa").toString());
            fastTagMap.computeIfAbsent(deviceKey, k -> new ConcurrentHashMap<>()).put(ioa, tag);
        });
        connection.setAutoRecovery(false);
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
    public boolean hasMessageId() {
        return true;
    }

    @Override
    public TcpServer newTcp(TcpServerConfig<ExampleRequestMessage, ExampleResponseMessage> config) {
        return new TcpServer(config);
    }

    @Override
    public Integer getDeviceKeyFormReceiveMessage(ExampleResponseMessage message) {
        return ((ExampleHeader) message.getHeader()).getDeviceId();
    }

    public Integer getDeviceKeyFormTag(Tag tag) {
        return (Integer) tag.getAttributes().get("deviceKey");
    }

    @Override
    public void unknownMessageReceiveProcess(Serializable deviceKey, ExampleResponseMessage message) {
        if (((ExampleResponsePrincipal) message.getPrincipal()).getOperation() == 0x02) {
            //心跳包
            return;
        }
        //请求后超时响应的  主动上报的
        List<TagData> tagDatas = new ArrayList<>();
        Map<Integer, Integer> ioaValues = ((ExampleResponsePrincipal) message.getPrincipal()).getIoas();
        long millis = System.currentTimeMillis();
        Integer deviceId = ((ExampleHeader) message.getHeader()).getDeviceId();
        ioaValues.forEach((ioa, value) -> {
            Tag tag = getTag(deviceId, ioa);
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
    public List<TagData> readResponseProcess(ExampleResponseMessage responseMessage) {
        List<TagData> tagDatas = new ArrayList<>();
        Map<Integer, Integer> ioaValues = ((ExampleResponsePrincipal) responseMessage.getPrincipal()).getIoas();
        long millis = System.currentTimeMillis();
        ioaValues.forEach((ioa, value) -> {
            Integer deviceId = ((ExampleHeader) responseMessage.getHeader()).getDeviceId();
            Tag tag = getTag(deviceId, ioa);
            if (tag != null) {
                tagDatas.add(TagData.readResponse(tag.getTagId(), value, StatusCode.OK, millis));
            }
        });
        return tagDatas;
    }

    @Override
    public List<TagData> writeReponseProcess(ExampleResponseMessage responseMessage) {
        List<TagData> tagDatas = new ArrayList<>();
        Map<Integer, Integer> ioaValues = ((ExampleResponsePrincipal) responseMessage.getPrincipal()).getIoas();
        ioaValues.forEach((ioa, value) -> {
            Integer deviceId = ((ExampleHeader) responseMessage.getHeader()).getDeviceId();
            Tag tag = getTag(deviceId, ioa);
            if (tag != null) {
                tagDatas.add(TagData.writeResponse(tag.getTagId(), value == 0 ? StatusCode.OK : StatusCode.ERR));
            }
        });
        return tagDatas;
    }

    @Override
    public HeartbeatConfig<ExampleRequestMessage> getHeartbeatConfig() {
        return new HeartbeatConfig<>(false, 30, channel -> new ExampleRequestMessage(0, (Integer) getDeviceKeyByChannel(channel.id().asShortText()), 0x02, new ConcurrentHashMap<>()));
    }

    @Override
    public Map<ChannelWrap, List<ExampleRequestMessage>> getChannelsForWrite(List<TagWrite> tagWrites) {
        Map<Integer, List<TagWrite>> deviceKeyTags = tagWrites.stream().collect(Collectors.groupingBy(tagWrite -> getDeviceKeyFormTag(tagWrite.getTag())));
        Map<ChannelWrap, List<ExampleRequestMessage>> groups = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<TagWrite>> entry : deviceKeyTags.entrySet()) {
            List<TagWrite> writeList = entry.getValue();
            List<ExampleRequestMessage> requestMessages = ListUtil.partition(writeList, 1000).stream().map(partition -> buildRequestForWirte(entry.getKey(), partition)).collect(Collectors.toList());
            Optional.ofNullable(getChannel(entry.getKey())).ifPresent(channelWrap -> groups.put(channelWrap, requestMessages));
        }
        return groups;
    }

    public ExampleRequestMessage buildRequestForWirte(Serializable deviceKey, List<TagWrite> tagWrites) {
        ConcurrentHashMap<Integer, Integer> ioas = tagWrites.stream().collect(Collectors.toMap(tagWrite -> (Integer) tagWrite.getTag().getAttributes().get("ioa"), tagWrite -> (Integer) tagWrite.getValue(), (a, b) -> b, () -> new ConcurrentHashMap<>(tagWrites.size())));
        return new ExampleRequestMessage(reqId.incrementAndGet() % 65536, (Integer) deviceKey, 1, ioas);
    }

    @Override
    public Map<ChannelWrap, List<ExampleRequestMessage>> getChannelsForRead(List<Tag> tags) {
        Map<Integer, List<Tag>> deviceKeyTags = tags.stream().collect(Collectors.groupingBy(this::getDeviceKeyFormTag));
        Map<ChannelWrap, List<ExampleRequestMessage>> groups = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Tag>> entry : deviceKeyTags.entrySet()) {
            List<Tag> writeList = entry.getValue();
            List<ExampleRequestMessage> requestMessages = ListUtil.partition(writeList, 1000).stream().map(partition -> buildRequestForRead(entry.getKey(), partition)).collect(Collectors.toCollection(LinkedList::new));
            Optional.ofNullable(getChannel(entry.getKey())).ifPresent(channelWrap -> groups.put(channelWrap, requestMessages));
        }
        return groups;
    }

    public ExampleRequestMessage buildRequestForRead(Serializable deviceKey, List<Tag> tags) {
        ConcurrentHashMap<Integer, Integer> ioas = tags.stream().collect(Collectors.toMap(tag -> (Integer) tag.getAttributes().get("ioa"), tag -> 0, (a, b) -> b, () -> new ConcurrentHashMap<>(tags.size())));
        return new ExampleRequestMessage(reqId.incrementAndGet() % 65536, (Integer) deviceKey, 0, ioas);
    }

    private Tag getTag(Integer deviceKey, Integer ioa) {
        Map<Integer, Tag> map = fastTagMap.get(deviceKey);
        if (map != null) {
            return map.get(ioa);
        }
        return null;
    }

}