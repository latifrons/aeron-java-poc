package twoagents;

import io.aeron.Publication;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

public class SendAgent implements Agent {
    //    private final Publication publication;
    private final Publication publication;
    private final int sendCount;
    private final UnsafeBuffer unsafeBuffer;
    private int currentCountItem = 1;
    private final Logger logger = LogManager.getLogger(SendAgent.class);

    private long groupName;

    public SendAgent(final Publication publication, int sendCount) {
        this.publication = publication;
        this.sendCount = sendCount;
        this.unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(64));
        this.groupName = System.currentTimeMillis();
        unsafeBuffer.putLong(0, groupName);
        unsafeBuffer.putInt(8, currentCountItem);
    }

//    public SendAgent(final OneToOneRingBuffer ringBuffer, int sendCount) {
//        this.ringBuffer = ringBuffer;
//        this.sendCount = sendCount;
//        this.unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(64));
//        unsafeBuffer.putInt(0, currentCountItem);
//    }

    @Override
    public int doWork() {
        if (currentCountItem > sendCount) {
            return 0;
        }

        if (publication.isConnected()) {
            if (publication.offer(unsafeBuffer, 0, 16) > 0) {
                currentCountItem += 1;
                unsafeBuffer.putLong(0, groupName);
                unsafeBuffer.putInt(8, currentCountItem);
            }
        }
        return 0;
    }

//    @Override
//    public int doWork() {
//        if (currentCountItem > sendCount) {
//            return 0;
//        }
//
//        final int claimIndex = ringBuffer.tryClaim(1, Integer.BYTES);
//        if (claimIndex > 0) {
//            currentCountItem += 1;
//            final AtomicBuffer buffer = ringBuffer.buffer();
//            buffer.putInt(claimIndex, currentCountItem);
//            ringBuffer.commit(claimIndex);
//        }
//        return 0;
//    }


    @Override
    public String roleName() {
        return "sender";
    }
}