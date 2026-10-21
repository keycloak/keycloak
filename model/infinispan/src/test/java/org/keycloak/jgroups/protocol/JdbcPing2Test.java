package org.keycloak.jgroups.protocol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.keycloak.cluster.infinispan.InfinispanClusterProvider;
import org.keycloak.cluster.infinispan.InfinispanClusterProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.NodeInfo;
import org.keycloak.infinispan.health.impl.JdbcPingClusterHealthImpl;
import org.keycloak.models.cache.infinispan.ClearCacheEvent;

import org.hamcrest.CoreMatchers;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.util.concurrent.WithinThreadExecutor;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.logging.Log;
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

import static org.awaitility.Awaitility.await;
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
    public void testConcurrentStartup() throws Exception {
        runSingleTest();
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

    @Test
    public void testMembersPublishOnlyTheirOwnEntry() throws Exception {
        try (JChannel a = createChannel("A"); JChannel b = createChannel("B")) {
            a.connect(CLUSTER);
            b.connect(CLUSTER);
            Util.waitUntilAllChannelsHaveSameView(5000, 20, a, b);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = a.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            KEYCLOAK_JDBC_PING2_FOR_TESTING member = b.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);

            member.writeAll();
            var entries = member.readFromDB(CLUSTER);
            assertEquals(2, entries.size());
            assertTrue(entries.stream().anyMatch(p -> p.getAddress().equals(a.getAddress()) && p.isCoord()));
            assertTrue(entries.stream().anyMatch(p -> p.getAddress().equals(b.getAddress()) && !p.isCoord()));

            setLastUpdate(member, b.getAddress(), Time.currentTime() - 120);
            coordinator.writeAll();
            assertEquals(List.of(a.getAddress()), coordinator.readFromDB(CLUSTER).stream().map(PingData::getAddress).toList());
            member.writeAll();
            assertEquals(2, member.readDiscoveryData(CLUSTER).partitions().get(a.getAddress()).size());
        }
    }

    @Test
    public void testDatabaseTaskOwnershipFallsBackToLowestFreshMember() throws Exception {
        try (JChannel a = createChannel("A"); JChannel b = createChannel("B"); JChannel c = createChannel("C")) {
            a.addAddressGenerator(() -> new UUID(0, 3));
            b.addAddressGenerator(() -> new UUID(0, 1));
            c.addAddressGenerator(() -> new UUID(0, 2));
            a.connect(CLUSTER);
            b.connect(CLUSTER);
            c.connect(CLUSTER);
            Util.waitUntilAllChannelsHaveSameView(5000, 20, a, b, c);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = a.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            KEYCLOAK_JDBC_PING2_FOR_TESTING first = b.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            KEYCLOAK_JDBC_PING2_FOR_TESTING second = c.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            View originalView = a.getView();

            assertTrue(coordinator.isDatabaseTaskCoordinator());
            assertFalse(first.isDatabaseTaskCoordinator());
            assertFalse(second.isDatabaseTaskCoordinator());

            setLastUpdate(first, a.getAddress(), Time.currentTime() - 120);
            assertFalse(coordinator.isDatabaseTaskCoordinator());
            assertTrue(first.isDatabaseTaskCoordinator());
            assertFalse(second.isDatabaseTaskCoordinator());

            setLastUpdate(second, b.getAddress(), Time.currentTime() - 120);
            assertFalse(first.isDatabaseTaskCoordinator());
            assertTrue(second.isDatabaseTaskCoordinator());

            coordinator.writeAll();
            assertTrue(coordinator.isDatabaseTaskCoordinator());
            assertFalse(first.isDatabaseTaskCoordinator());
            assertFalse(second.isDatabaseTaskCoordinator());
            assertEquals(originalView, a.getView());
            assertEquals(originalView, b.getView());
            assertEquals(originalView, c.getView());
        }
    }

    @Test
    public void testMemberStaysReadyWithoutCoordinatorDatabaseAccess() throws Exception {
        try (JChannel a = createChannel("A"); JChannel b = createChannel("B")) {
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = a.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            KEYCLOAK_JDBC_PING2_FOR_TESTING member = b.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            member.staleness_timeout = 2000;
            member.setInfoWriterSleepTime(1);
            a.connect(CLUSTER);
            b.connect(CLUSTER);
            Util.waitUntilAllChannelsHaveSameView(5000, 20, a, b);
            View originalView = a.getView();
            var coordinatorHealth = new JdbcPingClusterHealthImpl();
            var memberHealth = new JdbcPingClusterHealthImpl();
            coordinatorHealth.init(coordinator, new WithinThreadExecutor());
            memberHealth.init(member, new WithinThreadExecutor());
            assertTrue(coordinatorHealth.isHealthy());
            assertTrue(memberHealth.isHealthy());

            try {
                coordinator.setDatabaseAvailable(false);
                setLastUpdate(member, a.getAddress(), Time.currentTime() - 120);
                setLastUpdate(member, b.getAddress(), Time.currentTime() - 120);
                coordinatorHealth.triggerClusterHealthCheck();
                assertFalse(coordinatorHealth.isHealthy());

                // No explicit write: the participant's periodic writer must restore its fresh entry.
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                    memberHealth.triggerClusterHealthCheck();
                    assertTrue(memberHealth.isHealthy());
                    assertEquals(List.of(b.getAddress()), member.readFromDB(CLUSTER).stream().map(PingData::getAddress).toList());
                });
                assertTrue(member.isDatabaseTaskCoordinator());
                long firstPublication = lastUpdate(member, b.getAddress());
                await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                        assertTrue(lastUpdate(member, b.getAddress()) > firstPublication));
                assertEquals(originalView, a.getView());
                assertEquals(originalView, b.getView());

                member.setDatabaseAvailable(false);
                memberHealth.triggerClusterHealthCheck();
                assertFalse(memberHealth.isHealthy());
                assertFalse(member.isDatabaseTaskCoordinator());
                member.setDatabaseAvailable(true);
                member.writeAll();
                memberHealth.triggerClusterHealthCheck();
                assertTrue(memberHealth.isHealthy());

                coordinator.setDatabaseAvailable(true);
                coordinator.writeAll();
                coordinatorHealth.triggerClusterHealthCheck();
                assertTrue(coordinatorHealth.isHealthy());
                assertTrue(coordinator.isDatabaseTaskCoordinator());
                assertFalse(member.isDatabaseTaskCoordinator());
            } finally {
                coordinator.setDatabaseAvailable(true);
                member.setDatabaseAvailable(true);
            }
        }
    }

    @Test
    public void testNewPartitionLosesAndMergesAfterCoordinatorRecovers() throws Exception {
        try (JChannel a = createChannel("A"); JChannel b = createChannel("B"); JChannel c = createChannel("C")) {
            a.addAddressGenerator(() -> new UUID(0, 1));
            b.addAddressGenerator(() -> new UUID(0, 2));
            c.addAddressGenerator(() -> new UUID(0, 3));
            a.connect(CLUSTER);
            b.connect(CLUSTER);
            Util.waitUntilAllChannelsHaveSameView(5000, 20, a, b);
            View originalView = a.getView();
            for (JChannel channel : List.of(a, b)) {
                MERGE3 merge = channel.getProtocolStack().findProtocol(MERGE3.class);
                merge.stop();
            }
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = a.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            KEYCLOAK_JDBC_PING2_FOR_TESTING member = b.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            try {
                coordinator.setDatabaseAvailable(false);
                setLastUpdate(member, a.getAddress(), Time.currentTime() - 120);
                member.writeAll();
                GMS gms = c.getProtocolStack().findProtocol(GMS.class);
                gms.setJoinTimeout(100).setMaxJoinAttempts(1);
                c.connect(CLUSTER);
                MERGE3 merge = c.getProtocolStack().findProtocol(MERGE3.class);
                merge.stop();
                KEYCLOAK_JDBC_PING2_FOR_TESTING joiner = c.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
                assertEquals(1, c.getView().size());
                assertEquals(originalView, a.getView());
                assertEquals(originalView, b.getView());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, member.healthStatus());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.UNHEALTHY, joiner.healthStatus());
                assertTrue(member.isDatabaseTaskCoordinator());
                assertFalse(joiner.isDatabaseTaskCoordinator());

                coordinator.setDatabaseAvailable(true);
                coordinator.writeAll();
                await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                    for (JChannel channel : List.of(a, b, c)) {
                        MERGE3 protocol = channel.getProtocolStack().findProtocol(MERGE3.class);
                        protocol.sendInfo();
                    }
                    for (JChannel channel : List.of(a, b, c)) {
                        MERGE3 protocol = channel.getProtocolStack().findProtocol(MERGE3.class);
                        protocol.checkInconsistencies();
                    }
                    assertEquals(3, a.getView().size());
                    assertEquals(a.getView(), b.getView());
                    assertEquals(a.getView(), c.getView());
                });
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, member.healthStatus());
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, joiner.healthStatus());
            } finally {
                coordinator.setDatabaseAvailable(true);
            }
        }
    }

    @Test
    public void testPartitionRankingWithoutCoordinatorEntry() throws Exception {
        try (JChannel channel = createChannel("A")) {
            Address a = new UUID(0, 1);
            Address b = new UUID(0, 2);
            Address c = new UUID(0, 3);
            Address d = new UUID(0, 4);
            channel.addAddressGenerator(() -> a);
            channel.connect(CLUSTER);
            KEYCLOAK_JDBC_PING2_FOR_TESTING ping = channel.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            setLastUpdate(ping, a, Time.currentTime() - 120);
            insertEntry(ping, b, false, a);
            insertEntry(ping, c, true, c);
            try {
                // B is the only fresh witness of A's partition; its tie with C is won by A's lower address.
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, ping.healthStatus());
                assertEquals(List.of(c), ping.readFromDB(CLUSTER).stream()
                        .filter(PingData::isCoord).map(PingData::getAddress).toList());
                ping.removeStaleEntries();
                assertFalse(ping.readFromDB(CLUSTER).stream().anyMatch(p -> p.getAddress().equals(a)));
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, ping.healthStatus());

                insertEntry(ping, d, false, c);
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.UNHEALTHY, ping.healthStatus());
                setLastUpdate(ping, d, Time.currentTime() - 120);
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, ping.healthStatus());
                setLastUpdate(ping, b, Time.currentTime() - 120);
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.UNHEALTHY, ping.healthStatus());
                setLastUpdate(ping, c, Time.currentTime() - 120);
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.NO_COORDINATOR, ping.healthStatus());
            } finally {
                try (Connection conn = ping.getConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM jgroups WHERE address != ? AND cluster = ?")) {
                    ps.setString(1, Util.addressToString(a));
                    ps.setString(2, CLUSTER);
                    ps.executeUpdate();
                }
            }
        }
    }

    @Test
    public void testStartingEntryDoesNotCreateAPartition() throws Exception {
        try (JChannel channel = createChannel("A")) {
            channel.connect(CLUSTER);
            KEYCLOAK_JDBC_PING2_FOR_TESTING ping = channel.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            Address starting = new UUID(0, 0);
            insertEntry(ping, starting, false, null);
            try {
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY, ping.healthStatus());
                setLastUpdate(ping, channel.getAddress(), Time.currentTime() - 120);
                assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.NO_COORDINATOR, ping.healthStatus());
                assertFalse(ping.isDatabaseTaskCoordinator());
            } finally {
                try (Connection conn = ping.getConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM jgroups WHERE address = ?")) {
                    ps.setString(1, Util.addressToString(starting));
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void setLastUpdate(KEYCLOAK_JDBC_PING2_FOR_TESTING ping, Address address, long timestamp) throws Exception {
        try (Connection conn = ping.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE jgroups SET last_update = ? WHERE address = ?")) {
            ps.setLong(1, timestamp);
            ps.setString(2, Util.addressToString(address));
            assertEquals(1, ps.executeUpdate());
        }
    }

    private static long lastUpdate(KEYCLOAK_JDBC_PING2_FOR_TESTING ping, Address address) throws Exception {
        try (Connection conn = ping.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT last_update FROM jgroups WHERE address = ?")) {
            ps.setString(1, Util.addressToString(address));
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getLong(1);
            }
        }
    }

    private static void insertEntry(KEYCLOAK_JDBC_PING2_FOR_TESTING ping, Address address, boolean coordinator,
                                   Address coordinatedBy) throws Exception {
        try (Connection conn = ping.getConnection();
             PreparedStatement ps = conn.prepareStatement(ping.getInsertSingleSql())) {
            ps.setString(1, Util.addressToString(address));
            ps.setString(2, address.toString());
            ps.setString(3, CLUSTER);
            ps.setString(4, "127.0.0.1:9999");
            ps.setBoolean(5, coordinator);
            ps.setLong(6, Time.currentTime());
            ps.setString(7, coordinatedBy == null ? null : Util.addressToString(coordinatedBy));
            ps.executeUpdate();
        }
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
    public void testOnlyCoordinatorClearsStaleEntriesOnViewChange() throws Exception {
        try (JChannel a = createChannel("A"); JChannel b = createChannel("B")) {
            a.connect(CLUSTER);
            b.connect(CLUSTER);
            Util.waitUntilAllChannelsHaveSameView(5000, 20, a, b);
            KEYCLOAK_JDBC_PING2_FOR_TESTING coordinator = a.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            KEYCLOAK_JDBC_PING2_FOR_TESTING member = b.getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
            Address stale = UUID.randomUUID();
            insertEntry(member, stale, false, stale);
            long timestamp = Time.currentTime() - 120;
            setLastUpdate(member, stale, timestamp);

            member.handleView(b.getView(), b.getView(), true);
            assertEquals(timestamp, lastUpdate(member, stale));

            coordinator.handleView(a.getView(), a.getView(), true);
            try (Connection conn = coordinator.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT address FROM jgroups WHERE address = ?")) {
                ps.setString(1, Util.addressToString(stale));
                try (ResultSet rs = ps.executeQuery()) {
                    assertFalse(rs.next());
                }
            }
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
    }

    @Test
    public void testCachesClearedOnHealthRecovery() throws Exception {
        var ping = new ControlledJdbcPing();
        var capturingLog = new CapturingLog();

        DefaultCacheManager cacheManager = new DefaultCacheManager();
        var executor = Executors.newSingleThreadExecutor();
        try {
            cacheManager.defineConfiguration(InfinispanConnectionProvider.WORK_CACHE_NAME,
                    new ConfigurationBuilder().build());
            cacheManager.defineConfiguration(InfinispanConnectionProvider.REALM_CACHE_NAME,
                    new ConfigurationBuilder().clustering().cacheMode(CacheMode.LOCAL).build());

            Cache<String, Object> workCache = cacheManager.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);
            Cache<String, String> realmCache = cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);

            var cp = new InfinispanClusterProvider(0, new NodeInfo("test-node", null, "test-cluster"), workCache, executor);
            workCache.addListener(cp.new CacheEntryListener());
            cp.registerListener(InfinispanClusterProviderFactory.CLEAR_ALL_LOCAL_CACHES_EVENT, event ->
                    Arrays.stream(InfinispanConnectionProvider.LOCAL_CACHE_NAMES)
                            .filter(cacheManager::cacheExists)
                            .map(name -> cacheManager.<String, Object>getCache(name))
                            .filter(cache -> cache.getCacheConfiguration().clustering().cacheMode() == CacheMode.LOCAL)
                            .forEach(Cache::clear)
            );

            realmCache.put("test-realm", "test-data");
            assertEquals(1, realmCache.size());

            ping.setOnHealthRestored(() ->
                    cp.notify(InfinispanClusterProviderFactory.CLEAR_ALL_LOCAL_CACHES_EVENT,
                            ClearCacheEvent.getInstance(), false));

            ping.setView(new UUID(0, 0));

            // Disconnect: transition to ERROR
            ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR);
            ping.runHealthCheck(capturingLog);
            assertEquals("Cache should not be cleared during disconnect", 1, realmCache.size());

            // Reconnect: transition to HEALTHY — event broadcast clears caches on all nodes
            ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
            ping.runHealthCheck(capturingLog);
            assertEquals("Cache should be cleared after health recovery", 0, realmCache.size());

            // Populate again, stay HEALTHY — cache should NOT be cleared
            realmCache.put("new-key", "new-value");
            ping.enqueueStatus(KEYCLOAK_JDBC_PING2.HealthStatus.HEALTHY);
            ping.runHealthCheck(capturingLog);
            assertEquals("Cache should not be cleared when health is stable", 1, realmCache.size());
        } finally {
            executor.shutdownNow();
            cacheManager.stop();
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
