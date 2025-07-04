package io.aeron.samples;

import cluster.EchoService;
import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.cluster.ClusterConfig;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.ShutdownSignalBarrier;

import java.util.Arrays;

public class ClusterMediaLauncher {
    private static ErrorHandler errorHandler(final String context) {
        return
                (Throwable throwable) ->
                {
                    System.err.println(context);
                    throwable.printStackTrace(System.err);
                };
    }

    private static final int PORT_BASE = 10000;

    public static void main(String[] args) {
        Integer nodeId = Integer.parseInt(args[0]);

        String ingressHostnamesStr = "localhost,localhost,localhost";
        String clusterHostnamesStr = "localhost,localhost,localhost";

        var ingressHostnames = Arrays.asList(ingressHostnamesStr.split(","));
        var clusterHostnames = Arrays.asList(clusterHostnamesStr.split(","));

        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();

        final ClusterConfig clusterConfig = ClusterConfig.create(
                nodeId, ingressHostnames, clusterHostnames, PORT_BASE, new EchoService());

        var strategy = new BackoffIdleStrategy(10,5,1000L,20000000L);

        clusterConfig.mediaDriverContext()
                .threadingMode(ThreadingMode.DEDICATED)
                .conductorIdleStrategy(strategy)
                .senderIdleStrategy(strategy)
                .receiverIdleStrategy(strategy)
                .sharedIdleStrategy(strategy)
                .errorHandler(errorHandler("Media Driver"));
        clusterConfig.archiveContext().errorHandler(errorHandler("Archive"));
        clusterConfig.aeronArchiveContext().errorHandler(errorHandler("Aeron Archive"));
        clusterConfig.consensusModuleContext().errorHandler(errorHandler("Consensus Module"));
        clusterConfig.clusteredServiceContext().errorHandler(errorHandler("Clustered Service"));
//        clusterConfig.consensusModuleContext().ingressChannel("aeron:udp?endpoint=localhost:9010|term-length=64k");
        clusterConfig.consensusModuleContext().ingressChannel("aeron:udp?term-length=64k");
        clusterConfig.consensusModuleContext().deleteDirOnStart(false); //true to always start fresh


        try (ClusteredMediaDriver clusteredMediaDriver = ClusteredMediaDriver.launch(
                clusterConfig.mediaDriverContext(),
                clusterConfig.archiveContext(),
                clusterConfig.consensusModuleContext());
             ClusteredServiceContainer container = ClusteredServiceContainer.launch(
                     clusterConfig.clusteredServiceContext())) {
            System.out.println("Started Cluster Node...");
            System.out.println("Media Driver is running at " + clusteredMediaDriver.mediaDriver().aeronDirectoryName());
            System.out.println("Consensus is running at " + clusteredMediaDriver.consensusModule().context().aeronDirectoryName());
            System.out.println("Archive is running at " + clusteredMediaDriver.archive().context().aeronDirectoryName());
            System.out.println("Container cluster dir " + container.context().clusterDir());
            System.out.println("Container aeron dir " + container.context().aeronDirectoryName());
            barrier.await();
            System.out.println("Exiting");
        }
    }
}