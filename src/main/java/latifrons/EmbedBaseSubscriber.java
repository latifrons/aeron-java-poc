package latifrons;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.*;

public class EmbedBaseSubscriber {
    public static void main(String[] args) {
        String dir = System.getProperty("dir", "./aeron-driver");
        String channel = System.getProperty("channel", "aeron:ipc");
        int streamId = Integer.parseInt(System.getProperty("streamId", "10"));
        String idleConfig = System.getProperty("idle", "yield");

        System.out.printf("dir=%s channel=%s streamId=%d idleConfig=%s%n", dir, channel, streamId, idleConfig);

        IdleStrategy idle = toIdleStrategy(idleConfig);

        final io.aeron.driver.MediaDriver.Context mediaDriverCtx = new io.aeron.driver.MediaDriver.Context()
                .aeronDirectoryName(dir)
                .dirDeleteOnStart(true)
                .threadingMode(io.aeron.driver.ThreadingMode.DEDICATED)
                .conductorIdleStrategy(idle)
                .senderIdleStrategy(idle)
                .receiverIdleStrategy(idle)
                .sharedIdleStrategy(idle)
                .dirDeleteOnShutdown(true);

        final Aeron.Context aeronContext = new Aeron.Context();

        final FragmentHandler handler = (buffer, offset, length, header) -> {
            final byte[] data = new byte[length];
            buffer.getBytes(offset, data);

            String s = new String(data);
            var nano = Long.parseLong(s);
            var tNow = System.nanoTime();
            System.out.printf("%8d: Frag offset=%d length=%d delay=%d ns %d us payload: %s\n", tNow, offset, length, tNow - nano, (tNow - nano) / 1000, s);
        };

        aeronContext.aeronDirectoryName(dir).idleStrategy(idle);

        try (MediaDriver mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);
             Aeron aeron = Aeron.connect(aeronContext);
             Subscription sub = aeron.addSubscription(channel, streamId);
             Publication pub = aeron.addPublication(channel, streamId)) {
            System.out.println("Media driver started at " + mediaDriver.aeronDirectoryName());
            System.out.println("Media Driver is running. Press Ctrl+C to exit.");

            while (!pub.isConnected()) {
                idle.idle();
            }
            final FragmentAssembler assembler = new FragmentAssembler(handler);

            while (true) {
                final int fragmentsRead = sub.poll(assembler, 10);
                idle.idle(fragmentsRead);
            }
        } catch (Exception e) {
            System.err.println("Media Driver interrupted: " + e.getMessage());
        }
    }

    public static IdleStrategy toIdleStrategy(String idle) {
        String[] vs = idle.split(",");

        switch (vs[0]) {
            case "backoffp":
                int v1 = mustAtoI(vs[1]);
                int v2 = mustAtoI(vs[2]);
                int v3 = mustAtoI(vs[3]);
                int v4 = mustAtoI(vs[4]);
                return new BackoffIdleStrategy(v1, v2, v3, v4);

            case "":
            case "backoff":
                return new BackoffIdleStrategy();
            case "busyspin":
                return new BusySpinIdleStrategy();
            case "sleeping":
                long sleepNanos = mustAtoI(vs[1]);
                return new SleepingIdleStrategy(sleepNanos);

            case "yield":
                return new YieldingIdleStrategy();
            case "no-op":
                return new NoOpIdleStrategy();
            default:
                throw new IllegalArgumentException("Unknown idle strategy: " + idle);
        }
    }

    // Helper method to convert string to int (equivalent to mustAtoI)
    private static int mustAtoI(String s) {
        return Integer.parseInt(s);
    }

}
