package twoagents;

import io.aeron.Subscription;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ShutdownSignalBarrier;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

public class ReceiveAgent implements Agent {
    private final Subscription subscription;
    //    private final OneToOneRingBuffer ringBuffer;
    private final ShutdownSignalBarrier barrier;
    private final int sendCount;
    private final Logger logger = LogManager.getLogger(ReceiveAgent.class);

    public ReceiveAgent(final Subscription subscription,
                        ShutdownSignalBarrier barrier, int sendCount) {
        this.subscription = subscription;
        this.barrier = barrier;
        this.sendCount = sendCount;
    }
//    public ReceiveAgent(final OneToOneRingBuffer ringBuffer,
//                        ShutdownSignalBarrier barrier, int sendCount) {
//        this.ringBuffer = ringBuffer;
//        this.barrier = barrier;
//        this.sendCount = sendCount;
//    }

    @Override
    public int doWork() throws Exception {
        subscription.poll(this::handler, 1000);
        return 0;
    }

    private void handler(DirectBuffer buffer, int offset, int length,
                         Header header) {
        final long group = buffer.getLong(offset);
        final int lastValue = buffer.getInt(offset + 8);

        logger.info("received: {} {}, offset: {}, length: {}", group, lastValue, offset, length);


        if (lastValue >= sendCount) {
            logger.info("received: {}", lastValue);
            barrier.signal();
        }
    }

//    private void handler(final int messageType, final DirectBuffer buffer, final int offset, final int length) {
//        final int lastValue = buffer.getInt(offset);
//        if (lastValue % 10000 == 0) {
//            logger.info("received: {}", lastValue);
//        }
//
//        if (lastValue >= sendCount) {
//            logger.info("received: {}", lastValue);
//            barrier.signal();
//        }
//    }

    @Override
    public String roleName() {
        return "receiver";
    }
}