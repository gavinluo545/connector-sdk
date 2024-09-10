package io.github.gavinluo545.connector.connection;

import io.github.gavinluo545.connector.connection.model.TagData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class AbstractTagsDataReporter {

    public void report(List<TagData> results) {
        try {
            reportAction(results);
        } finally {
            TagReportQPS.successAdd(results.stream().filter(x -> x.getQ() == StatusCode.OK).count());
        }
    }

    public abstract void reportAction(List<TagData> results);
}
