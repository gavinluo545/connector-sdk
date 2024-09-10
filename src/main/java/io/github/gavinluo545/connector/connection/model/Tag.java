package io.github.gavinluo545.connector.connection.model;

import cn.hutool.core.lang.Assert;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@Data
public class Tag implements Serializable {
    private static final long serialVersionUID = -3392606545106060643L;
    /**
     * 连接id
     */
    @JsonProperty("connectionId")
    private String connectionId;

    /**
     * 点位ID
     **/
    @JsonProperty("tagId")
    private String tagId;

    /**
     * 点位名称
     **/
    @JsonProperty("tagName")
    private String tagName;

    /**
     * 读写类型，0-R 1-W 2-RW
     **/
    @JsonProperty("rwType")
    private Integer rwType = 0;

    /**
     * 采集类型 0-周期采集 1-主动上报
     **/
    @JsonProperty("collectionType")
    private Integer collectionType = 0;

    /**
     * 采集周期，毫秒，主动上报时可以不填写
     **/
    @JsonProperty("collectionPeriod")
    private Integer collectionPeriod = 1000;

    /**
     * 点位属性
     */
    @JsonProperty("attributes")
    private Map<String, Object> attributes;

    public void setTagId(String tagId) {
        this.tagId = Assert.notBlank(tagId);
    }

    public void setTagName(String tagName) {
        this.tagName = Assert.notBlank(tagName);
    }

    public void setRwType(Integer rwType) {
        this.rwType = Assert.notNull(rwType);
    }

    public void setCollectionType(Integer collectionType) {
        this.collectionType = Assert.notNull(collectionType);
    }

    public void setCollectionPeriod(Integer collectionPeriod) {
        Assert.isTrue(collectionPeriod != null && collectionPeriod >= 100);
        this.collectionPeriod = collectionPeriod;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = Assert.notBlank(connectionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return Objects.equals(connectionId, tag.connectionId) && Objects.equals(tagId, tag.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId, tagId);
    }
}
