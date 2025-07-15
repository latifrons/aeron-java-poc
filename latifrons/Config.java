MediaDriver.Context mediaDriverContext = (new MediaDriver.Context()).
        aeronDirectoryName(aeronDirName). // C:\Users\LATFIR~1\AppData\Local\Temp\aeron-latfirons-0-driver
                threadingMode(ThreadingMode.SHARED).
        termBufferSparseFile(true).
        multicastFlowControlSupplier(new MinMulticastFlowControlSupplier());

AeronArchive.Context replicationArchiveContext = (new AeronArchive.Context()).
        controlResponseChannel("aeron:udp?endpoint=" + hostname + ":0"); // aeron:udp?endpoint=localhost:0

Archive.Context archiveContext = (new Archive.Context()).
        aeronDirectoryName(aeronDirName). // C:\Users\LATFIR~1\AppData\Local\Temp\aeron-latfirons-0-driver
                archiveDir(new File(baseDir, "archive")). // D:\ws\latifrons\aeron-java-poc\aeron-cluster-0\archive
                controlChannel(udpChannel(memberId, hostname, portBase, 1)). // aeron:udp?endpoint=localhost:10001
                archiveClientContext(replicationArchiveContext).
        localControlChannel("aeron:ipc?term-length=64k").
        replicationChannel("aeron:udp?endpoint=" + hostname + ":0"). // aeron:udp?endpoint=localhost:0
                recordingEventsEnabled(false).
        threadingMode(ArchiveThreadingMode.SHARED);

AeronArchive.Context aeronArchiveContext = (new AeronArchive.Context()).
        lock(NoOpLock.INSTANCE).
        controlRequestChannel(archiveContext.localControlChannel()).
        controlRequestStreamId(archiveContext.localControlStreamId()).
        controlResponseChannel(archiveContext.localControlChannel()).
        aeronDirectoryName(aeronDirName);

ConsensusModule.Context consensusModuleContext = (new ConsensusModule.Context()).
        clusterMemberId(memberId). // 0
                clusterMembers(clusterMembers). //'0,localhost:10002,localhost:10003,localhost:10004,localhost:10005,localhost:10001|1,localhost:10102,localhost:10103,localhost:10104,localhost:10105,localhost:10101|2,localhost:10202,localhost:10203,localhost:10204,localhost:10205,localhost:10201|'
                clusterDir(new File(baseDir, "cluster")). // D:\ws\latifrons\aeron-java-poc\aeron-cluster-0\cluster
                archiveContext(aeronArchiveContext.clone()).
        serviceCount(1 + additionalServices.length). // 1
                replicationChannel("aeron:udp?endpoint=" + hostname + ":0"); // aeron:udp?endpoint=localhost:0

List<ClusteredServiceContainer.Context> serviceContexts = new ArrayList();

ClusteredServiceContainer.Context clusteredServiceContext = (new ClusteredServiceContainer.Context()).
        aeronDirectoryName(aeronDirName). // 'C:\Users\LATFIR~1\AppData\Local\Temp\aeron-latfirons-0-driver'
                archiveContext(aeronArchiveContext.clone()).
        clusterDir(new File(baseDir, "cluster")). // D:\ws\latifrons\aeron-java-poc\aeron-cluster-0\cluster
                clusteredService(clusteredService).
        serviceId(0);


ClusteredServiceContainer.Context additionalServiceContext = (new ClusteredServiceContainer.Context()).
        aeronDirectoryName(aeronDirName).
        archiveContext(aeronArchiveContext.clone()).
        clusterDir(new File(baseDir, "cluster")).
        clusteredService(additionalServices[i]).
        serviceId(i + 1);
