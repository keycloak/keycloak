/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.jgroups.protocol;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.keycloak.common.util.Time;

import org.jgroups.Address;
import org.jgroups.BytesMessage;
import org.jgroups.EmptyMessage;
import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.protocols.PingData;
import org.jgroups.protocols.PingHeader;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.Protocol;
import org.jgroups.util.MessageBatch;
import org.jgroups.util.TimeScheduler3;
import org.jgroups.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JdbcPingQuarantineTest {
    private final Address local = UUID.randomUUID();
    private final Address remote = UUID.randomUUID();
    private final KEYCLOAK_JDBC_PING2 gate = new KEYCLOAK_JDBC_PING2();
    private final RecordingProtocol upper = new RecordingProtocol();
    private final RecordingProtocol lower = new RecordingProtocol();

    public JdbcPingQuarantineTest() {
        gate.setAddress(local);
        gate.setId((short) 1801);
        gate.setUpProtocol(upper);
        gate.setDownProtocol(lower);
    }

    @Test
    public void testRemoteTrafficIsBlockedOnlyDuringQuarantine() {
        Message outbound = new EmptyMessage(remote);
        Message inbound = new EmptyMessage(local).setSrc(remote);
        gate.down(outbound);
        gate.up(inbound);
        assertEquals(List.of(outbound), lower.messages);
        assertEquals(List.of(inbound), upper.messages);

        gate.setQuarantined(true);
        gate.down(outbound);
        gate.up(inbound);
        assertEquals(1, lower.messages.size());
        assertEquals(1, upper.messages.size());

        gate.setQuarantined(false);
        gate.down(outbound);
        gate.up(inbound);
        assertEquals(2, lower.messages.size());
        assertEquals(2, upper.messages.size());
    }

    @Test
    public void testLocalUnicastAndMulticastLoopbackRemainAvailable() {
        gate.setQuarantined(true);
        Message unicast = new EmptyMessage(local).setSrc(local);
        gate.down(unicast);
        gate.up(unicast);
        Message multicast = new EmptyMessage(null);
        gate.down(multicast);
        assertEquals(List.of(unicast), lower.messages);
        assertEquals(List.of(unicast, multicast), upper.messages);
        assertEquals(local, multicast.getSrc());

        gate.down(new EmptyMessage(null).setFlag(Message.TransientFlag.DONT_LOOPBACK));
        assertEquals(2, upper.messages.size());

        // Loopback must still pass through Discovery instead of leaking its protocol messages up the stack.
        gate.down(new BytesMessage(null).putHeader(gate.getId(), new PingHeader(PingHeader.GET_MBRS_RSP))
                .setArray(gate.marshal(List.of(new PingData(local, true)))));
        assertEquals(2, upper.messages.size());
    }

    @Test
    public void testBatchDropsRemoteMessagesAndPreservesLocalMessages() {
        gate.setQuarantined(true);
        Message localMessage = new EmptyMessage(local).setSrc(local);
        gate.up(new MessageBatch(List.of(new EmptyMessage(local).setSrc(remote), localMessage)));
        assertEquals(List.of(localMessage), upper.messages);
        gate.up(new MessageBatch(List.of(new EmptyMessage(local).setSrc(remote))));
        assertEquals(1, upper.messages.size());
    }

    @Test
    public void testDiscoveryResponsesUseTheSameGate() throws Exception {
        var physicalAddress = new IpAddress("127.0.0.1", 9000);
        var members = List.of(new PingData(local, true, "local", physicalAddress));
        gate.sendDiscoveryResponse(local, physicalAddress, "local", remote, true);
        gate.sendDiscoveryResponse(members, remote);
        assertEquals(2, lower.messages.size());

        gate.setQuarantined(true);
        gate.sendDiscoveryResponse(local, physicalAddress, "local", remote, true);
        gate.sendDiscoveryResponse(members, remote);
        assertEquals(2, lower.messages.size());
    }

    @Test
    public void testDelayedDiscoveryResponsesRecheckQuarantine() throws Exception {
        var timer = new ControlledScheduler();
        try {
            gate.setValue("timer", timer);
            gate.setValue("stagger_timeout", 100L);
            var physicalAddress = new IpAddress("127.0.0.1", 9000);
            gate.sendDiscoveryResponse(local, physicalAddress, "local", remote, true);
            gate.sendDiscoveryResponse(List.of(new PingData(local, true, "local", physicalAddress)), remote);
            assertEquals(2, timer.tasks.size());
            assertTrue(lower.messages.isEmpty());
            gate.setQuarantined(true);
            timer.tasks.forEach(Runnable::run);
            assertTrue(lower.messages.isEmpty());
        } finally {
            timer.stop();
        }
    }

    @Test
    public void testProtocolEventsAreNotBlocked() {
        gate.setQuarantined(true);
        Event event = new Event(Event.GET_PHYSICAL_ADDRESS, local);
        assertSame(event, gate.down(event));
        assertSame(event, gate.up(event));
        assertTrue(upper.messages.isEmpty());
        assertTrue(lower.messages.isEmpty());
    }

    @Test
    public void testSuccessfulInFlightPublicationPreventsQuarantine() throws Exception {
        var publicationStarted = new CountDownLatch(1);
        var finishPublication = new CountDownLatch(1);
        var healthCheckStarted = new CountDownLatch(1);
        var ping = new ControlledJdbcPing() {
            @Override
            protected Connection getConnection() throws SQLException {
                publicationStarted.countDown();
                try {
                    if (!finishPublication.await(5, TimeUnit.SECONDS)) {
                        throw new SQLException("Timed out waiting to finish publication");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException(e);
                }
                return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                            case "getAutoCommit" -> true;
                            case "setAutoCommit", "commit", "close" -> null;
                            default -> throw new UnsupportedOperationException(method.getName());
                        });
            }

            @Override
            protected void insert(Connection connection, PingData data, String cluster) {
            }

            @Override
            protected void delete(Connection connection, String cluster, Address address) {
            }

            @Override
            protected HealthStatus queryHealthStatus() {
                healthCheckStarted.countDown();
                return HealthStatus.HEALTHY;
            }
        };
        ping.setAddress(local);
        ping.setValue("view", View.create(local, 1, local, remote));
        ping.setValue("is_coord", true);
        ping.setValue("coordinatorWriteFailureSince", Time.currentTimeMillis() - 80000);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var publication = executor.submit(() -> {
                ping.writeToDB(new PingData(local, true).coord(true), "test");
                return null;
            });
            assertTrue(publicationStarted.await(5, TimeUnit.SECONDS));
            var healthCheck = executor.submit(() -> ping.runHealthCheck(new JdbcPing2Test.CapturingLog()));
            assertTrue(healthCheckStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> healthCheck.get(100, TimeUnit.MILLISECONDS));
            finishPublication.countDown();
            publication.get(5, TimeUnit.SECONDS);
            healthCheck.get(5, TimeUnit.SECONDS);
            assertFalse(ping.isQuarantined());
        } finally {
            finishPublication.countDown();
            executor.shutdownNow();
        }
    }

    private static class ControlledScheduler extends TimeScheduler3 {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public Future<?> schedule(Runnable task, long delay, TimeUnit unit, boolean canBlock) {
            tasks.add(task);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class RecordingProtocol extends Protocol {
        private final List<Message> messages = new ArrayList<>();

        @Override
        public Object up(Message message) {
            messages.add(message);
            return null;
        }

        @Override
        public void up(MessageBatch batch) {
            messages.addAll(batch.stream().toList());
        }

        @Override
        public Object down(Message message) {
            messages.add(message);
            return null;
        }

        @Override
        public Object up(Event event) {
            return event;
        }

        @Override
        public Object down(Event event) {
            return event;
        }
    }
}
