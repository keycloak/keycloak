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

package org.keycloak.models.sessions.infinispan;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.hamcrest.MatcherAssert;
import org.infinispan.Cache;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class SessionAffinityServiceTest {

    private static final Address ADDRESS = Address.random();
    private static final KeyPartitioner KEY_PARTITIONER = key -> {
        Assert.assertTrue(key instanceof Integer);
        return (int) key;
    };

    @Test
    public void testNoOwnership() {
        // no segment belongs to the address, it should not block and return the first key
        // theoretical scenario
        var generator = new KeyGenerator();
        var service = new SessionAffinityService<>(generator, KEY_PARTITIONER, ADDRESS);
        var ch = new ControlledConsistentHash(Set.of(), Set.of());
        service.handleViewChange(new TopologyEventImpl(ch, 1));
        assertThat(service.get(), is(1));
    }

    @Test
    public void testNoPrimaryOwnedSegments() {
        // joining or network partition recovery, nodes can be temporary without being a primary owner of any segment
        var generator = new KeyGenerator();
        var service = new SessionAffinityService<>(generator, KEY_PARTITIONER, ADDRESS);
        var ch = new ControlledConsistentHash(Set.of(), Set.of(10));
        service.handleViewChange(new TopologyEventImpl(ch, 1));
        assertThat(service.get(), is(10));
    }

    @Test
    public void testStableTopology() {
        // healthy cluster
        var generator = new KeyGenerator();
        var service = new SessionAffinityService<>(generator, KEY_PARTITIONER, ADDRESS);
        var ch = new ControlledConsistentHash(Set.of(15), Set.of(10));
        service.handleViewChange(new TopologyEventImpl(ch, 1));
        assertThat(service.get(), is(15));
    }

    @Test
    public void testTopologyEventsOutOfOrder() {
        // older topology should not replace new ones
        // theoretical scenario
        var generator = new KeyGenerator();
        var service = new SessionAffinityService<>(generator, KEY_PARTITIONER, ADDRESS);
        var ch = new ControlledConsistentHash(Set.of(5), Set.of());
        service.handleViewChange(new TopologyEventImpl(ch, 5));
        assertThat(service.get(), is(5));

        // reset
        generator.nextKey = 0;
        ch = new ControlledConsistentHash(Set.of(1), Set.of());
        service.handleViewChange(new TopologyEventImpl(ch, 4));
        assertThat(service.get(), is(5));
    }

    @Test
    public void testSwitchOwnership() {
        // segment switches between primary <-> backup
        // theoretical scenario
        var generator = new KeyGenerator();
        var service = new SessionAffinityService<>(generator, KEY_PARTITIONER, ADDRESS);
        var ch = new ControlledConsistentHash(Set.of(5), Set.of(3));
        service.handleViewChange(new TopologyEventImpl(ch, 1));
        assertThat(service.get(), is(5));

        // reset
        generator.nextKey = 0;
        ch = new ControlledConsistentHash(Set.of(3), Set.of(5));
        service.handleViewChange(new TopologyEventImpl(ch, 2));
        assertThat(service.get(), is(3));
    }

    @Test
    public void testUnluckyGenerator() {
        // the key generator is unable to generate a key belonging to the segment before SessionAffinityService.MAX_ATTEMPTS
        var generator = new KeyGenerator();
        var service = new SessionAffinityService<>(generator, KEY_PARTITIONER, ADDRESS);
        var ch = new ControlledConsistentHash(Set.of(128), Set.of());
        service.handleViewChange(new TopologyEventImpl(ch, 1));
        assertThat(service.get(), is(SessionAffinityService.MAX_ATTEMPTS + 1));
    }

    private static class KeyGenerator implements Supplier<Integer> {

        int nextKey;

        @Override
        public Integer get() {
            return ++nextKey;
        }
    }

    private record TopologyEventImpl(
            ControlledConsistentHash consistentHash,
            int topologyId) implements TopologyChangedEvent<Integer, Object> {
        @Override
        public ConsistentHash getReadConsistentHashAtStart() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConsistentHash getWriteConsistentHashAtStart() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConsistentHash getReadConsistentHashAtEnd() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConsistentHash getWriteConsistentHashAtEnd() {
            return consistentHash;
        }

        @Override
        public int getNewTopologyId() {
            return topologyId;
        }

        @Override
        public Type getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPre() {
            return false;
        }

        @Override
        public Cache<Integer, Object> getCache() {
            throw new UnsupportedOperationException();
        }
    }

    private record ControlledConsistentHash(
            Set<Integer> primarySegments,
            Set<Integer> allSegments) implements ConsistentHash {

        @Override
        public int getNumSegments() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Address> getMembers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Address> locateOwnersForSegment(int segmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Address locatePrimaryOwnerForSegment(int segmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Integer> getSegmentsForOwner(Address owner) {
            MatcherAssert.assertThat(owner, sameInstance(ADDRESS));
            return allSegments;
        }

        @Override
        public Set<Integer> getPrimarySegmentsForOwner(Address owner) {
            MatcherAssert.assertThat(owner, sameInstance(ADDRESS));
            return primarySegments;
        }

        @Override
        public String getRoutingTableAsString() {
            throw new UnsupportedOperationException();
        }
    }
}
