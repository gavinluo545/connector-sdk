package cn.gavinluo.driver.connection;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * StatusCode 是枚举类型，表示连接器中的状态码。
 * 每个枚举值包含一个短整型的状态码和对应的描述信息。
 *
 * @author gavinluo7@foxmail.com
 */
@Getter
@AllArgsConstructor
public enum StatusCode {

    /**
     * 表示状态码的枚举值
     */
    OK((short) 1, "好"),
    ERR((short) -1, "坏"),

    CONNECTION_NOT_FOUND((short) 11, "设备连接不存在"),
    CONNECTION_NOT_CONNECTED((short) 12, "设备连接未与设备建立连接"),
    CONNECTION_IS_LOCKED((short) 13, "设备连接正在做其他更新，请稍后再试"),

    TAG_NOT_FOUND((short) 21, "点位不存在"),
    TAG_WRITE_NOT_SUPPORT((short) 22, "点位不支持写"),
    TAG_READ_NOT_SUPPORT((short) 23, "点位不支持读"),
    ;

    /**
     * 状态码
     */
    @JsonValue
    private final Short code;

    /**
     * 状态码对应的描述信息
     */
    private final String message;

    public static final Map<Short, StatusCode> CODE_MAP = Arrays.stream(StatusCode.values()).collect(Collectors.toMap(StatusCode::getCode, Function.identity()));

    public static StatusCode lookup(int q) {
        return CODE_MAP.getOrDefault((short) q, StatusCode.ERR);
    }
}
