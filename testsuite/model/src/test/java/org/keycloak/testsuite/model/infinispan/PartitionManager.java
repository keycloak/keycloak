package org.keycloak.testsuite.model.infinispan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.TestingUtil;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.MergeView;
import org.jgroups.View;
import org.jgroups.protocols.DISCARD;
import org.jgroups.protocols.Discovery;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.MutableDigest;
import org.junit.Assert;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

import static org.infinispan.test.TestingUtil.blockUntilViewsReceived;
import static org.infinispan.test.TestingUtil.waitForNoRebalance;

public class PartitionManager {

    private static final Logger log = Logger.getLogger(PartitionManager.class);

    final EmbeddedCacheManager[] cacheManagers;
    final AtomicInteger viewId;
    volatile Partition[] partitions;

    public PartitionManager(int numberOfCacheManagers) {
        this.cacheManagers = new EmbeddedCacheManager[numberOfCacheManagers];
        this.viewId = new AtomicInteger(5);
    }

    public void addManager(int index, EmbeddedCacheManager cacheManager) {
        this.cacheManagers[index] = cacheManager;
    }

    public void splitCluster(int[]... parts) {
        partitions = new Partition[parts.length];
        for (int i = 0; i < parts.length; i++) {
            Partition p = new Partition();
            for (int j : parts[i]) {
                p.addNode(cacheManagers[j]);
            }
            partitions[i] = p;
            p.discardOtherMembers();
        }
        // Only install the new views after installing DISCARD
        // Otherwise broadcasts from the first partition would be visible in the other partitions
        for (Partition p : partitions) {
            p.partition();
        }
        for (var p : partitions) {
            waitForNoRebalanceInClusteredCaches(p.cacheManagerList);
        }
    }

    public void merge(int p1, int p2) {
        var partition = partitions[p1];
        partition.merge(partitions[p2]);
        List<Partition> tmp = new ArrayList<>(Arrays.asList(this.partitions));
        if (!tmp.remove(partition)) throw new AssertionError();
        this.partitions = tmp.toArray(new Partition[0]);
    }

    private static Address addressOf(EmbeddedCacheManager cm) {
        return TestingUtil.extractJChannel(cm).getAddress();
    }

    private static GMS gmsProtocolOf(EmbeddedCacheManager cm) {
        return TestingUtil.extractJChannel(cm).getProtocolStack().findProtocol(GMS.class);
    }

    private static STABLE stableProtocolOf(EmbeddedCacheManager cm) {
        return TestingUtil.extractJChannel(cm).getProtocolStack().findProtocol(STABLE.class);
    }

    private static void waitForNoRebalanceInClusteredCaches(List<EmbeddedCacheManager> cacheManagers) {
        for (var cacheName : List.of(WORK_CACHE_NAME, AUTHENTICATION_SESSIONS_CACHE_NAME, ACTION_TOKEN_CACHE, LOGIN_FAILURE_CACHE_NAME)) {
            var caches = cacheManagers.stream()
                    .map(cm -> cm.getCache(cacheName))
                    .toList();
            blockUntilViewsReceived(10000, caches);
            waitForNoRebalance(caches);
        }
    }

    private class Partition {
        final List<EmbeddedCacheManager> cacheManagerList = new ArrayList<>();

        public void addNode(EmbeddedCacheManager c) {
            cacheManagerList.add(c);
        }

        public void partition() {
            log.info("Partition forming");
            disableDiscovery();
            installNewView();
            assertPartitionFormed();
            log.info("New views installed");
        }

        private void disableDiscovery() {
            getPartitionChannels().forEach(c ->
                    c.getProtocolStack().<Discovery>findProtocol(Discovery.class).setClusterName(c.getAddressAsString())
            );
        }

        private void assertPartitionFormed() {
            var viewMembers = getPartitionAddresses();
            getPartitionChannels().forEach(c -> {
                var members = c.getView().getMembers();
                Assert.assertEquals(members, viewMembers);
            });
        }

