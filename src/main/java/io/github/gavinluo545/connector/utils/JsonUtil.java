package io.github.gavinluo545.connector.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.List;

@UtilityClass
public class JsonUtil {

    private static final ObjectMapper MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 设置序列化时排除空值属性
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 配置在反序列化时忽略未知的属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static <T> List<T> parseArray(String text, Class<T> type) {
        try {
            return MAPPER.readValue(text, MAPPER.getTypeFactory().constructCollectionType(List.class, type));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJSONString(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArrayNode parseArray(byte[] bytes) {
        try {
            return (ArrayNode) MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArrayNode parseArray(String json) {
        try {
            return (ArrayNode) MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectNode parseObject(String json) {
        try {
            return (ObjectNode) MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectNode parseObject(byte[] bytes) {
        try {
            return (ObjectNode) MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(byte[] str, Class<T> objectClass) {
        try {
            return MAPPER.readValue(str, objectClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String str, Class<T> objectClass) {
        try {
            return MAPPER.readValue(str, objectClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toJSONBytes(Object object) {
        try {
            return MAPPER.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isJsonObject(String text) {
        return text.startsWith("{") && text.endsWith("}");
    }

    public static boolean isJsonArray(String text) {
        return text.startsWith("[") && text.endsWith("]");
    }
}


