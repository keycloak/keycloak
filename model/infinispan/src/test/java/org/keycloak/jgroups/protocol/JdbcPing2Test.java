package org.keycloak.jgroups.protocol;

import org.jboss.logging.Logger;
import org.jgroups.JChannel;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.ThreadFactory;
import org.jgroups.util.Util;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Misc tests for {@link KEYCLOAK_JDBC_PING2}, running against H2
 * @author Bela Ban
 * @author Alexander Schwartz
 */
public class JdbcPing2Test {
    protected static Logger log = Logger.getLogger(JdbcPing2Test.class);

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

    private static long runSingleTest() throws Exception {
        JChannel[] channels = new JChannel[NUM_NODES];
        List<Thread> threads = new ArrayList<>();
        try {
            for (int i = 0; i < channels.length; i++) {
                channels[i] = createChannel(PROTOCOL_STACK, String.valueOf(i + 1));
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

    protected static JChannel createChannel(String cfg, String name) throws Exception {
        return new JChannel(cfg).name(name);
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
