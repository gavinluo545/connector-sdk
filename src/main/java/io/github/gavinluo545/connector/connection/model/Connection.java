package io.github.gavinluo545.connector.connection.model;

import io.github.gavinluo545.connector.utils.JsonUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class Connection implements Serializable {
    private static final long serialVersionUID = 2002221383969848897L;

    /**
     * 设备连接ID
     **/
    @JsonProperty("connectionId")
    private String connectionId;

    /**
     * 设备连接名称
     **/
    @JsonProperty("connectionName")
    private String connectionName;

    /**
     * 采集超时时长，毫秒
     * 最小100
     **/
    @JsonProperty("collectionTimeout")
    private Integer collectionTimeout = 3000;

    /**
     * 帧间隔，毫秒
     * 最小100
     **/
    @JsonProperty("timeWait")
    private Integer timeWait = 500;

    /**
     * 异常恢复间隔(单位，秒) 默认是30秒
     * 最小1
     **/
    @JsonProperty("recoveryInterval")
    private Integer recoveryInterval = 60;

    /**
     * 最长恢复时间(单位，分钟)
     * 如果小于等于0 则认为一直尝试异常恢复
     **/
    @JsonProperty("maximumRecoveryTime")
    private Integer maximumRecoveryTime = 1440;

    /**
     * 设备连接类型
     **/
    @JsonProperty("connectionType")
    private String connectionType;

    /**
     * 驱动定义的协议参数
     **/
    @JsonProperty("connectionParams")
    private String connectionParams;

    /**
     * 是否断连自动重连标志
     **/
    @JsonProperty("autoRecovery")
    private Boolean autoRecovery;

    @JsonProperty("recoveryTimeout")
    private Integer recoveryTimeout = 6000;


    /**
     * 将connectionParams字符串解析为键值对的Map。
     * 如果connectionParams为空白，则返回一个空Map。
     *
     * @return 连接参数的Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConnectionParamsMap() {
        if (StrUtil.isNotBlank(connectionParams) && JsonUtil.isJsonObject(connectionParams)) {
            return (Map<String, Object>) JsonUtil.parseObject(connectionParams, Map.class);
        }
        return new HashMap<>(16);
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = Assert.notBlank(connectionId);
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = Assert.notBlank(connectionName);
    }

    public void setCollectionTimeout(Integer collectionTimeout) {
        Assert.isTrue(collectionTimeout != null && collectionTimeout >= 100);
        this.collectionTimeout = collectionTimeout;
    }

    public void setTimeWait(Integer timeWait) {
        Assert.isTrue(timeWait != null && timeWait >= 100);
        this.timeWait = Assert.notNull(timeWait);
    }

    public void setRecoveryInterval(Integer recoveryInterval) {
        Assert.isTrue(recoveryInterval != null && recoveryInterval >= 1);
        this.recoveryInterval = recoveryInterval;
    }

    public void setMaximumRecoveryTime(Integer maximumRecoveryTime) {
        this.maximumRecoveryTime = Assert.notNull(maximumRecoveryTime);
    }

    public void setAutoRecovery(Boolean autoRecovery) {
        this.autoRecovery = autoRecovery != null && autoRecovery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return Objects.equals(connectionId, that.connectionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId);
    }
}
