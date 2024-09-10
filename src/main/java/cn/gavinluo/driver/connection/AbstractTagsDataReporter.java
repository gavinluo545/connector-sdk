package cn.gavinluo.driver.connection;

import cn.gavinluo.driver.connection.model.TagData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 定义点位数据上报的接口。
 * 用于将点位数据结果报告给外部系统。
 * 通过实现该接口，可以将点位数据推送至外部系统。
 *
 * @author gavinluo7@foxmail.com
 */
@Slf4j
public abstract class AbstractTagsDataReporter {

    /**
     * 报告实时点位数据结果。
     *
     * @param results 点位数据结果列表
     */
    public void report(List<TagData> results) {
        try {
            reportAction(results);
        } finally {
            TagReportQPS.successAdd(results.stream().filter(x -> x.getQ() == StatusCode.OK).count());
        }
    }

    public abstract void reportAction(List<TagData> results);
}
