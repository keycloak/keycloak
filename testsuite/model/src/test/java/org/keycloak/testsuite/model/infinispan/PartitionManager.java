package org.keycloak.testsuite.model.infinispan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.Cache;
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
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.MutableDigest;

import static org.infinispan.test.TestingUtil.blockUntilViewsReceived;
import static org.infinispan.test.TestingUtil.waitForNoRebalance;

public class PartitionManager {

   private static final Logger log = Logger.getLogger(PartitionManager.class);

   final int numberOfCacheManagers;
   final Set<String> cacheNames;
   final EmbeddedCacheManager[] cacheManagers;
   final AtomicInteger viewId;
   volatile Partition[] partitions;

   public PartitionManager(int numberOfCacheManagers, Set<String> cacheNames) {
      this.numberOfCacheManagers = numberOfCacheManagers;
      this.cacheNames = cacheNames;
      this.cacheManagers = new EmbeddedCacheManager[2];
      this.viewId = new AtomicInteger(5);
   }

   public void addManager(int index, EmbeddedCacheManager cacheManager) {
      this.cacheManagers[index] = cacheManager;
   }

   public void splitCluster(int[]... parts) {
      List<Address> allMembers = channel(0).getView().getMembers();
      partitions = new Partition[parts.length];
      for (int i = 0; i < parts.length; i++) {
         Partition p = new Partition(viewId, allMembers, getCaches());
         for (int j : parts[i]) {
            p.addNode(channel(j));
         }
         partitions[i] = p;
         p.discardOtherMembers();
      }
      // Only install the new views after installing DISCARD
      // Otherwise broadcasts from the first partition would be visible in the other partitions
      for (Partition p : partitions) {
         p.partition();
      }
   }

   public void merge(int p1, int p2) {
      var partition = partitions[p1];
      partition.merge(partitions[p2]);
      List<Partition> tmp = new ArrayList<>(Arrays.asList(this.partitions));
      if (!tmp.remove(partition)) throw new AssertionError();
      this.partitions = tmp.toArray(new Partition[0]);
   }

   private List<Cache<?, ?>> getCaches() {
      return cacheNames.stream()
            .flatMap(
                  name -> Arrays.stream(cacheManagers).map(m -> m.getCache(name))
            )
            .collect(Collectors.toList());
   }

   private JChannel channel(int index) {
      return TestingUtil.extractJChannel(cacheManagers[index]);
   }

   private static class Partition {
      final AtomicInteger viewId;
      final List<Address> allMembers;
      final List<Cache<?, ?>> caches;
      final List<JChannel> channels = new ArrayList<>();

      public Partition(AtomicInteger viewId, List<Address> allMembers, List<Cache<?, ?>> caches) {
         this.viewId = viewId;
         this.allMembers = allMembers;
         this.caches = caches;
      }

      public void addNode(JChannel c) {
         channels.add(c);
      }

      public void partition() {
         log.trace("Partition forming");
         disableDiscovery();
         installNewView();
         assertPartitionFormed();
         log.trace("New views installed");
      }

      private void disableDiscovery() {
         channels.forEach(c ->
               ((Discovery) c.getProtocolStack().findProtocol(Discovery.class)).setClusterName(c.getAddressAsString())
         );
      }

      private void assertPartitionFormed() {
         final List<Address> viewMembers = new ArrayList<>();
         for (JChannel ac : channels) viewMembers.add(ac.getAddress());
         for (JChannel c : channels) {
            List<Address> members = c.getView().getMembers();
            if (!members.equals(viewMembers)) throw new AssertionError();
         }
      }

      private List<Address> installNewView() {
         final List<Address> viewMembers = new ArrayList<>();
         for (JChannel c : channels) viewMembers.add(c.getAddress());
         View view = View.create(channels.get(0).getAddress(), viewId.incrementAndGet(),
               viewMembers.toArray(new Address[0]));

         log.trace("Before installing new view...");
         for (JChannel c : channels) {
            getGms(c).installView(view);
         }
         return viewMembers;
      }

