package org.keycloak.jgroups.protocol;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.keycloak.cluster.infinispan.ClusterHealthRestored;
import org.keycloak.cluster.infinispan.InfinispanClusterProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.DefaultInfinispanConnectionProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.NodeInfo;
import org.keycloak.connections.infinispan.TopologyInfo;
import org.keycloak.infinispan.health.impl.JdbcPingClusterHealthImpl;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderEventListener;

import org.hamcrest.CoreMatchers;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.util.concurrent.WithinThreadExecutor;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.logging.Log;
import org.jgroups.protocols.FD_ALL3;
import org.jgroups.protocols.MERGE3;
import org.jgroups.protocols.PingData;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.relay.SiteUUID;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.NameCache;
import org.jgroups.util.Responses;
import org.jgroups.util.ThreadFactory;
import org.jgroups.util.UUID;
import org.jgroups.util.Util;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Misc tests for {@link KEYCLOAK_JDBC_PING2}, running against H2
 * @author Bela Ban
 * @author Alexander Schwartz
 */
public class JdbcPing2Test {
    protected static final Logger log = Logger.getLogger(JdbcPing2Test.class);

    protected static final String CLUSTER="jdbc-test";
    protected static final int NUM_NODES=8;
    public static final String PROTOCOL_STACK = "jdbc-h2.xml";

    static {
        ClassConfigurator.addProtocol((short) 1026, KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
    }

    /**
     * 100 iterations would run approx 8 minutes and should complete successfully,
     * with an average of 3.3 seconds in converging.
     */
    @Test
    @Ignore
    public void testConcurrentStartupMultipleTimes() throws Exception {
        int count = 100;
        long sum = 0;
        for (int j = 0; j < 100; j++) {
            sum += runSingleTest();
        }
        log.info("Average time to form the cluster: " + Duration.ofNanos(sum / count));
    }

    @Test
    public void testClusterHealth() {
        var ping = new ControlledJdbcPing();
        var clusterHealth = new JdbcPingClusterHealthImpl();
        var addresses = IntStream.range(0, 2)
                .mapToObj(operand -> new UUID(0, operand))
                .sorted()
                .toArray(Address[]::new);

        // Set up initial healthy state before init, so the synchronous check in init() passes
        ping.setView(addresses[0]);
        ping.setPingData(List.of(addresses[0]));
        assertFalse(clusterHealth.isHealthy());
        clusterHealth.init(ping, new WithinThreadExecutor());
        assertTrue(clusterHealth.isHealthy());

        // test exception
        ping.setException(new RuntimeException("Induced"));
        assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, ping.healthStatus());
        clusterHealth.triggerClusterHealthCheck();
        assertFalse(clusterHealth.isHealthy());
        // Remove exception
        ping.setException(null);

        // test empty table / no coordinator
        ping.setPingData(List.of());
        assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.NO_COORDINATOR, ping.healthStatus());
        clusterHealth.triggerClusterHealthCheck();
        assertFalse(clusterHealth.isHealthy());

