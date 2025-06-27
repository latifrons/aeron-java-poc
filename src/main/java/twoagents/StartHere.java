package twoagents;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.concurrent.*;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

class XConfig {
    SendAgent sendAgent;
    ReceiveAgent receiveAgent;
    IdleStrategy idleStrategySend = new NoOpIdleStrategy();
    IdleStrategy idleStrategyReceive = new NoOpIdleStrategy();
    ShutdownSignalBarrier barrier;

    public XConfig(SendAgent sendAgent, ReceiveAgent receiveAgent, ShutdownSignalBarrier barrier) {
        this.sendAgent = sendAgent;
        this.receiveAgent = receiveAgent;
        this.barrier = barrier;
    }
}

public class StartHere {
    private static final Logger logger = LogManager.getLogger(StartHere.class);

    public static XConfig setup2(String channel, int stream, int sendCount) {

        //construct Aeron, pointing at the media driver's folder
        final Aeron.Context aeronCtx = new Aeron.Context()
                .idleStrategy(new NoOpIdleStrategy())
//                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                ;
        final Aeron aeron = Aeron.connect(aeronCtx);

        final IdleStrategy idleStrategySend = new NoOpIdleStrategy();
        final IdleStrategy idleStrategyReceive = new NoOpIdleStrategy();
        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();

        //construct the subs and pubs
        final Subscription subscription = aeron.addSubscription(channel, stream);
        final Publication publication = aeron.addPublication(channel, stream);

        //construct the agents
        final SendAgent sendAgent = new SendAgent(publication, sendCount);
        final ReceiveAgent receiveAgent = new ReceiveAgent(subscription, barrier,
                sendCount);

        return new XConfig(sendAgent, receiveAgent, barrier);
    }

//    public static XConfig setup1(int sendCount) {
//        final int bufferLength = 16384 + RingBufferDescriptor.TRAILER_LENGTH;
//        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(bufferLength));
//        final OneToOneRingBuffer ringBuffer = new OneToOneRingBuffer(unsafeBuffer);
//        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();
//
//        //construct the agents
//        final SendAgent sendAgent = new SendAgent(ringBuffer, sendCount);
//        final ReceiveAgent receiveAgent = new ReceiveAgent(ringBuffer, barrier, sendCount);
//
//        return new XConfig(sendAgent, receiveAgent, barrier);
//    }

    public static void main(String[] args) {
        logger.info("StartHere starting up");
        ChannelUriStringBuilder channelUriBuilder = new ChannelUriStringBuilder()
//                .media("ipc")
                .media("udp")
                .endpoint("127.0.0.1:39999")
//                .termLength(64 * 1024)
//                .mtu(16384)
                ;

        final String channel = channelUriBuilder.build();
        System.out.println("channel: " + channel);

        final int stream = 10;
        final int sendCount = 100;

//        XConfig xConfig = setup1(sendCount);
        XConfig xConfig = setup2(channel, stream, sendCount);

        //construct agent runners
        final AgentRunner sendAgentRunner = new AgentRunner(xConfig.idleStrategySend,
                Throwable::printStackTrace, null, xConfig.sendAgent);
        final AgentRunner receiveAgentRunner = new AgentRunner(xConfig.idleStrategyReceive,
                Throwable::printStackTrace, null, xConfig.receiveAgent);

        logger.info("starting");
        long v1 = System.currentTimeMillis();
        System.out.printf("starting at %d\n", v1);

        //start the runners
        AgentRunner.startOnThread(sendAgentRunner);
//        AgentRunner.startOnThread(receiveAgentRunner);

        //wait for the final item to be received before closing


        long v2 = System.currentTimeMillis();
        System.out.printf("ended at %d, total %d, tps %.2f\n", v2, v2 - v1,
                (sendCount * 1000.0) / (v2 - v1));

        xConfig.barrier.await();

        //close the resources
        receiveAgentRunner.close();
        sendAgentRunner.close();
//        aeron.close();
//        mediaDriver.close();
    }
}
