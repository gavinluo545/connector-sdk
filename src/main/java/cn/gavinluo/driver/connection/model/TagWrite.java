package cn.gavinluo.driver.connection.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 表示写操作的请求数据对象。
 * 用于在写操作中携带点位信息及待写入的值。
 *
 * @author gavinluo7@foxmail.com
 */
@Data
public class TagWrite implements Serializable {
    private static final long serialVersionUID = 879759007809448630L;

    /**
     * 点位信息
     */
    private final Tag tag;

    /**
     * 待写入的值
     */
    private final Object value;

    public TagWrite(Tag tag, Object value) {
        this.tag = tag;
        this.value = value;
    }

}
