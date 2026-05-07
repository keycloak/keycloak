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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.keycloak.common.util.Time;
import org.keycloak.infinispan.health.impl.JdbcPingClusterHealthImpl;

import org.hamcrest.CoreMatchers;
import org.infinispan.util.concurrent.WithinThreadExecutor;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.PingData;
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
        clusterHealth.init(ping, new WithinThreadExecutor());
        var addresses = IntStream.range(0, 2)
                .mapToObj(operand -> new UUID(0, operand))
                .sorted()
                .toArray(Address[]::new);

        // test exception
        ping.setException(new RuntimeException("Induced"));
        assertEquals(KEYCLOAK_JDBC_PING2.HealthStatus.ERROR, ping.healthStatus());
        // A database exception do not change the cluster health.
        // It relies on Quarkus database health check to mark the Keycloak instance as not ready.
        clusterHealth.triggerClusterHealthCheck();
        assertTrue(clusterHealth.isHealthy());
        // Remove exception
        ping.setException(null);

        // test empty table / no coordinator
        ping.setView(addresses[0]);
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