        // test member in the view / single coordinator
        ping.setPingData(List.of(addresses[0]));
        assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, ping.healthStatus());
        clusterHealth.triggerClusterHealthCheck();
        assertTrue(clusterHealth.isHealthy());

        // test higher ID loses
        // coordinator a[0] in the table, and we belong to view with the coordinator a[1]
        ping.setView(addresses[1]);
        assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.UNHEALTHY, ping.healthStatus());
        clusterHealth.triggerClusterHealthCheck();
        assertFalse(clusterHealth.isHealthy());

        // test lower ID wins
        // coordinator a[0] and a[1] in the table, and we belong to view with the coordinator a[0]
        ping.setPingData(List.of(addresses[1], addresses[0]));
        ping.setView(addresses[0]);
        assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, ping.healthStatus());
        clusterHealth.triggerClusterHealthCheck();
        assertTrue(clusterHealth.isHealthy());

        // test lower ID wins
        // coordinator a[0] and a[1] in the table, and we belong to view with the coordinator a[1]
        ping.setPingData(List.of(addresses[1], addresses[0]));
        ping.setView(addresses[1]);
        assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.UNHEALTHY, ping.healthStatus());
        clusterHealth.triggerClusterHealthCheck();
        assertFalse(clusterHealth.isHealthy());

        // test more members in a partition win
        // coordinator a[0] and a[1] in the table, and we belong to view with the coordinator a[1]
        ping.setPingData(List.of(addresses[1], addresses[0]), Map.of(addresses[0], 1, addresses[1], 2));
        assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, ping.healthStatus());
        clusterHealth.triggerClusterHealthCheck();
        assertTrue(clusterHealth.isHealthy());
    }

    @SuppressWarnings("resource")
    private static long runSingleTest() throws Exception {
        JChannel[] channels = new JChannel[NUM_NODES];
        List<Thread> threads = new ArrayList<>();
        try {
            for (int i = 0; i < channels.length; i++) {
                channels[i] = createChannel(String.valueOf(i + 1));
            }
            CountDownLatch latch = new CountDownLatch(1);
            int index = 1;
            for (JChannel ch : channels) {
                ThreadFactory thread_factory = ch.stack().getTransport().getThreadFactory();
                Connector connector = new Connector(latch, ch);
                Thread thread = thread_factory.newThread(connector, "connector-" + index++);
                threads.add(thread);
                thread.start();
            }
            latch.countDown();
            long start = System.nanoTime();
            Util.waitUntilAllChannelsHaveSameView(40000, 100, channels);
            long time = System.nanoTime() - start;
            log.infof("-- cluster of %d formed in %s:\n%s\n", NUM_NODES, Duration.ofNanos(time),
                    Stream.of(channels).map(ch -> String.format("%s: %s", ch.address(), ch.view()))
                            .collect(Collectors.joining("\n")));
            return time;
        } finally {
            for (Thread thread : threads) {
                thread.join();
            }
            Arrays.stream(channels).filter(ch -> ch.view().getCoord() != ch.getAddress()).forEach(JChannel::close);
            Arrays.stream(channels).filter(ch -> !ch.isClosed()).forEach(JChannel::close);
            log.infof("Closed");
        }
    }

    @Test
    public void testClearing() throws Exception {
        JChannel channel = createChannel("0");
        try (channel) {
            channel.connect("ISPN");
            KEYCLOAK_JDBC_PING2_FOR_TESTING jdbcPing = (KEYCLOAK_JDBC_PING2_FOR_TESTING) channel.getProtocolStack().getProtocols().stream().filter(protocol -> protocol instanceof KEYCLOAK_JDBC_PING2).findFirst().orElseThrow(() -> new RuntimeException("Didn't find JDBC_PING"));
            try (Connection con = jdbcPing.getConnection()) {
                // Insert an entry of a second coordinator
                PingData data = new PingData(new UUID(), false, "old", new IpAddress("127.0.0.1:9999")).coord(true);
                try (PreparedStatement ps = con.prepareStatement(jdbcPing.getInsertSingleSql())) {
                    Address address = data.getAddress();
                    String addr = Util.addressToString(address);
                    String name = address instanceof SiteUUID ? ((SiteUUID) address).getName() : NameCache.get(address);
                    PhysicalAddress ip_addr = data.getPhysicalAddr();
                    String ip = ip_addr.toString();
                    ps.setString(1, addr);
                    ps.setString(2, name);
                    ps.setString(3, jdbcPing.getClusterName());
                    ps.setString(4, ip);
                    ps.setBoolean(5, data.isCoord());
                    ps.setLong(6, Time.currentTime());
                    ps.setString(7, Util.addressToString(data.getAddress()));
                    ps.executeUpdate();
                }

                // See that the second coordinator is still there
                Responses responses = new Responses(false);
                jdbcPing.findMembers(null, false, responses);
                assertThat(responses.size(), CoreMatchers.equalTo(2));

                // Advance the time beyond the timeout. See the entry is not returned.
                Time.setOffset(120);
                responses.clear();
                jdbcPing.writeAll();
                jdbcPing.findMembers(null, false, responses);
                assertThat(responses.size(), CoreMatchers.equalTo(1));
                // The entry is still in the database though as it will only be cleared on view change
                try (PreparedStatement ps= con.prepareStatement(jdbcPing.getSelectAllPingdataSql())) {
                    ps.setString(1, jdbcPing.getClusterName());
                    try (ResultSet resultSet = ps.executeQuery()) {
                        resultSet.last();
                        assertThat(resultSet.getRow(), CoreMatchers.equalTo(2));
                    }
                }

                // Simulate a row change to trigger a cleanup of the table.
                jdbcPing.handleView(jdbcPing.getTransport().view(), jdbcPing.getTransport().view(), true);
                try (PreparedStatement ps= con.prepareStatement(jdbcPing.getSelectAllPingdataSql())) {
                    ps.setString(1, jdbcPing.getClusterName());
                    try (ResultSet resultSet = ps.executeQuery()) {
                        resultSet.last();
                        assertThat(resultSet.getRow(), CoreMatchers.equalTo(1));
                    }
                }
            }
        } finally {
            Time.setOffset(0);
        }
    }

    @SuppressWarnings("resource")
    protected static JChannel createChannel(String name) throws Exception {
        return new JChannel(JdbcPing2Test.PROTOCOL_STACK).name(name);
    }

    private static void suspectPeers(JChannel first, JChannel second) throws Exception {
        // JGroups timers do not use Time's offset. Deliver their verified-suspicion events without waiting for them.
        GMS firstGms = first.getProtocolStack().findProtocol(GMS.class);
        GMS secondGms = second.getProtocolStack().findProtocol(GMS.class);
        firstGms.up(new Event(Event.SUSPECT, List.of(second.getAddress())));
        secondGms.up(new Event(Event.SUSPECT, List.of(first.getAddress())));
        Util.waitUntil(10000, 50, () -> first.getView().size() == 1 && second.getView().size() == 1);
    }

    private static void mergePartitions(JChannel... channels) throws Exception {
        MERGE3[] protocols = Arrays.stream(channels)
                .map(channel -> channel.getProtocolStack().<MERGE3>findProtocol(MERGE3.class))
                .toArray(MERGE3[]::new);
        // Run normal discovery and merge checks directly instead of waiting for JGroups' independent periodic timers.
        Util.waitUntil(10000, 50, () -> {
            var view = channels[0].getView();
            if (view.size() == channels.length && Arrays.stream(channels).allMatch(channel -> view.equals(channel.getView()))) {
                return true;
            }
            for (MERGE3 protocol : protocols) {
                protocol.sendInfo();
            }
            for (MERGE3 protocol : protocols) {
                protocol.checkInconsistencies();
            }
            return false;
        }, () -> Arrays.stream(channels).map(channel -> channel.getName() + ": " + channel.getView())
                .collect(Collectors.joining("\n")));
    }

    @Test
    public void testCoordinatorQuarantinesAndRecoversWithoutRestart() throws Exception {
        try (JChannel first = createChannel("coordinator"); JChannel second = createChannel("member")) {
            first.connect("quarantine-recovery");
            second.connect("quarantine-recovery");
            Util.waitUntilAllChannelsHaveSameView(10000, 50, first, second);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = first.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            KEYCLOAK_JDBC_PING2_FOR_TESTING member = second.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            var recoveryCallbacks = new AtomicInteger();
            var cleanupAllowed = new AtomicBoolean();
            coordinator.setOnHealthRestored(() -> {
                if (coordinator.isQuarantined()) {
                    assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, coordinator.healthStatus());
                }
                recoveryCallbacks.incrementAndGet();
                if (!cleanupAllowed.get()) {
                    throw new IllegalStateException("Induced cache cleanup failure");
                }
            });
            coordinator.setDatabaseAvailable(false);
            try {
                coordinator.writeAll();
                coordinator.runHealthCheck(new CapturingLog());
                assertFalse(coordinator.isQuarantined());
                Time.setOffset(80);
                coordinator.runHealthCheck(new CapturingLog());
                assertTrue(coordinator.isQuarantined());
                // Keep one end-to-end check that quarantined traffic is detected automatically by the real failure detectors.
                Util.waitUntil(20000, 50, () -> first.getView().size() == 1 && second.getView().size() == 1);
                assertTrue(first.isConnected());
                assertTrue(second.isConnected());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, member.healthStatus());
                try (JChannel joiner = createChannel("joiner")) {
                    joiner.connect("quarantine-recovery");
                    Util.waitUntilAllChannelsHaveSameView(10000, 50, second, joiner);
                    assertEquals(second.getAddress(), joiner.getView().getCoord());
                    coordinator.setDatabaseAvailable(true);
                    coordinator.runHealthCheck(new CapturingLog());
                    assertTrue(coordinator.isQuarantined());
                    assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, coordinator.healthStatus());
                    assertTrue(recoveryCallbacks.get() >= 1);
                    coordinator.writeAll();
                    assertFalse(member.readFromDB("quarantine-recovery").stream()
                            .anyMatch(data -> data.isCoord() && data.getAddress().equals(first.getAddress())));
                    cleanupAllowed.set(true);
                    coordinator.runHealthCheck(new CapturingLog());
                    assertFalse(coordinator.isQuarantined());
                    assertTrue(recoveryCallbacks.get() >= 2);
                    assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.UNHEALTHY, coordinator.healthStatus());
                    assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, member.healthStatus());
                    assertTrue(member.readFromDB("quarantine-recovery").stream()
                            .anyMatch(data -> data.isCoord() && data.getAddress().equals(first.getAddress())));
                    mergePartitions(first, second, joiner);
                    coordinator.runHealthCheck(new CapturingLog());
                    assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, coordinator.healthStatus());
                    assertTrue(first.isConnected());
                }
            } finally {
                Time.setOffset(0);
                coordinator.setDatabaseAvailable(true);
            }
        }
    }

    @Test
    public void testTransientDatabaseFailureDoesNotQuarantine() throws Exception {
        try (JChannel first = createChannel("coordinator"); JChannel second = createChannel("member")) {
            first.connect("quarantine-transient");
            second.connect("quarantine-transient");
            Util.waitUntilAllChannelsHaveSameView(10000, 50, first, second);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = first.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            var initialView = first.getView();
            coordinator.setDatabaseAvailable(false);
            try {
                coordinator.writeAll();
                coordinator.runHealthCheck(new CapturingLog());
                Time.setOffset(20);
                coordinator.runHealthCheck(new CapturingLog());
                assertFalse(coordinator.isQuarantined());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, coordinator.healthStatus());
                coordinator.setDatabaseAvailable(true);
                coordinator.writeAll();
                coordinator.runHealthCheck(new CapturingLog());
                Time.setOffset(80);
                coordinator.writeAll();
                coordinator.runHealthCheck(new CapturingLog());
                assertFalse(coordinator.isQuarantined());
                assertEquals(initialView, first.getView());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, coordinator.healthStatus());
            } finally {
                Time.setOffset(0);
                coordinator.setDatabaseAvailable(true);
            }
        }
    }

    @Test
    public void testReadFailuresDoNotQuarantineWhilePublicationSucceeds() throws Exception {
        try (JChannel first = createChannel("coordinator"); JChannel second = createChannel("member")) {
            first.connect("quarantine-read-failure");
            second.connect("quarantine-read-failure");
            Util.waitUntilAllChannelsHaveSameView(10000, 50, first, second);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = first.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            Object selectSql = coordinator.getValue("select_all_pingdata_sql");
            var initialView = first.getView();
            try {
                coordinator.setValue("select_all_pingdata_sql", "SELECT * FROM missing_table");
                coordinator.runHealthCheck(new CapturingLog());
                Time.setOffset(80);
                coordinator.writeAll();
                coordinator.runHealthCheck(new CapturingLog());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, coordinator.healthStatus());
                assertFalse(coordinator.isQuarantined());
                assertEquals(initialView, first.getView());
            } finally {
                coordinator.setValue("select_all_pingdata_sql", selectSql);
                Time.setOffset(0);
            }
        }
    }

    @Test
    public void testCoordinatorQuarantinesWhenDatabaseIsReadableButNotWritable() throws Exception {
        try (JChannel first = createChannel("coordinator"); JChannel second = createChannel("member")) {
            first.connect("quarantine-read-only");
            second.connect("quarantine-read-only");
            Util.waitUntilAllChannelsHaveSameView(10000, 50, first, second);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = first.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            String insertSql = coordinator.getInsertSingleSql();
            try {
                coordinator.setValue("insert_single_sql", "INSERT INTO missing_table VALUES (?)");
                coordinator.writeAll();
                coordinator.runHealthCheck(new CapturingLog());
                assertFalse(coordinator.isQuarantined());
                Time.setOffset(80);
                coordinator.runHealthCheck(new CapturingLog());
                assertTrue(coordinator.isQuarantined());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, coordinator.healthStatus());
                suspectPeers(first, second);
                coordinator.runHealthCheck(new CapturingLog());
                assertTrue(coordinator.isQuarantined());
            } finally {
                coordinator.setValue("insert_single_sql", insertSql);
                Time.setOffset(0);
            }
        }
    }

    @Test
    public void testQuarantineRecoversAfterClusterWideDatabaseOutage() throws Exception {
        try (JChannel first = createChannel("coordinator"); JChannel second = createChannel("member")) {
            first.connect("quarantine-full-outage");
            second.connect("quarantine-full-outage");
            Util.waitUntilAllChannelsHaveSameView(10000, 50, first, second);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = first.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            KEYCLOAK_JDBC_PING2_FOR_TESTING member = second.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            coordinator.setDatabaseAvailable(false);
            member.setDatabaseAvailable(false);
            try {
                coordinator.writeAll();
                coordinator.runHealthCheck(new CapturingLog());
                member.runHealthCheck(new CapturingLog());
                Time.setOffset(80);
                coordinator.runHealthCheck(new CapturingLog());
                suspectPeers(first, second);
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, coordinator.healthStatus());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, member.healthStatus());
                coordinator.setDatabaseAvailable(true);
                member.setDatabaseAvailable(true);
                coordinator.runHealthCheck(new CapturingLog());
                member.writeAll();
                mergePartitions(first, second);
                coordinator.runHealthCheck(new CapturingLog());
                member.runHealthCheck(new CapturingLog());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, coordinator.healthStatus());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, member.healthStatus());
            } finally {
                Time.setOffset(0);
                coordinator.setDatabaseAvailable(true);
                member.setDatabaseAvailable(true);
            }
        }
    }

    @Test
    public void testQuarantineWaitsForSingletonAfterEarlyDatabaseRecovery() throws Exception {
        try (JChannel first = createChannel("coordinator"); JChannel second = createChannel("member")) {
            first.connect("quarantine-unchanged-view");
            second.connect("quarantine-unchanged-view");
            Util.waitUntilAllChannelsHaveSameView(10000, 50, first, second);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = first.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            KEYCLOAK_JDBC_PING2_FOR_TESTING member = second.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            var initialView = first.getView();
            FD_ALL3 firstFailureDetector = first.getProtocolStack().findProtocol(FD_ALL3.class);
            FD_ALL3 secondFailureDetector = second.getProtocolStack().findProtocol(FD_ALL3.class);
            // Hold the original view until the early-recovery assertions, even on a slow test runner.
            firstFailureDetector.stopFailureDetection();
            secondFailureDetector.stopFailureDetection();
            var recoveryCallbacks = new AtomicInteger();
            coordinator.setOnHealthRestored(() -> {
                if (coordinator.isQuarantined()) {
                    assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, coordinator.healthStatus());
                    assertEquals(1, first.getView().size());
                }
                recoveryCallbacks.incrementAndGet();
            });
            coordinator.setDatabaseAvailable(false);
            try {
                coordinator.writeAll();
                coordinator.runHealthCheck(new CapturingLog());
                Time.setOffset(80);
                coordinator.runHealthCheck(new CapturingLog());
                assertTrue(coordinator.isQuarantined());

                coordinator.setDatabaseAvailable(true);
                coordinator.runHealthCheck(new CapturingLog());
                assertTrue(coordinator.isQuarantined());
                assertEquals(initialView, first.getView());
                assertEquals(initialView, second.getView());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.NO_COORDINATOR, member.healthStatus());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, coordinator.healthStatus());
                assertEquals(0, recoveryCallbacks.get());

                suspectPeers(first, second);
                coordinator.runHealthCheck(new CapturingLog());
                assertFalse(coordinator.isQuarantined());
                assertTrue(recoveryCallbacks.get() >= 1);
                assertTrue(first.isConnected());
                mergePartitions(first, second);
                coordinator.runHealthCheck(new CapturingLog());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, coordinator.healthStatus());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, member.healthStatus());
                List<PingData> published = member.readFromDB("quarantine-unchanged-view");
                assertEquals(2, published.size());
                assertEquals(List.of(first.getView().getCoord()), published.stream().filter(PingData::isCoord)
                        .map(PingData::getAddress).toList());
                assertEquals(2, published.stream().filter(PingData::isCoord).findFirst().orElseThrow().mbrs().size());
            } finally {
                Time.setOffset(0);
                coordinator.setDatabaseAvailable(true);
            }
        }
    }

    @Test
    public void testQuarantineRecoversWhenOtherMembersHaveStopped() throws Exception {
        try (JChannel first = createChannel("coordinator"); JChannel second = createChannel("member")) {
            first.connect("quarantine-last-survivor");
            second.connect("quarantine-last-survivor");
            Util.waitUntilAllChannelsHaveSameView(10000, 50, first, second);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = first.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
            coordinator.setDatabaseAvailable(false);
            try {
                coordinator.writeAll();
                coordinator.runHealthCheck(new CapturingLog());
                Time.setOffset(80);
                coordinator.runHealthCheck(new CapturingLog());
                suspectPeers(first, second);
                second.close();
                Time.setOffset(160);
                coordinator.setDatabaseAvailable(true);
                coordinator.runHealthCheck(new CapturingLog());
                assertFalse(coordinator.isQuarantined());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, coordinator.healthStatus());
                assertTrue(first.isConnected());
                assertEquals(1, first.getView().size());
            } finally {
                Time.setOffset(0);
                coordinator.setDatabaseAvailable(true);
            }
        }
    }

    @Test
    public void testRunHealthCheck() {
        var ping = new ControlledJdbcPing();
        var capturingLog = new CapturingLog();

        // Seed the view so runHealthCheck doesn't bail out early.
        ping.setView(new org.jgroups.util.UUID(0, 0));

        // Initial state is HEALTHY — no log on first call.
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
        ping.runHealthCheck(capturingLog);
        assertEquals(0, capturingLog.errors.get());
        assertEquals(0, capturingLog.infos.get());

        // ERROR transition: one warning logged immediately (ERROR is a warn-level event, not an error).
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR);
        ping.runHealthCheck(capturingLog);
        assertEquals(1, capturingLog.warns.get());

        // Stays ERROR: no additional log on the next cycle.
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR);
        ping.runHealthCheck(capturingLog);
        assertEquals(1, capturingLog.warns.get());

        // Recovery to HEALTHY: one info logged.
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
        ping.runHealthCheck(capturingLog);
        assertEquals(1, capturingLog.infos.get());

        // Stays HEALTHY: no further log.
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
        ping.runHealthCheck(capturingLog);
        assertEquals(1, capturingLog.infos.get());
    }

    @Test
    public void testOnHealthRestoredCallback() {
        var ping = new ControlledJdbcPing();
        var capturingLog = new CapturingLog();
        var callbackCount = new AtomicInteger();

        ping.setView(new UUID(0, 0));
        ping.setOnHealthRestored(callbackCount::incrementAndGet);

        // Initial HEALTHY — no callback (no transition)
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
        ping.runHealthCheck(capturingLog);
        assertEquals(0, callbackCount.get());

        // Transition to ERROR — no callback
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR);
        ping.runHealthCheck(capturingLog);
        assertEquals(0, callbackCount.get());

        // Recovery to HEALTHY — callback fires
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
        ping.runHealthCheck(capturingLog);
        assertEquals(1, callbackCount.get());

        // Stay HEALTHY — no callback
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
        ping.runHealthCheck(capturingLog);
        assertEquals(1, callbackCount.get());

        // UNHEALTHY then recovery — callback fires again
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.UNHEALTHY);
        ping.runHealthCheck(capturingLog);
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
        ping.runHealthCheck(capturingLog);
        assertEquals(2, callbackCount.get());

        // A failed cleanup must be retried even if database health stays HEALTHY.
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR);
        ping.runHealthCheck(capturingLog);
        ping.setOnHealthRestored(() -> {
            throw new IllegalStateException("Induced cache cleanup failure");
        });
        int errorsBeforeCleanup = capturingLog.errors.get();
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
        ping.runHealthCheck(capturingLog);
        assertEquals(errorsBeforeCleanup + 1, capturingLog.errors.get());
        ping.setOnHealthRestored(callbackCount::incrementAndGet);
        ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
        ping.runHealthCheck(capturingLog);
        assertEquals(3, callbackCount.get());
    }

    @Test
    public void testCachesClearedOnHealthRecovery() throws Exception {
        var ping = new ControlledJdbcPing();
        var capturingLog = new CapturingLog();
        boolean initializedProfile = Profile.getInstance() == null;
        if (initializedProfile) {
            Profile.defaults();
        }
        DefaultCacheManager cacheManager = new DefaultCacheManager();
        var clusterFactory = new InfinispanClusterProviderFactory() {
            @Override
            protected int initClusterStartupTime(KeycloakSession session) {
                return 0;
            }
        };
        try {
            cacheManager.defineConfiguration(InfinispanConnectionProvider.WORK_CACHE_NAME,
                    new ConfigurationBuilder().build());
            cacheManager.defineConfiguration(InfinispanConnectionProvider.REALM_CACHE_NAME,
                    new ConfigurationBuilder().clustering().cacheMode(CacheMode.LOCAL).build());
            cacheManager.defineConfiguration(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME,
                    new ConfigurationBuilder().build());
            cacheManager.defineConfiguration(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME,
                    new ConfigurationBuilder().build());

            Cache<String, String> realmCache = cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
            Cache<String, String> sessionCache = cacheManager.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            Cache<String, String> authenticationCache = cacheManager.getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME);

            var connections = new DefaultInfinispanConnectionProvider(cacheManager, new TopologyInfo(cacheManager),
                    new NodeInfo("test-node", null, "test-cluster"));
            var session = (KeycloakSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{KeycloakSession.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getProvider") && args[0] == InfinispanConnectionProvider.class) {
                            return connections;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            clusterFactory.create(session);
            var recoveryListener = new AtomicReference<ProviderEventListener>();
            var sessionFactory = (KeycloakSessionFactory) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{KeycloakSessionFactory.class}, (proxy, method, args) -> {
                        if (method.getName().equals("register")) {
                            recoveryListener.set((ProviderEventListener) args[0]);
                            return null;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            clusterFactory.postInit(sessionFactory);
            ping.setOnHealthRestored(() -> recoveryListener.get().onEvent(new ClusterHealthRestored()));

            realmCache.put("test-realm", "test-data");
            sessionCache.put("test-session", "test-data");
            authenticationCache.put("test-authentication", "test-data");
            ping.setView(new UUID(0, 0));

            // Disconnect: transition to ERROR
            ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR);
            ping.runHealthCheck(capturingLog);
            assertEquals("Cache should not be cleared during disconnect", 1, realmCache.size());
            assertEquals(1, sessionCache.size());

            // Recovery must synchronously clear database-backed caches, but not in-memory authentication sessions.
            ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
            ping.runHealthCheck(capturingLog);
            assertEquals("Cache should be cleared after health recovery", 0, realmCache.size());
            assertEquals(0, sessionCache.size());
            assertEquals(1, authenticationCache.size());

            // Populate again, stay HEALTHY — cache should NOT be cleared
            realmCache.put("new-key", "new-value");
            sessionCache.put("new-session", "new-value");
            ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
            ping.runHealthCheck(capturingLog);
            assertEquals("Cache should not be cleared when health is stable", 1, realmCache.size());
            assertEquals(1, sessionCache.size());
        } finally {
            clusterFactory.close();
            cacheManager.stop();
            if (initializedProfile) {
                Profile.reset();
            }
        }
    }

    /** Minimal {@link Log} that counts error and info calls; everything else is a no-op. */
    static class CapturingLog implements Log {
        final AtomicInteger errors = new AtomicInteger();
        final AtomicInteger warns  = new AtomicInteger();
        final AtomicInteger infos  = new AtomicInteger();

        @Override public void error(String msg, Object... args) { errors.incrementAndGet(); }
        @Override public void error(String msg) { errors.incrementAndGet(); }
        @Override public void error(String msg, Throwable t) { errors.incrementAndGet(); }
        @Override public void info(String msg, Object... args) { infos.incrementAndGet(); }
        @Override public void info(String msg) { infos.incrementAndGet(); }
        @Override public void warn(String msg, Object... args) { warns.incrementAndGet(); }
        @Override public void warn(String msg) { warns.incrementAndGet(); }
        @Override public void warn(String msg, Throwable t) { warns.incrementAndGet(); }
        @Override public void debug(String msg, Object... args) {}
        @Override public void debug(String msg) {}
        @Override public void debug(String msg, Throwable t) {}
        @Override public void trace(Object msg) {}
        @Override public void trace(String msg) {}
        @Override public void trace(String msg, Object... args) {}
        @Override public void trace(String msg, Throwable t) {}
        @Override public void fatal(String msg) {}
        @Override public void fatal(String msg, Object... args) {}
        @Override public void fatal(String msg, Throwable t) {}
        @Override public void setLevel(String level) {}
        @Override public String getLevel() { return "info"; }
        @Override public boolean isFatalEnabled() { return false; }
        @Override public boolean isErrorEnabled() { return true; }
        @Override public boolean isWarnEnabled()  { return true; }
        @Override public boolean isInfoEnabled()  { return true; }
        @Override public boolean isDebugEnabled() { return false; }
        @Override public boolean isTraceEnabled() { return false; }
    }

    protected record Connector(CountDownLatch latch, JChannel ch) implements Runnable {
        @Override
        public void run() {
            try {
                latch.await();
                ch.connect(CLUSTER);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
