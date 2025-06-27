package latifrons;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.BufferUtil;
import org.agrona.concurrent.*;

import java.time.Instant;

public class EmbedBasePublisher {
    public static void main(String[] args) {
        String dir = System.getProperty("dir", "./aeron-driver");
        String channel = System.getProperty("channel", "aeron:ipc");
        int streamId = Integer.parseInt(System.getProperty("streamId", "10"));
        String idleConfig = System.getProperty("idle", "yield");

        System.out.printf("dir=%s channel=%s streamId=%d idleConfig=%s%n", dir, channel, streamId, idleConfig);

        IdleStrategy idle = toIdleStrategy(idleConfig);

        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
                .aeronDirectoryName(dir)
                .dirDeleteOnStart(true)
                .threadingMode(io.aeron.driver.ThreadingMode.DEDICATED)
                .conductorIdleStrategy(idle)
                .senderIdleStrategy(idle)
                .receiverIdleStrategy(idle)
                .sharedIdleStrategy(idle)
                .dirDeleteOnShutdown(true);

        final Aeron.Context aeronContext = new Aeron.Context();

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

            final UnsafeBuffer buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(256, 64));

            while (true){
                // publish current nano time as a string once a second
                Instant now = Instant.now();
                long epochNanos = now.getEpochSecond() * 1_000_000_000L + now.getNano();
                String message = String.valueOf(epochNanos);
                final int length = buffer.putStringWithoutLengthAscii(0, message);
                long result = pub.offer(buffer, 0, length);
                if (result < 0) {
                    if (result == Publication.BACK_PRESSURED) {
                        System.out.println("Back pressured, retrying...");
                    } else if (result == Publication.NOT_CONNECTED) {
                        System.out.println("Publication not connected, waiting...");
                    } else if (result == Publication.ADMIN_ACTION) {
                        System.out.println("Admin action required, waiting...");
                    } else {
                        System.err.println("Failed to send message: " + result);
                    }
                } else {
                    System.out.printf("%8d: Published message: %s%n", epochNanos, message);
                }
                idle.idle(); // Idle to avoid busy waiting
                try {
                    Thread.sleep(1000); // Sleep for 1 second before sending the next message
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Publisher interrupted: " + e.getMessage());
                    break;
                }
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
