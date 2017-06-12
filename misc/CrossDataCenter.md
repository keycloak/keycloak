Test Cross-Data-Center scenario (test with external JDG server)
===============================================================

These are temporary notes. This docs should be removed once we have cross-DC support finished and properly documented. 

Note that these steps are already automated, see Cross-DC tests section in [HOW-TO-RUN.md](../testsuite/integration-arquillian/HOW-TO-RUN.md) document.
 
What is working right now is:
- Propagating of invalidation messages for "realms" and "users" caches
- All the other things provided by ClusterProvider, which is:
-- ClusterStartupTime (used for offlineSessions and revokeRefreshToken) is shared for all clusters in all datacenters
-- Periodic userStorage synchronization is always executed just on one node at a time. It won't be never executed concurrently on more nodes (Assuming "nodes" refer to all servers in all clusters in all datacenters)

What doesn't work right now:
- UserSessionProvider and offline sessions
  

Basic setup
===========

This is setup with 2 keycloak nodes, which are NOT in cluster. They just share the same database and they will be configured with "work" infinispan cache with remoteStore, which will point
to external JDG server.

JDG Server setup
----------------
- Download JDG 7.0 server and unzip to some folder

- Add this into JDG_HOME/standalone/configuration/standalone.xml under cache-container named "local" :

```
<local-cache name="work" start="EAGER" batching="false" />
```

- Start server:
```
cd JDG_HOME/bin
./standalone.sh -Djboss.socket.binding.port-offset=100
```

Keycloak servers setup
----------------------
You need to setup 2 Keycloak nodes in this way. 

For now, it's recommended to test Keycloak overlay on EAP7 because of infinispan bug, which is fixed in EAP 7.0 (infinispan 8.1.2), but not 
yet on Wildfly 10 (infinispan 8.1.0). See below for details.

1) Configure shared database in KEYCLOAK_HOME/standalone/configuration/standalone.xml . For example MySQL

2) Add `module` attribute to the infinispan keycloak container:
  
```  
<cache-container name="keycloak" jndi-name="infinispan/Keycloak" module="org.keycloak.keycloak-model-infinispan">
```
  
3) Configure `work` cache to use remoteStore. You should use this:  

```
<local-cache name="work">
    <remote-store passivation="false" fetch-state="false" purge="false" preload="false" shared="true" cache="work" remote-servers="remote-cache">    
        <property name="rawValues">true</property>
        <property name="marshaller">org.keycloak.cluster.infinispan.KeycloakHotRodMarshallerFactory</property>
    </remote-store>
</local-cache>  
```

4) Configure connection to the external JDG server. Because we used port offset 100 for JDG (see above), the HotRod endpoint is running on 11322 . 
So add the config like this to the bottom of standalone.xml under `socket-binding-group` element:

```
<outbound-socket-binding name="remote-cache">
    <remote-destination host="localhost" port="11322"/>
</outbound-socket-binding>
```

5) Optional: Configure logging in standalone.xml to see what invalidation events were send:
````
<logger category="org.keycloak.cluster.infinispan">
    <level name="TRACE"/>
</logger>
<logger category="org.keycloak.models.cache.infinispan">
    <level name="DEBUG"/>
</logger>
````
           
6)  Setup Keycloak node2 . Just copy Keycloak to another location on your laptop and repeat steps 1-5 above for second server too.
          
7) Run server 1 with parameters like (assuming you have virtual hosts "node1" and "node2" defined in your `/etc/hosts` ):
```           
./standalone.sh -Djboss.node.name=node1 -b node1 -bmanagement node1
```

and server2 with:
```
./standalone.sh -Djboss.node.name=node2 -b node2 -bmanagement node2
```

8) Note something like this in both `KEYCLOAK_HOME/standalone/log/server.log` on both nodes. Note that cluster Startup Time will be same time on both nodes:
```
2016-11-16 22:12:52,080 DEBUG [org.keycloak.cluster.infinispan.InfinispanClusterProviderFactory] (ServerService Thread Pool -- 62) My address: node1-1953169551
2016-11-16 22:12:52,081 DEBUG [org.keycloak.cluster.infinispan.CrossDCAwareCacheFactory] (ServerService Thread Pool -- 62) RemoteStore is available. Cross-DC scenario will be used
2016-11-16 22:12:52,119 DEBUG [org.keycloak.cluster.infinispan.InfinispanClusterProviderFactory] (ServerService Thread Pool -- 62) Loaded cluster startup time: Wed Nov 16 22:09:48 CET 2016
2016-11-16 22:12:52,128 DEBUG [org.keycloak.cluster.infinispan.InfinispanNotificationsManager] (ServerService Thread Pool -- 62) Added listener for HotRod remoteStore cache: work
```

9) Login to node1. Then change any realm on node2. You will see in the node2 server.log that RealmUpdatedEvent was sent and on node1 that this event was received. 

This is done even if node1 and node2 are NOT in cluster as it's the external JDG used for communication between 2 keycloak servers and sending/receiving cache invalidation events. But note that userSession
doesn't yet work (eg. if you login to node1, you won't see the userSession on node2).


WARNING: Previous steps works on Keycloak server overlay deployed on EAP 7.0 . With deploy on Wildfly 10.0.0.Final, you will see exception 
at startup caused by the bug https://issues.jboss.org/browse/ISPN-6203 .

There is a workaround to add this line into KEYCLOAK_HOME/modules/system/layers/base/org/wildfly/clustering/service/main/module.xml :

```
<module name="org.infinispan.client.hotrod"/>
```
