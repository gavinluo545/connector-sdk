package cn.gavinluo.driver.connection.mock;

import cn.gavinluo.driver.connection.model.Connection;
import cn.gavinluo.driver.connection.model.ConnectionTags;
import cn.gavinluo.driver.connection.model.Tag;
import cn.gavinluo.driver.utils.JsonUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MockConnectionData {
    public static int port = 2000;
    public static final boolean hasMessageId = true;
    public static final boolean parcelRequest = true;
    static final Map<Connection, List<Tag>> connectionTagsMap = new HashMap<>();
    public static int totalServerNum = 30;
    public static int totalClientNum = 30;
    public static int clientTagNum = 10000;

    public static void main(String[] args) {
        initServerData();
    }

    public static void initServerData() {
        int random = totalClientNum / totalServerNum;
        AtomicInteger totalClientNumAtomic = new AtomicInteger(0);
        for (int i = 0; i < totalServerNum; i++) {
            Connection connection = new Connection();
            connection.setConnectionId(IdUtil.nanoId());
            connection.setCollectionTimeout(5000);
            connection.setConnectionName(RandomUtil.randomString(4));
            connection.setAutoRecovery(true);
            Map<String, String> params = new HashMap<>();
            params.put("ip", "127.0.0.1");
            params.put("port", String.valueOf(port++));
            connection.setConnectionParams(JsonUtil.toJSONString(params));
            List<Tag> tags = new ArrayList<>();
            connectionTagsMap.put(connection, tags);
            int finalI = i;
            IntStream.range(0, random).forEach(n -> {
                int incrementedAndGet = totalClientNumAtomic.incrementAndGet();
                if (incrementedAndGet > totalClientNum) {
                    return;
                }
                IntStream.range(0, clientTagNum).forEach(x -> {
                    Tag tag = new Tag();
                    tag.setTagId(IdUtil.nanoId());
                    tag.setTagName(RandomUtil.randomString(4));
                    tag.setRwType(2);
                    tag.setConnectionId(connection.getConnectionId());
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("ioa", RandomUtil.randomInt(0, Integer.MAX_VALUE));
                    attributes.put("deviceKey", finalI * totalServerNum + n);
                    tag.setAttributes(attributes);
                    tags.add(tag);
                });
            });
        }
        List<ConnectionTags> connectionTagsList = new ArrayList<>();
        connectionTagsMap.forEach(((connection, tagList) -> {
            connectionTagsList.add(new ConnectionTags(connection, new CopyOnWriteArrayList<>(tagList)));
        }));
        int more = (totalClientNum % totalServerNum) * clientTagNum;
        IntStream.range(0, more).forEach(x -> {
            ConnectionTags connectionTags = connectionTagsList.get(RandomUtil.randomInt(0, connectionTagsList.size() - 1));
            Connection connection = connectionTags.getConnection();
            List<Tag> tags = connectionTags.getTags();
            List<Object> deviceKeys = tags.stream().map(tag -> tag.getAttributes().get("deviceKey")).collect(Collectors.toList());
            Tag tag = new Tag();
            tag.setTagId(IdUtil.nanoId());
            tag.setTagName(RandomUtil.randomString(4));
            tag.setRwType(2);
            tag.setConnectionId(connection.getConnectionId());
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("ioa", RandomUtil.randomInt(0, Integer.MAX_VALUE));
            attributes.put("deviceKey", deviceKeys.get(RandomUtil.randomInt(0, deviceKeys.size() - 1)));
            tag.setAttributes(attributes);
            tags.add(tag);
        });
        String jsonString = JsonUtil.toJSONString(connectionTagsList);
        FileUtil.writeUtf8String(jsonString, System.getProperty("user.dir") + "/ServerConnectorConnectionTagsMap.json");
        System.out.println("connection num:" + connectionTagsList.size() + " total tag num:" + connectionTagsList.stream().mapToInt(x -> x.getTags().size()).sum());
    }

    public static void initClientData() {
        int[] ports = IntStream.range(0, totalServerNum).map(i -> port++).toArray();
        for (int i = 0; i < totalClientNum; i++) {
            Connection connection = new Connection();
            connection.setConnectionId(IdUtil.nanoId());
            connection.setCollectionTimeout(5000);
            connection.setConnectionName(RandomUtil.randomString(4));
            connection.setAutoRecovery(true);
            Map<String, String> params = new HashMap<>();
            params.put("ip", "127.0.0.1");
            params.put("deviceKey", String.valueOf(i));
            params.put("port", String.valueOf(ports[i % totalServerNum]));
            connection.setConnectionParams(JsonUtil.toJSONString(params));
            List<Tag> tags = new ArrayList<>();
            int finalI = i;
            IntStream.range(0, clientTagNum).forEach(x -> {
                Tag tag = new Tag();
                tag.setTagId(IdUtil.nanoId());
                tag.setTagName(RandomUtil.randomString(4));
                tag.setRwType(2);
                tag.setConnectionId(connection.getConnectionId());
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("ioa", finalI * totalClientNum + x);
                tag.setAttributes(attributes);
                tags.add(tag);
            });
            connectionTagsMap.put(connection, tags);
        }
        List<ConnectionTags> connectionTagsList = new ArrayList<>();
        connectionTagsMap.forEach(((connection, tagList) -> {
            connectionTagsList.add(new ConnectionTags(connection, new CopyOnWriteArrayList<>(tagList)));
        }));
        String jsonString = JsonUtil.toJSONString(connectionTagsList);
        FileUtil.writeUtf8String(jsonString, System.getProperty("user.dir") + "/ClientConnectorConnectionTagsMap.json");
        System.out.println("connection num:" + connectionTagsList.size() + " total tag num:" + connectionTagsList.stream().mapToInt(x -> x.getTags().size()).sum());
    }

}
