package cn.gavinluo.driver.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.List;

/**
 * JSON 工具类，使用 Jackson 处理 JSON 数据。
 * <p>
 * 该类提供了解析 JSON、将对象转为 JSON 字符串等功能，使用了 Jackson 的 ObjectMapper。
 *
 * @author gavinluo7@foxmail.com
 */
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

    /**
     * 将 JSON 数组字符串解析为 List 对象。
     *
     * @param text JSON 数组字符串
     * @param type List 中元素的类型
     * @param <T>  泛型类型
     * @return 解析得到的 List 对象
     */
    public static <T> List<T> parseArray(String text, Class<T> type) {
        try {
            return MAPPER.readValue(text, MAPPER.getTypeFactory().constructCollectionType(List.class, type));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将对象转为 JSON 字符串。
     *
     * @param object 要转换的对象
     * @return 对象的 JSON 字符串表示
     */
    public static String toJSONString(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字节数组解析为 Jackson 的 ArrayNode。
     *
     * @param bytes JSON 字节数组
     * @return 解析得到的 ArrayNode
     */
    public static ArrayNode parseArray(byte[] bytes) {
        try {
            return (ArrayNode) MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串解析为 Jackson 的 ArrayNode。
     *
     * @param json JSON 字符串
     * @return 解析得到的 ArrayNode
     */
    public static ArrayNode parseArray(String json) {
        try {
            return (ArrayNode) MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串解析为 Jackson 的 ObjectNode。
     *
     * @param json JSON 字符串
     * @return 解析得到的 ObjectNode
     */
    public static ObjectNode parseObject(String json) {
        try {
            return (ObjectNode) MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字节数组解析为 Jackson 的 ObjectNode。
     *
     * @param bytes JSON 字节数组
     * @return 解析得到的 ObjectNode
     */
    public static ObjectNode parseObject(byte[] bytes) {
        try {
            return (ObjectNode) MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字节数组解析为指定类型的对象。
     *
     * @param str         JSON 字节数组
     * @param objectClass 目标对象的类型
     * @param <T>         泛型类型
     * @return 解析得到的对象
     */
    public static <T> T parseObject(byte[] str, Class<T> objectClass) {
        try {
            return MAPPER.readValue(str, objectClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串解析为指定类型的对象。
     *
     * @param str         JSON 字符串
     * @param objectClass 目标对象的类型
     * @param <T>         泛型类型
     * @return 解析得到的对象
     */
    public static <T> T parseObject(String str, Class<T> objectClass) {
        try {
            return MAPPER.readValue(str, objectClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将对象转为 JSON 字节数组。
     *
     * @param object 要转换的对象
     * @return 对象的 JSON 字节数组表示
     */
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


