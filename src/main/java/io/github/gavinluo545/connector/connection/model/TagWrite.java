package io.github.gavinluo545.connector.connection.model;

import lombok.Data;

import java.io.Serializable;

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
