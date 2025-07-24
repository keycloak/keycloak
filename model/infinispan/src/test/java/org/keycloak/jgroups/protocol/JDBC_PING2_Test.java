package org.keycloak.jgroups.protocol;

import org.jgroups.JChannel;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.util.ThreadFactory;
import org.jgroups.util.Util;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Misc tests for {@link org.jgroups.protocols.JDBC_PING2}, running against H2
 * @author Bela Ban
 * @since  5.4, 5.3.7
 */
public class JDBC_PING2_Test {
    protected static final String CLUSTER="jdbc-test";
    protected static final int NUM_NODES=8;
    public static final String PROTOCOAL_STACK = "jdbc-h2.xml";

    static {
        ClassConfigurator.addProtocol((short) 1026, KEYCLOAK_JDBC_PING2_FOR_TESTING.class);
    }

    @Test
    public void testClusterFormedAfterRestart() throws Exception {
        try(var a=createChannel("jdbc-h2.xml", "A")) {
            a.connect(CLUSTER);
            for(int i=1; i <= 10; i++) {
                long start=System.nanoTime();
                try(var b=createChannel("jdbc-h2.xml", "B")) {
                    b.connect(CLUSTER);
                    Util.waitUntilAllChannelsHaveSameView(10000, 10, a,b);
                    long time=System.nanoTime()-start;
                    System.out.printf("-- join #%d took %s\n", i, Util.printTime(time, TimeUnit.NANOSECONDS));
                }
            }
        }
    }

    /**
     * 100 iterations would run approx 4 minutes and should complete successfully.
     */
    @Test
    @Ignore
    public void testConcurrentStartupMultipleTimes() throws Exception {
        for (int j = 0; j < 100; j++) {
            runSingleTest();
        }
    }

    /**
     * 100 iterations would run approx 4 minutes and should complete successfully.
     */
    @Test
    public void testConcurrentStartup() throws Exception {
        runSingleTest();
    }

    private static void runSingleTest() throws Exception {
        JChannel[] channels=new JChannel[NUM_NODES];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = createChannel(PROTOCOAL_STACK, String.valueOf(i + 1));
        }
        CountDownLatch latch=new CountDownLatch(1);
        int index=1;
        List<Thread> threads=new ArrayList<>();
        for (JChannel ch : channels) {
            ThreadFactory thread_factory = ch.stack().getTransport().getThreadFactory();
            Connector connector = new Connector(latch, ch);
            Thread thread = thread_factory.newThread(connector, "connector-" + index++);
            threads.add(thread);
            thread.start();
        }
        latch.countDown();
        long start = System.nanoTime();
        Util.waitUntilAllChannelsHaveSameView(20000, 100, channels);
        long time = System.nanoTime() - start;
        System.out.printf("-- cluster of %d formed in %s:\n%s\n", NUM_NODES, Duration.ofNanos(time),
                Stream.of(channels).map(ch -> String.format("%s: %s", ch.address(), ch.view()))
                        .collect(Collectors.joining("\n")));
        Arrays.stream(channels).filter(ch -> ch.view().getCoord() != ch.getAddress()).forEach(JChannel::close);
        Arrays.stream(channels).filter(ch -> !ch.isClosed()).forEach(JChannel::close);
        System.out.println("Closed");
    }

    protected static JChannel modify(JChannel ch) {
        GMS gms=ch.stack().findProtocol(GMS.class);
        gms.setJoinTimeout(3000).setMaxJoinAttempts(5);
        return ch;
    }

    protected static JChannel createChannel(String cfg, String name) throws Exception {
        return modify(new JChannel(cfg).name(name));
    }

    protected static class Connector implements Runnable {
        protected final CountDownLatch latch;
        protected final JChannel       ch;

        protected Connector(CountDownLatch latch, JChannel ch) {
            this.latch=latch;
            this.ch=ch;
        }

        @Override
        public void run() {
            try {
                latch.await();
                ch.connect(CLUSTER);
            }
            catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