        private void installNewView() {
            var viewMembers = getPartitionAddresses();
            var view = View.create(addressOf(cacheManagerList.get(0)), viewId.incrementAndGet(),
                    viewMembers.toArray(new Address[0]));

            log.info("Before installing new view...");
            cacheManagerList.stream()
                    .map(PartitionManager::gmsProtocolOf)
                    .forEach(p -> p.installView(view));
        }

        private void installMergeView(List<JChannel> view1, List<JChannel> view2) {
            var allAddresses = Stream.concat(view1.stream(), view2.stream())
                    .map(JChannel::getAddress)
                    .distinct()
                    .toList();

            View v1 = toView(view1);
            View v2 = toView(view2);
            List<View> allViews = new ArrayList<>();
            allViews.add(v1);
            allViews.add(v2);

            // Remove all sent NAKACK2 messages to reproduce ISPN-9291
            cacheManagerList.stream()
                    .map(PartitionManager::stableProtocolOf)
                    .forEach(STABLE::gc);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted.");
                return;
            }

            MergeView mv = new MergeView(view1.get(0).getAddress(), viewId.incrementAndGet(), allAddresses, allViews);
            // Compute the merge digest, without it nodes would request the retransmission of all messages
            // Including those that were removed by STABLE earlier
            MutableDigest digest = new MutableDigest(allAddresses.toArray(new Address[0]));
            var gmsList = cacheManagerList.stream()
                    .map(PartitionManager::gmsProtocolOf)
                    .toList();
            gmsList.stream()
                    .map(GMS::getDigest)
                    .forEach(digest::merge);
            gmsList.forEach(gms -> gms.installView(mv, digest));
        }

        private View toView(List<JChannel> channels) {
            final List<Address> viewMembers = new ArrayList<>();
            for (JChannel c : channels) viewMembers.add(c.getAddress());
            return View.create(channels.get(0).getAddress(), viewId.incrementAndGet(),
                    viewMembers.toArray(new Address[0]));
        }

        private void discardOtherMembers() {
            List<Address> outsideMembers = new ArrayList<>();
            for (var cm : cacheManagers) {
                var a = addressOf(cm);
                boolean inThisPartition = false;
                for (var otherCm : cacheManagerList) {
                    if (addressOf(otherCm).equals(a)) inThisPartition = true;
                }
                if (!inThisPartition) outsideMembers.add(a);
            }
            for (var c : getPartitionChannels()) {
                DISCARD discard = new DISCARD();
                log.infof("%s discarding messages from %s", c.getAddress(), outsideMembers);
                for (Address a : outsideMembers) discard.addIgnoreMember(a);
                try {
                    c.getProtocolStack().insertProtocol(discard, ProtocolStack.Position.ABOVE, TP.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void merge(Partition partition) {
            log.infof("Merging partition. %s and %s", this, partition);
            observeMembers(partition);
            partition.observeMembers(this);
            var view1 = getPartitionChannels();
            var view2 = partition.getPartitionChannels();
            partition.cacheManagerList.stream().filter(c -> !cacheManagerList.contains(c)).forEach(cacheManagerList::add);
            installMergeView(view1, view2);
            enableDiscovery();
            waitForNoRebalanceInClusteredCaches(cacheManagerList);
        }

        public void enableDiscovery() {
            getPartitionChannels().forEach(c -> {
                try {
                    String defaultClusterName = TransportConfiguration.CLUSTER_NAME.getDefaultValue();
                    ((Discovery) c.getProtocolStack().findProtocol(Discovery.class)).setClusterName(defaultClusterName);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            log.info("Discovery started.");
        }

        private void observeMembers(Partition partition) {
            for (JChannel c : getPartitionChannels()) {
                var discard = c.getProtocolStack().<DISCARD>findProtocol(DISCARD.class);
                partition.getPartitionAddresses().forEach(discard::removeIgnoredMember);
            }
        }

        private List<Address> getPartitionAddresses() {
            return getPartitionChannels().stream()
                    .map(JChannel::getAddress)
                    .toList();
        }

        private List<JChannel> getPartitionChannels() {
            return cacheManagerList.stream()
                    .map(TestingUtil::extractJChannel)
                    .toList();
        }


        @Override
        public String toString() {
            return "Partition{" + getPartitionAddresses() +'}';
        }
    }
}
