package latifrons;

import cluster.ClusterConfig;
import cluster.EchoServiceNode;
import io.aeron.CommonContext;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.client.AeronArchive;
import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.MinMulticastFlowControlSupplier;
import io.aeron.driver.ThreadingMode;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.NoOpLock;
import org.agrona.concurrent.ShutdownSignalBarrier;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static java.lang.Integer.parseInt;

public class ClusterMediaLauncher {
    private static ErrorHandler errorHandler(final String context) {
        return
                (Throwable throwable) ->
                {
                    System.err.println(context);
                    throwable.printStackTrace(System.err);
                };
    }

    private static final int PORT_BASE = 9000;

    public static void main(String[] args) {
        final int nodeId = parseInt(EnvTool.mustGetEnv("aeron.cluster.nodeId"));
        final String hostnamesStr = EnvTool.mustGetEnv("aeron.cluster.hostnames");
        final String clusterDir = EnvTool.mustGetEnv("aeron.cluster.clusterDir");
        final String aeronDir = EnvTool.mustGetEnv("aeron.cluster.aeronDir");

        final int ingressStreamId = parseInt(EnvTool.mustGetEnv("aeron.cluster.ingressStreamId"));
        final List<String> hostnames = Arrays.asList(hostnamesStr.split(","));
        final List<String> internalHostnames = Arrays.asList(hostnamesStr.split(","));

        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();

        final cluster.ClusterConfig clusterConfig = ClusterConfig.create(
                nodeId, hostnames, internalHostnames, PORT_BASE, null);

        clusterConfig.mediaDriverContext().
                aeronDirectoryName(aeronDir).
                errorHandler(errorHandler("Media Driver"));
        clusterConfig.archiveContext()
                .archiveDir(new File(clusterDir, "archive"))
                .errorHandler(errorHandler("Archive"));
        clusterConfig.aeronArchiveContext()
                .errorHandler(errorHandler("Aeron Archive"));
        clusterConfig.consensusModuleContext()
                .clusterDir(new File(clusterDir, "cluster"))
                .ingressChannel("aeron:udp?endpoint=" + hostnames.get(nodeId) + ":0")
                .ingressStreamId(ingressStreamId)
                .errorHandler(errorHandler("Consensus Module"));
        clusterConfig.clusteredServiceContext()
                .errorHandler(errorHandler("Clustered Service"));

        try (
                ClusteredMediaDriver ignore = ClusteredMediaDriver.launch(
                        clusterConfig.mediaDriverContext(),
                        clusterConfig.archiveContext(),
                        clusterConfig.consensusModuleContext())) {

            // ClusteredServiceContainer ignore2 = ClusteredServiceContainer.launch(
            //                        clusterConfig.clusteredServiceContext())
            System.out.println("[" + nodeId + "] Started Cluster Node on " + hostnames.get(nodeId) + "...");
            System.out.println("[" + nodeId + "] Cluster Members: " + clusterConfig.consensusModuleContext().clusterMembers());
            System.out.println("[" + nodeId + "] Aeron Folder: " + clusterConfig.mediaDriverContext().aeronDirectoryName());
            System.out.println("[" + nodeId + "] Cluster Folder: " + clusterConfig.consensusModuleContext().clusterDir());
            System.out.println("[" + nodeId + "] Archive Folder: " + clusterConfig.archiveContext().archiveDirectoryName());

            barrier.await();
            System.out.println("[" + nodeId + "] Exiting");
        } catch (Exception e) {
            System.err.println("[" + nodeId + "] Error during Cluster Node startup: " + e.getMessage());
            e.printStackTrace(System.err);
            throw e;
        }
    }
}
