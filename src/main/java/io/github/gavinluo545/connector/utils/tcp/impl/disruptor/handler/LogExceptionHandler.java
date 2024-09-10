package io.github.gavinluo545.connector.utils.tcp.impl.disruptor.handler;

import io.github.gavinluo545.connector.utils.tcp.impl.disruptor.SequenceId;
import com.lmax.disruptor.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogExceptionHandler implements ExceptionHandler<SequenceId> {

    @Override
    public void handleEventException(Throwable ex, long sequence, SequenceId event) {
        log.error("处理事件[{}]异常", event);
        if (ex instanceof NullPointerException) {
            StackTraceElement[] stackTrace = ex.getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                log.error(stackTraceElement.toString());
            }
        }
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        log.error("事件处理器启动失败", ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        log.error("事件处理器关闭失败", ex);
    }
}