      private List<Address> installMergeView(ArrayList<JChannel> view1, ArrayList<JChannel> view2) {
         List<Address> allAddresses =
               Stream.concat(view1.stream(), view2.stream()).map(JChannel::getAddress).distinct()
                     .collect(Collectors.toList());

         View v1 = toView(view1);
         View v2 = toView(view2);
         List<View> allViews = new ArrayList<>();
         allViews.add(v1);
         allViews.add(v2);

         // Remove all sent NAKACK2 messages to reproduce ISPN-9291
         for (JChannel c : channels) {
            STABLE stable = c.getProtocolStack().findProtocol(STABLE.class);
            stable.gc();
         }
         try {
            Thread.sleep(10);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }

         MergeView mv = new MergeView(view1.get(0).getAddress(), viewId.incrementAndGet(), allAddresses, allViews);
         // Compute the merge digest, without it nodes would request the retransmission of all messages
         // Including those that were removed by STABLE earlier
         MutableDigest digest = new MutableDigest(allAddresses.toArray(new Address[0]));
         for (JChannel c : channels) {
            digest.merge(getGms(c).getDigest());
         }

         for (JChannel c : channels) {
            getGms(c).installView(mv, digest);
         }
         return allMembers;
      }

      private View toView(ArrayList<JChannel> channels) {
         final List<Address> viewMembers = new ArrayList<>();
         for (JChannel c : channels) viewMembers.add(c.getAddress());
         return View.create(channels.get(0).getAddress(), viewId.incrementAndGet(),
               viewMembers.toArray(new Address[0]));
      }

      private void discardOtherMembers() {
         List<Address> outsideMembers = new ArrayList<>();
         for (Address a : allMembers) {
            boolean inThisPartition = false;
            for (JChannel c : channels) {
               if (c.getAddress().equals(a)) inThisPartition = true;
            }
            if (!inThisPartition) outsideMembers.add(a);
         }
         for (JChannel c : channels) {
            DISCARD discard = new DISCARD();
            log.tracef("%s discarding messages from %s", c.getAddress(), outsideMembers);
            for (Address a : outsideMembers) discard.addIgnoreMember(a);
            try {
               c.getProtocolStack().insertProtocol(discard, ProtocolStack.Position.ABOVE, TP.class);
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }
      }

      private GMS getGms(JChannel c) {
         return c.getProtocolStack().findProtocol(GMS.class);
      }

      public void merge(Partition partition) {
         observeMembers(partition);
         partition.observeMembers(this);
         ArrayList<JChannel> view1 = new ArrayList<>(channels);
         ArrayList<JChannel> view2 = new ArrayList<>(partition.channels);
         partition.channels.stream().filter(c -> !channels.contains(c)).forEach(c -> channels.add(c));
         installMergeView(view1, view2);
         enableDiscovery();
         waitForPartitionToForm();
      }

      private void waitForPartitionToForm() {
         var caches = new ArrayList<>(this.caches);
         caches.removeIf(c -> !channels.contains(TestingUtil.extractJChannel(c.getCacheManager())));
         blockUntilViewsReceived(10000, caches);
         waitForNoRebalance(caches);
      }

      public void enableDiscovery() {
         channels.forEach(c -> {
            try {
               String defaultClusterName = TransportConfiguration.CLUSTER_NAME.getDefaultValue();
               ((Discovery) c.getProtocolStack().findProtocol(Discovery.class)).setClusterName(defaultClusterName);
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         });
         log.trace("Discovery started.");
      }

      private void observeMembers(Partition partition) {
         for (JChannel c : channels) {
            List<Protocol> protocols = c.getProtocolStack().getProtocols();
            for (Protocol p : protocols) {
               if (p instanceof DISCARD) {
                  for (JChannel oc : partition.channels) {
                     ((DISCARD) p).removeIgnoredMember(oc.getAddress());
                  }
               }
            }
         }
      }

      @Override
      public String toString() {
         StringBuilder addresses = new StringBuilder();
         for (JChannel c : channels) addresses.append(c.getAddress()).append(" ");
         return "Partition{" + addresses + '}';
      }
   }
}
