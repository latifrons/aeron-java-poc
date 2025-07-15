package io.aeron.samples;

import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

import java.io.File;

public class MediaLauncher {
    public static void main(String[] args) {
        var dir = System.getProperty("aeron.dir", "./aeron-driver");
//        var strategy = new BackoffIdleStrategy(10,5,1000L,20000000L);
//        var strategy = new BackoffIdleStrategy(10,5,100L,400L);
        var strategy = new YieldingIdleStrategy();

        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
                .aeronDirectoryName(dir)
                .dirDeleteOnStart(true)
                .threadingMode(ThreadingMode.SHARED)
                .conductorIdleStrategy(strategy)
                .senderIdleStrategy(strategy)
                .receiverIdleStrategy(strategy)
                .sharedIdleStrategy(strategy)
                .dirDeleteOnShutdown(true);
//        final MediaDriver mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        MediaDriver mediaDriver = MediaDriver.launch(mediaDriverCtx);


        // keep the media driver running

        System.out.println("Media driver started at " + new File(mediaDriver.aeronDirectoryName()).getAbsolutePath());
        System.out.println("Media Driver is running. Press Ctrl+C to exit.");
        try {
            Thread.currentThread().join();  // Keep the main thread alive
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Media Driver interrupted: " + e.getMessage());
        }
        mediaDriver.close();
    }
}