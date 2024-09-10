package io.github.gavinluo545.connector.connection.model;

import io.github.gavinluo545.connector.connection.StatusCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class TagData implements Serializable {
    private static final long serialVersionUID = -7767829466383217523L;

    /**
     * 数据键
     */
    @JsonProperty("k")
    private String k;

    /**
     * 数据值
     */
    @JsonProperty("v")
    private Object v;

    /**
     * 操作结果代码
     */
    @JsonProperty("q")
    private StatusCode q;

    /**
     * 时间戳
     */
    @JsonProperty("t")
    private Long t;

    public TagData(String k, Object v, StatusCode q, Long t) {
        this.k = k;
        this.v = v;
        this.q = q;
        this.t = t;
    }

    /**
     * 创建写操作的响应数据对象。
     *
     * @param id   数据键
     * @param code 操作结果代码
     * @return 写操作的响应数据对象
     */
    public static TagData writeResponse(String id, StatusCode code) {
        return new TagData(id, null, code, null);
    }

    /**
     * 创建读操作的响应数据对象。
     *
     * @param id 数据键
     * @param v  数据值
     * @param q  操作结果代码
     * @param ts 时间戳
     * @return 读操作的响应数据对象
     */
    public static TagData readResponse(String id, Object v, StatusCode q, Long ts) {
        return new TagData(id, v, q, ts);
    }

}
