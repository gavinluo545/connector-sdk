package cn.gavinluo.driver.utils.tcp.impl.message;

import cn.gavinluo.driver.utils.tcp.message.FrameMessage;
import lombok.Data;

@Data
public class FutureResponse {
    private final FrameMessage message;
    private final int futureRequestSequenceId;
    private final byte responseUnusedTimeoutSenconds;
    public FutureResponse(FrameMessage message, int futureRequestSequenceId, byte responseUnusedTimeoutSenconds) {
        this.message = message;
        this.futureRequestSequenceId = futureRequestSequenceId;
        this.responseUnusedTimeoutSenconds = responseUnusedTimeoutSenconds;
    }

}
