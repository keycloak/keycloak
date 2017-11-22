Test Cross-Data-Center scenario (test with external JDG server)
===============================================================

These are temporary notes. This docs should be removed once we have cross-DC support finished and properly documented. 

These steps are already automated for embedded Undertow, see Cross-DC tests section in [HOW-TO-RUN.md](../testsuite/integration-arquillian/HOW-TO-RUN.md) document. For Wildfly they are not yet automated.
Following instructions are related to Wildfly server.
 
Right now, everything should work correctly in cross-dc environment. Especially:
- Propagating of invalidation messages for `realms`, `users` and `authorization` caches
- sessions, offline sessions and login failures are propagated between datacenters 
  
  
Documentation intro
===================

Keycloak has support for cross-datacenter (cross-site) replication. Typical usecase is, that you have multiple 
datacenters (sites) in different geographical locations. Every datacenter has it's own cluster of Keycloak servers.


Here is the picture with some example architecture:

https://4.bp.blogspot.com/-TuP-tUCytyY/Wa-1b33MTxI/AAAAAAAAIjA/FSSSzfDP1uMqlhkyUqayb4NJwH-O7EFZQCLcBGAs/s1600/Cross-site%2Bdiagram.jpg
  
QUESTION: Is this picture appropriate for the Keycloak/RHSSO docs or do we need some better? 
TODO: ATM there are databases missing in the picture. Also there is "keycloak" referenced everywhere, should be rather replaced with "RHSSO" 
or something universal for both Keycloak and RHSSO? 

QUESTION: Is it better to use term "site" or term "datacenter" . In the whole docs, I am using both (but probably term "datacenter" a bit more).
Should it be consolidated?
 
 
Prerequisities
==============

Cross-datacenter replication is quite an advanced topic. It's recommended that you have some background knowledge and go through 
those documents first:

* Keycloak/RHSSO clustering - http://www.keycloak.org/docs/latest/server_installation/index.html#_clustering 
With cross-datacenter, you will usually setup more independent Keycloak clusters. 
So it's good to understand how cluster works and basic concepts and requirements around it (Multicast, Loadbalancing, Shared database etc).  

* JDG Cross-Datacenter docs - https://access.redhat.com/documentation/en-us/red_hat_jboss_data_grid/7.1/html/administration_and_configuration_guide/set_up_cross_datacenter_replication
Keycloak Cross-Datacenter uses JDG for the actual replication of infinispan data between the datacenters. So it's good to read and understand
JDG first. We use the `Remote Client-Server Mode` described in here - https://access.redhat.com/documentation/en-us/red_hat_jboss_data_grid/7.1/html/administration_and_configuration_guide/set_up_cross_datacenter_replication#configure_cross_datacenter_replication_remote_client_server_mode 
 
   

Technical details
================= 

Data
----
Keycloak is stateful application, which uses 2 main sources of data.
* Database - is used to persist permanent data (EG. informations about the users). 
* Infinispan cache - is used to cache persistent data from DB and also to save some short-lived and often-changing metadata like user sessions. 
Infinispan is usually much faster then database, however the data saved here are not permanent and usually they don't survive cluster restarts.  

Assume you have 2 datacenters called `site1` and `site2` .
For the cross-datacenter setup, we need to make sure that both sources of data work reliably and Keycloak 
servers from `site1` are eventually able to read the data saved by Keycloak servers on `site2` . 
Based on the environment, you have some flexibility to decide if you prefer:
* Reliability - typically needed in Active/Active mode. Data written on `site1` need to be visible immediately on `site2`.
* Performance - typically in Active/Passive mode. Data written on `site1` doesn't need to be visible immediately on `site2`. 
In some cases, they may not be visible on `site2` at all. 

More details about this is in [Modes section](#modes).
 


Request processing
------------------

In typical scenario, end user's browser sends HTTP request to the [frontend loadbalancer server](http://www.keycloak.org/docs/latest/server_installation/index.html#_setting-up-a-load-balancer-or-proxy). 
Loadbalancer is usually HTTPD or Wildfly with mod_cluster, NGinx, HA Proxy or other kind of software or hardware loadbalancer. 
Loadbalancer then forwards HTTP requests to the underlying Keycloak instances, which can be spread among 
multiple datacenters (sites). Loadbalancers typically offer support for [sticky sessions](http://www.keycloak.org/docs/latest/server_installation/index.html#sticky-sessions),
which means that loadbalancer is able to forward HTTP requests from one user always to the same Keycloak instance in same datacenter.

There are also HTTP requests, which are sent from client applications to the loadbalancer. Those HTTP requests are `backchannel requests`. 
They are not seen by end user's browser and can't be part of sticky session between user and loadbalancer. Hence loadbalancer can forward 
the particular HTTP request to any Keycloak instance in any datacenter. This is challenging as some OpenID Connect or SAML flows require 
multiple HTTP requests from both user and application. Because we can't reliably rely on sticky sessions, it means that some data need to be 
replicated between datacenters, so they are seen by subsequent HTTP requests during particular flow.  


Modes
-----
   
According your requirements, there are 2 basic operating modes for the cross-dc setup:
* Active/Passive - Here the users and client applications send the requests just to the Keycloak nodes in single datacenter.
The second datacenter is used just as a `backup` for saving the data. In case of the failure in the main datacenter,
the data can be usually restored from the second datacenter.

* Active/Active - Here the users and client applications send the requests to the Keycloak nodes in both datacenters.
It means that data need to be visible immediately on both sites and available to be consumed immediately from Keycloak servers on both sites.
Especially if Keycloak server writes some data on `site1`, it is required that the data are available immediately for reading 
for Keycloak servers on `site2` at the time when the write on `site1` is finished.

The active/passive mode is better for performance. More info about how to configure caches for both modes will follow 
in the [sync or async backups section](#sync-or-async-backups).    

 
Database
--------
Keycloak uses RDBMS to persist some metadata about realms, clients, users etc. See [this chapter](http://www.keycloak.org/docs/latest/server_installation/index.html#_database) 
for more details. In cross-datacenter setup, we assume that either both datacenters talk to same database or every datacenter 
has it's own database node and both database nodes are synchronously replicated. In both cases, it's required that when Keycloak server
on `site1` persists some data and commits the transaction, those data are immediately visible by subsequent DB transactions on `site2`.

Details of DB setup are out-of-scope of Keycloak, however note that many RDBMS vendors like PostgreSQL, MariaDB or Oracle offers 
replicated databases and synchronous replication. We tested Keycloak with those vendors:
TODO: Details about MariaDB and Oracle RAC versions etc.


Infinispan caches
-----------------
Here are some overview about the infinispan caches. More details about the details of the cache setup will follow later.

**Authentication sessions**

In Keycloak we have concept of authentication sessions. There is separate infinispan cache `authenticationSessions` used to save data during 
authentication of particular user. Requests from this cache usually involves just browser and Keycloak server, not the application. Hence we can 
rely on sticky sessions and `authenticationSessions` cache content doesn't need to be replicated among datacenters even if you are in Active/Active mode. 


**Action tokens**

We have concept of [action tokens](http://www.keycloak.org/docs/latest/server_development/index.html#_action_token_spi), which 
are used typically for scenarios when user needs to confirm some actions asynchronously by email. 
For example during `forget password` flow. The `actionTokens` infinispan cache is used to track metadata about action tokens 
(eg. which action token was already used, so it can't be reused second time) and it usually needs to be replicated between datacenters. 


**Caching and invalidation of persistent data**

Keycloak uses infinispan for cache persistent data to avoid many unecessary requests to the database. 
Caching is great for save performance, however there is one additional challenge, that when some Keycloak 
server updates any data, all other Keycloak servers in all datacenters need to be aware of it, so they 
invalidate particular data from their caches. Keycloak uses local infinispan caches called `realms`, `users` 
and `authorization` to cache persistent data.

We use separate cache `work`, which is replicated among all datacenters. The work cache itself doesn't cache 
any real data. It is defacto used just for sending invalidation messages between cluster nodes and datacenters.
In other words, when some data is updated (eg. user `john` is updated), the particular Keycloak node sends 
the invalidation message to all other cluster nodes in same datacenter and also to all other datacenters. 
Every node then invalidates particular data from their local cache once it receives the invalidation message. 


**User sessions**

There are infinispan caches `sessions`, `clientSessions`, `offlineSessions` and `offlineClientSessions`, 
which usually need to be replicated between datacenters. Those caches are used to save data about user 
sessions, which are valid for the whole life of one user's browser session. The caches need to deal with 
the HTTP requests from the end user and from the application. As described above, sticky session can't be 
always reliably used, but we still want to ensure that subsequent HTTP requests can see the latest data. 
Hence the data are usually replicated between datacenters. 


**Brute force protection**

Finally `loginFailures` cache is used to track data about failed logins (eg. how many times user `john` 
filled the bad password on username/password screen etc). The details are described [here](http://www.keycloak.org/docs/latest/server_admin/index.html#password-guess-brute-force-attacks) .
It is up to the admin if he wants this cache to be replicated between datacenters. To have accurate count of login failures, 
the replication is needed. On the other hand, avoid replicating this data can save some performance. So if performance is 
more important then accurate counts of login failures, the replication can be avoided. 

More details about how can be caches configured is [in this section](#tuning-jdg-cache-configuration) .

Communication details
---------------------

Under the covers, there are multiple separate infinispan clusters here. Every Keycloak node is in the cluster 
with the other Keycloak nodes in same datacenter, but not with the Keycloak nodes in different datacenters. 
Keycloak node doesn't communicate directly with the Keycloak nodes from different datacenters. Keycloak nodes use external JDG 
(or infinispan server) for communication between datacenters. This is done 
through the [Infinispan HotRod protocol](http://infinispan.org/docs/8.2.x/user_guide/user_guide.html#using_hot_rod_server) .

QUESTION: Should we just remove "(or infinispan server)" from the docs? Background: Integration is tested with the JDG server 7.1.0 and 
Infinispan server 8.2.8. I think that for our customers (product documentation), we even don't want to mention to use community infinispan
server in the product documentation and we always want to use JDG. For the community, I am not sure we can also always stick with the JDG 7.1.0
or mention Infinispan 8.2.8 as an option as well?

The infinispan caches on Keycloak side needs to be configured with the [remoteStore](http://infinispan.org/docs/8.2.x/user_guide/user_guide.html#remote_store), 
to ensure that data are saved to the remote cache, which uses HotRod protocol under the covers. There is separate infinispan cluster 
between JDG servers, so the data saved on JDG1 on `site1` are replicated to JDG2 on `site2` . 

Finally the receiver JDG server then notifies Keycloak servers in it's cluster through the Client Listeners, which is a feature of 
HotRod protocol. Keycloak nodes on `site2` then update their infinispan caches and particular user session is visible on Keycloak nodes on 
site 2 too. 

See the picture in [intro section](#documentation-intro) for more details.  

QUESTION: Do we want to add another picture with the details for communication? Or is the picture in the intro section sufficient?


Basic setup
===========

This is the example setup simulating 2 datacenters `site1 ` and `site 2` . Each datacenter consists of 1 infinispan server and 2 Keycloak servers. 
So 2 infinispan servers and 4 Keycloak servers are totally in the testing setup. 

* Site1 consists of infinispan server `jdg1` and 2 Keycloak servers `node11` and `node12` .

* Site2 consists of infinispan server `jdg2` and 2 Keycloak servers `node21` and `node22` .
 
* Infinispan servers `jdg1` and `jdg2` are connected with each other through the RELAY2 protocol and `backup` based infinispan caches in
similar way as described in the [JDG documentation](https://access.redhat.com/documentation/en-us/red_hat_jboss_data_grid/7.1/html-single/administration_and_configuration_guide/#configure_cross_datacenter_replication_remote_client_server_mode) . 
* Keycloak servers `node11` and `node12` forms cluster with each other, but they don't communicate with any server in `site2` .
They communicate with infinispan server `jdg1` through the HotRod protocol (Remote cache). See [previous section](#communication-details) for the details.
  
* Same applies for `node21` and `node22` . They have cluster with each other and communicate just with `jdg2` server through the HotRod protocol.
  
Example setup assumes all 6 servers are bootstrapped on localhost, but each on different ports. It also assumes
that all 4 Keycloak servers talk to same database, which can be either locally set MySQL, PostgreSQL, MariaDB 
or any other. In production, there will be rather separate synchronously replicated databases between datacenters as described in 
[this section](#database) . 




JDG Server setup
----------------

1) Download JDG 7.1.0 server (or Infinispan 8.2.8 server for the community - TODO: Same question as in previous section applies here) 
and unzip to some folder. It will be referred in later steps as `JDG1_HOME` .

2) Change those things in the `JDG1_HOME/standalone/configuration/clustered.xml` in the configuration of JGroups subsystem:

2.a) Add the `xsite` channel, which will use `tcp` stack, under `channels` element:

```xml
<channels default="cluster">
    <channel name="cluster"/>
    <channel name="xsite" stack="tcp"/>
</channels>
```

2.b) Add `relay` element to the end of the `udp` stack. We will configure it in a way that our site is `site1` and the 
other site, where we will backup, is `site2`:

```xml
<stack name="udp">
    ...
    <relay site="site1">
        <remote-site name="site2" channel="xsite"/>
    </relay>
</stack>
```

2.c) Configure `tcp` stack to use `TCPPING` protocol instead of `MPING` . Just remove `MPING` element and replace with the `TCPPING` like this. 
The `initial_hosts` element points to the hosts `jdg1` and `jdg2`, which in our example setup are both on `localhost`, 
but differs just in the port offset:

```xml
<stack name="tcp">
    <transport type="TCP" socket-binding="jgroups-tcp"/>
    <protocol type="TCPPING">
        <property name="initial_hosts">localhost[8610],localhost[9610]"</property>
        <property name="ergonomics">false</property>
    </protocol>
    <protocol type="MERGE3"/>
    ...
</stack>
```

NOTE:
This is just an example setup to have things quickly running. In production, you are not required to use `tcp` stack for the
JGroups `RELAY2`, but you can configure any other stack. For example the other instance of UDP protocol, if the network between your 
datacenters is able to support multicast. Similarly you are not required to use `TCPPING` as discovery protocol. And in production, 
you probably won't use `TCPPING` due it's static nature. Site names are 
also configurable. 

Details of this setup are out-of-scope of the Keycloak documentation. You can take a look at JDG documentation and JGroups documentation
for more details.
ENDNOTE

TODO, QUESTION: I guess we want to document Amazon setup? Should we add the link to the Amazon setup documentation here? 


3) Add this into `JDG1_HOME/standalone/configuration/clustered.xml` under cache-container named `clustered` :

```xml   
<cache-container name="clustered" default-cache="default" statistics="true">
        ...
        <replicated-cache-configuration name="sessions-cfg" mode="SYNC" start="EAGER" batching="false">
            <transaction mode="NON_DURABLE_XA" locking="PESSIMISTIC"/>
            <locking acquire-timeout="0" />
            <backups>
                <backup site="site2" failure-policy="FAIL" strategy="SYNC" enabled="true">
                    <take-offline min-wait="60000" after-failures="3" />
                </backup>
            </backups>
        </replicated-cache-configuration>

        <replicated-cache name="work" configuration="sessions-cfg"/>
        <replicated-cache name="sessions" configuration="sessions-cfg"/>
        <replicated-cache name="clientSessions" configuration="sessions-cfg"/>
        <replicated-cache name="offlineSessions" configuration="sessions-cfg"/>
        <replicated-cache name="offlineClientSessions" configuration="sessions-cfg"/>
        <replicated-cache name="actionTokens" configuration="sessions-cfg"/>
        <replicated-cache name="loginFailures" configuration="sessions-cfg"/>
                
</cache-container>
```

NOTE: Details about the configuration options inside `replicated-cache-configuration` are explained in [later section](#tuning-jdg-cache-configuration). Also
with possibilities to tweak some of those options.
ENDNOTE 
        
4) Copy the server into the second location referred later as `JDG2_HOME`

5) In the `JDG2_HOME/standalone/configuration/clustered.xml` exchange `site1` with `site2` and viceversa in the configuration of `relay` in the
JGroups subsystem and in configuration of `backups` in the cache-subsystem.

In other words, the `relay` element should look like this:

```
<relay site="site2">
    <remote-site name="site1" channel="xsite"/>
</relay>
``` 

And the backups like this:

```
            <backups>
                <backup site="site1" ....
                ...
```  

NOTE: It's currently needed to have different configuration files for the JDG servers on both sites as Infinispan subsystem doesn't support
replacing site name with expressions. See [this issue](https://issues.jboss.org/browse/WFLY-9458) for more details.
ENDNOTE

6) Start server `jdg1`:

```
cd JDG1_HOME/bin
./standalone.sh -c clustered.xml -Djava.net.preferIPv4Stack=true \
-Djboss.socket.binding.port-offset=1010 -Djboss.default.multicast.address=234.56.78.99 \
-Djboss.node.name=jdg1
```

7) Start server `jdg2` . There is different multicast address, so the `jdg1` and `jdg2` servers are not in "direct" cluster with each other,
but they are just connected through the RELAY2 protocol and TCP JGroups stack is used for communication between them. So the startup command is like this:

```
cd JDG2_HOME/bin
./standalone.sh -c clustered.xml -Djava.net.preferIPv4Stack=true \
-Djboss.socket.binding.port-offset=2010 -Djboss.default.multicast.address=234.56.78.100 \
-Djboss.node.name=jdg2
```

8) To verify that channel works at this point, you may need to use JConsole and connect either to JDG1 or JDG2 running server. When
use the MBean `jgroups:type=protocol,cluster="cluster",protocol=RELAY2` and operation `printRoutes`, you should see the output like this:

```
site1 --> _jdg1:site1
site2 --> _jdg2:site2
```

When use the MBean `jgroups:type=protocol,cluster="cluster",protocol=GMS`, you should see that attribute member contains just single member. On JDG1
it should be like this:

```
(1) jdg1
```

And on JDG2 like this:
```
(1) jdg2
```

In production, you can have more JDG servers in every datacenter. You just need to ensure that JDG servers in same datacenter are using 
same multicast address (In other words, same `jboss.default.multicast.address` during startup). Then in jconsole in `GMS` protocol 
view, you will see all the members of current cluster.


Keycloak servers setup
----------------------
1) Unzip keycloak server distribution to some location referred later as `NODE11`

2) Configure shared database for KeycloakDS datasource. Recommended to use MySQL, MariaDB or PostgreSQL for testing purposes. 
See [this section](#database)] for more details.

Note again, that in production you will likely need to have separate database server in every datacenter and both database servers 
should be synchronously replicated to each other. In the example setup, we just use single database and connect all 4 Keycloak servers to it.   
 
3) Edit `NODE11/standalone/configuration/standalone-ha.xml` :

3.1) Add attribute `site` to the JGroups UDP protocol:
  
```xml
                  <stack name="udp">
                      <transport type="UDP" socket-binding="jgroups-udp" site="${jboss.site.name}"/>
```  

3.2) Add this `module` attribute under `cache-container` element of name `keycloak` :
 
```xml
 <cache-container name="keycloak" jndi-name="infinispan/Keycloak" module="org.keycloak.keycloak-model-infinispan">
``` 

3.3) Add the `remote-store` under `work` cache:

```xml
<replicated-cache name="work" mode="SYNC">
    <remote-store cache="work" remote-servers="remote-cache" passivation="false" fetch-state="false" purge="false" preload="false" shared="true">
        <property name="rawValues">true</property>
        <property name="marshaller">org.keycloak.cluster.infinispan.KeycloakHotRodMarshallerFactory</property>
    </remote-store>
</replicated-cache>
```

3.5) Add the `remote-store` like this under `sessions` cache:

```xml
<distributed-cache name="sessions" mode="SYNC" owners="1">
    <remote-store cache="sessions" remote-servers="remote-cache" passivation="false" fetch-state="false" purge="false" preload="false" shared="true">   
        <property name="rawValues">true</property>
        <property name="marshaller">org.keycloak.cluster.infinispan.KeycloakHotRodMarshallerFactory</property>
    </remote-store>
</distributed-cache>
```

3.6) Same for `offlineSessions`, `clientSessions`, `offlineClientSessions`, `loginFailures`, and `actionTokens` caches (the only difference
from `sessions` cache is that `cache` property value are different):

```xml
<distributed-cache name="offlineSessions" mode="SYNC" owners="1">
    <remote-store cache="offlineSessions" remote-servers="remote-cache" passivation="false" fetch-state="false" purge="false" preload="false" shared="true">
        <property name="rawValues">true</property>
        <property name="marshaller">org.keycloak.cluster.infinispan.KeycloakHotRodMarshallerFactory</property>
    </remote-store>
</distributed-cache>

<distributed-cache name="clientSessions" mode="SYNC" owners="1">
    <remote-store cache="clientSessions" remote-servers="remote-cache" passivation="false" fetch-state="false" purge="false" preload="false" shared="true">
        <property name="rawValues">true</property>
        <property name="marshaller">org.keycloak.cluster.infinispan.KeycloakHotRodMarshallerFactory</property>
    </remote-store>
</distributed-cache>

<distributed-cache name="offlineClientSessions" mode="SYNC" owners="1">
    <remote-store cache="offlineClientSessions" remote-servers="remote-cache" passivation="false" fetch-state="false" purge="false" preload="false" shared="true">
        <property name="rawValues">true</property>
        <property name="marshaller">org.keycloak.cluster.infinispan.KeycloakHotRodMarshallerFactory</property>
    </remote-store>
</distributed-cache>

<distributed-cache name="loginFailures" mode="SYNC" owners="1">
    <remote-store cache="loginFailures" remote-servers="remote-cache" passivation="false" fetch-state="false" purge="false" preload="false" shared="true">
        <property name="rawValues">true</property>
        <property name="marshaller">org.keycloak.cluster.infinispan.KeycloakHotRodMarshallerFactory</property>
    </remote-store>
</distributed-cache>

<distributed-cache name="actionTokens" mode="SYNC" owners="2">
    <eviction max-entries="-1" strategy="NONE"/>
    <expiration max-idle="-1" interval="300000"/>
    <remote-store cache="actionTokens" remote-servers="remote-cache" passivation="false" fetch-state="false" purge="false" preload="true" shared="true">
        <property name="rawValues">true</property>
        <property name="marshaller">org.keycloak.cluster.infinispan.KeycloakHotRodMarshallerFactory</property>
    </remote-store>
</distributed-cache>
```

3.7) Add outbound socket binding for the remote store into `socket-binding-group` element configuration:

```xml
<outbound-socket-binding name="remote-cache">
    <remote-destination host="${remote.cache.host:localhost}" port="${remote.cache.port:11222}"/>
</outbound-socket-binding>
```

3.8) The configuration of distributed cache `authenticationSessions` and other caches is left unchanged.

3.9) Optionally enable DEBUG logging under `logging` subsystem:

```xml
<logger category="org.keycloak.cluster.infinispan">
    <level name="DEBUG"/>
</logger>
<logger category="org.keycloak.connections.infinispan">
    <level name="DEBUG"/>
</logger>
<logger category="org.keycloak.models.cache.infinispan">
    <level name="DEBUG"/>
</logger>
<logger category="org.keycloak.models.sessions.infinispan">
    <level name="DEBUG"/>
</logger>
```

4) Copy the `NODE11` to 3 other directories referred later as `NODE12`, `NODE21` and `NODE22`.
 
5) Start `NODE11` :
 
```
cd NODE11/bin
./standalone.sh -c standalone-ha.xml -Djboss.node.name=node11 -Djboss.site.name=site1 \
-Djboss.default.multicast.address=234.56.78.1 -Dremote.cache.port=12232 -Djava.net.preferIPv4Stack=true \
-Djboss.socket.binding.port-offset=3000

```

6) Start `NODE12` :

````
cd NODE12/bin
./standalone.sh -c standalone-ha.xml -Djboss.node.name=node12 -Djboss.site.name=site1 \
-Djboss.default.multicast.address=234.56.78.1 -Dremote.cache.port=12232 -Djava.net.preferIPv4Stack=true \
-Djboss.socket.binding.port-offset=4000
````

The cluster nodes should be connected. Something like this should be in the log of both NODE11 and NODE12:

```
Received new cluster view for channel keycloak: [node11|1] (2) [node11, node12]
```

NOTE: The channel name in the log might be different.
ENDNOTE


7) Start `NODE21` :

```
cd NODE21/bin
./standalone.sh -c standalone-ha.xml -Djboss.node.name=node21 -Djboss.site.name=site2 \
-Djboss.default.multicast.address=234.56.78.2 -Dremote.cache.port=13232 -Djava.net.preferIPv4Stack=true \
-Djboss.socket.binding.port-offset=5000
```

It shouldn't be connected to the cluster with `NODE11` and `NODE12`, but to separate one:

```
Received new cluster view for channel keycloak: [node21|0] (1) [node21]
```

8) Start `NODE22` :

```
cd NODE22/bin
./standalone.sh -c standalone-ha.xml -Djboss.node.name=node22 -Djboss.site.name=site2 \
-Djboss.default.multicast.address=234.56.78.2 -Dremote.cache.port=13232 -Djava.net.preferIPv4Stack=true \
-Djboss.socket.binding.port-offset=6000
```

It should be in cluster with `NODE21` :

```
Received new cluster view for channel keycloak: [node21|1] (2) [node21, node22]
```

NOTE: The channel name in the log might be different.
ENDNOTE


9) Test:

9.1) Go to `http://localhost:11080/auth/` and create initial admin user

9.2) Go to `http://localhost:11080/auth/admin` and login as admin to admin console

9.3) Open 2nd browser and go to any of nodes `http://localhost:12080/auth/admin` or `http://localhost:13080/auth/admin` or `http://localhost:14080/auth/admin` . After login, you should be able to see 
the same sessions in tab `Sessions` of particular user, client or realm on all 4 servers

9.4) After doing any change in Keycloak admin console (eg. update some user or some realm), the update 
should be immediately visible on any of 4 nodes as caches should be properly invalidated everywhere.


9.5) Check server.logs if needed. After login or logout, the message like this should be on all the nodes `NODEXY/standalone/log/server.log` :

```
2017-08-25 17:35:17,737 DEBUG [org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionListener] (Client-Listener-sessions-30012a77422542f5) Received event from remote store. 
Event 'CLIENT_CACHE_ENTRY_REMOVED', key '193489e7-e2bc-4069-afe8-f1dfa73084ea', skip 'false'
```


Administration of Cross-DC deployment
=====================================

Few tips and possibilities related to the Cross-DC deployment.

* When you run the Keycloak server inside datacenter, it is required that database referenced in `KeycloakDS` datasource 
is already running and available in that datacenter. It is also necessary that JDG server referenced by the `outbound-socket-binding`, which
is referenced from the infinispan cache `remote-store` elements, is already running. Otherwise Keycloak server will fail to start.


* Every datacenter can have more database nodes if you want to support database failover and better reliability.
In that case, the JDBC URL used in the configuration of the `KeycloakDS` datasource in `standalone-ha.xml` 
needs to contain URLs of all the actual database nodes of the particular DC.

QUESTION: Is more info needed? Example of the JDBC URLs with more DB nodes? Or is it out of our scope?


* As mentioned before in [the JDG Server setup section](#jdg-server-setup), every datacenter can have more JDG servers 
running in the cluster. This is useful if you want some failover and better fault tolerance. 
The hotrod protocol used for communication between JDG servers and Keycloak servers has a feature that JDG servers will 
automatically send new topology to the Keycloak servers about the change in
the JDG cluster, so the remote store on Keycloak side will know to which JDG servers it can connect. 
Read the JDG/Infinispan and Wildfly documentation for more details around this.

QUESTION: Should we provide more info? Or rather wait for the feedback from customers and community?
  
 
* It is highly recommended that master JDG server is running in every site before the Keycloak servers in **any** site 
are executed. Like in our example, we executed both `jdg1` and `jdg2` first and all Keycloak servers afterwards. The details are described in
[next section](#bringing-sites-offline-and-online).


Bringing sites offline and online
=================================

For example, assume this scenario.

1) Site `site2` is entirely offline from the `site1` perspective. It means that all JDG servers on `site2` are off *or* the network between `site1` and `site2` is broken. 
2) You run Keycloak servers and JDG server `jdg1` in site `site1` 
3) Someone login on some Keycloak server on `site1`. 
4) The Keycloak server from `site1` will try to write the session to the remote
cache on `jdg1` server, which is supposed to backup data to the `jdg2` server in the `site2`. See [this section](#communication-details) for 
the details. 
5) Server `jdg2` is offline or unreachable from `jdg1`. So the backup from `jdg1` to `jdg2` will fail.
6) The exception is thrown in `jdg1` log and the failure will be propagated from `jdg1` server to Keycloak servers as well because 
the default `FAIL` backup failure policy is configured. See [this section](#backup-failure-policy) for details around the backup policies.
7) The error will happen on Keycloak side too and user may not be able to finish his login.

According to your environment, it may be more or less probable that the network between sites is unavailable or temporarily broken (split-brain).
In case that this will happen, it's good that JDG servers on `site1` are aware of the fact that JDG servers on `site2` are 
unavailable (In other words, that `site2` is offline), so they will stop
trying to reach servers in `jdg2` site and the backup failures won't happen. This is called `Take site offline` .

Take site offline
-----------------

There are 2 ways to take the site offline.

1) **Manually by admin** - Admin can use the `jconsole` or other tool and run some JMX operations to manually take the particular site offline.
This is useful especially if the outage is planned. With `jconsole`, you can connect to the `jdg1` server and use the MBean `jboss.datagrid-infinispan:type=Cache,name="sessions(repl_sync)",manager="clustered",component=XSiteAdmin`
and then operation `takeSiteOffline` with the argument `site2` as shown in the picture. You can then check the operation `status` to check
if site is really offline. See the picture for details:

PICTURE: https://drive.google.com/file/d/1g6tJ979lSmlcR7g3AWDj4cUc0xWKruiH/view?usp=sharing
 
WARNING: This turned off the backup to `site2` for the cache `sessions`. The same steps usually needs to be done for all the 
other Keycloak caches mentioned [here](#sync-or-async-backups) .
STOPWARNING

There are also ways to take site offline manually with usage of CLI. More details about this is 
in the [JDG documentation](https://access.redhat.com/documentation/en-us/red_hat_jboss_data_grid/7.1/html/administration_and_configuration_guide/set_up_cross_datacenter_replication#taking_a_site_offline) 

QUESTION: Should we provide some CLI script to help taking site offline for all our caches? And similarly for putting sites back online
and do the state transfer?



2) **Automatically** - After some amount of failed backups, the `site2` will be usually automatically taken offline. This is done due the 
configuration of `take-offline` element inside the cache configuration as configured [here](#jdg-server-setup) .

```
<take-offline min-wait="60000" after-failures="3" />
```

It means that site will be automatically taken offline for the particular single cache if there are at least 3 subsequent failed backups
and there is no any success backup within 60 seconds.

Automatically taking site offline is useful especially if the broken network between sites is unplanned. It's disadvantage is, that
there will be some failed backups until the network outage is detected, which could also mean the failures on the application side. 
For example, there will be failed logins for some users or big login timeouts. Especially if `failure-policy` with value `FAIL` is used.

WARNING: The tracking if site is offline or not is again tracked separately for every cache.
ENDWARNING
 

Take site online
----------------


Once your network is back and `site1` and `site2` can talk to each other, you may need to put the site online. This needs to be done 
manually through JMX or CLI in similar way as described in the [previous section](#take-site-offline). 
The JMX operation is `bringSiteOnline` . Again, you may need to check all the caches and bring them online.

Once the sites are put online, it's usually good to:
* Do the [state transfer](#state-transfer)
* Manually [clear the Keycloak caches](#clear-caches) . 

State transfer
-------------

State transfer is manually required step. JDG doesn't do this automatically as 
for example during split-brain, it's just the admin who may need to decide which site has preference and hence if state-transfer 
needs to be done bi-directionaly between both sites or just unidirectionally (EG. just from `site1` to `site2`, but not from `site2` to `site1`). 

During bi-directional state transfer, it will ensure that entities, which were created *after* split-brain on `site1` will be transferred
to `site2` . This is no issue as they don't exist yet on `site2` . Similarly entities created *after* split-brain on `site2` will be transferred 
to `site1` . Possible problematic parts are the entities, which exists *before* split brain on both sites and which were updated during split-brain
on both sites. In that case one of the site will *win* and will overwrite the updates done during split-brain by the second part.

Unfortunately there is no any universal solution to this. Split-brains and network outages are just state, which is usually impossible to be handled 100% 
correctly with 100% consistent data between sites. For the case of Keycloak, it typically is not critical issue. In worst case, users
will need to re-login again to their clients. Or have the improper count of loginFailures tracked for brute force protection. See JDG/JGroups/Infinispan
docs for more tips how to deal with split brain.

The state transfer can be done through JMX. Operation name is `pushState` . There are few other operations to monitor status, cancel push state etc.
More info about state transfer is in JDG docs - https://access.redhat.com/documentation/en-us/red_hat_jboss_data_grid/7.1/html/administration_and_configuration_guide/set_up_cross_datacenter_replication#state_transfer_between_sites

Clear caches
------------

After split-brain it's also safe to manually clear caches in Keycloak admin console. Reason is, that there might be some data changed in DB
on `site1` and the event, that cache should be invalidated wasn't transferred during split-brain to `site2` . 
Hence Keycloak nodes on `site2` may still have some stale data in their caches. 

To clear the caches, take a look at http://www.keycloak.org/docs/latest/server_admin/index.html#_clear-cache .

When network is back, it's sufficient to clear the cache just on one Keycloak node on any random site.
The event about cache invalidation will be sent to all the other Keycloak nodes in all sites. However it needs 
to be done for all the caches (realms, users, keys).    


Tuning JDG cache configuration
==============================

Backup failure policy
---------------------

By default, the configuration of backup `failure-policy` in the infinispan cache configuration in JDG `clustered.xml` 
file is configured as `FAIL` . According your preferences, you may change it to `WARN` or `IGNORE` . 

The difference between `FAIL` and `WARN` is, that when JDG server tries to backup data to the other site and the backup fails (EG. second site
is temporarily unreachable or there is concurrent transaction, which is trying to update same entity), 
then the failure will be propagated back to the caller (Keycloak server) if the `FAIL` policy is used. 
The Keycloak server will then try to retry the particular operation few times. However if the second site is really unavailable, 
the retry will fail too and the user might see the error after some longer timeout (EG. 1 minute). 

With `WARN` policy, the failed backups are not propagated from JDG server to the Keycloak server. User won't see the error and the 
failed backup will be just ignored. There will just be some shorter timeout, 
typically 10 seconds as that's the default timeout for backup. It can be changed by the attribute `timeout` of `backup` element. 
There won't be retries. There will just be the WARNING message in the JDG server log. 

The potential issue is, that in some cases, there may be just some very short network outage between sites, where the retry
(usage of the `FAIL` policy) may help, so with `WARN` (without retry), there will be some data inconsistencies between sites. 
This can also happen if there is an attempt to update same entity concurrently on both sites.

The question is, how bad inconsistencies are. Usually it means that user just need to re-authenticate.

With `WARN` policy, it may happen that single-use cache, which is provided by the `actionTokens` cache and which handles that 
particular key is really single
use, may "successfully" write the same key twice. But for example the OAuth2 specification mentions that code must be single-use. 
See [here](https://tools.ietf.org/html/rfc6749#section-10.5) .
With the `WARN` policy, this may not be strictly guaranteed and the same code could be written twice if there is an attempt to write
it concurrently in both sites.

If there is real longer network outage or split-brain, then with both `FAIL` and `WARN`, the other site will be taken offline after some
time and failures as described [here](#take-site-offline) . With the default 1 minute timeout, it is usually after 1-3 minutes 
until all the involved caches are taken offline. Then all the operations will work fine from the end user perspective. 
You just need to manually restore the site when it's back online as mentioned [here](#take-site-online) .

In summary, if you expect often longer outages between sites and it's acceptable for you to have some data inconsistencies and 
not 100% accurate single-use cache, but you never want end-users to see the errors and long timeouts, then switch to `WARN` .

The difference between `WARN` and `IGNORE` is, that with `IGNORE` there are even no warnings in the JDG log. See more details in the Infinispan 
documentation.


Lock acquisition timeout
------------------------
The default configuration is using transaction in NON_DURABLE_XA mode with acquire timeout 0. This means that 
transaction will fail-fast if there is other transaction in progress for same key.

The reason for switch this to 0 instead of default 10 seconds was to avoid possible deadlock issues. With Keycloak,
it can happen that same entity (typically session entity or loginFailure) is updated concurrently from both sites.
This can cause deadlock under some circumstances, which will cause the transaction blocked for 10 seconds. See [this 
 JIRA](https://issues.jboss.org/browse/JDG-1318) for details (TODO: REMOVE THIS NOTE NOTE: It was decided on some PM call
 to mention this issue in our docs if I understood correctly).
 
With timeout 0, the transaction will immediately fail and then will be retried from Keycloak if backup `failure-policy` with 
the value `FAIL` is configured. As long as the second concurrent transaction is finished, the retry will be usually successful and entity 
will have applied updates from both concurrent transactions.

We see very good consistency and results for concurrent transaction with this configuration, so at least for now is 
recommended to keep it. 

The only (non-functional) problem is the exception in the JDG log, which happen every time when the lock is not
immediately available.



SYNC or ASYNC backups
---------------------

One important note on the `backup` element is a `strategy` attribute and decide whether it needs to be `SYNC` or `ASYNC` . Actually we have
7 caches, which might be cross-dc aware, and those can be configured in 3 different modes regarding cross-dc:
1) SYNC backup
2) ASYNC backup
3) No backup at all

If the `SYNC` backup is used, then the backup is synchronous and operation is considered finished on the caller (Keycloak server) side
once the backup is processed on the second site. This has worse performance than `ASYNC`, but on the other hand, you are sure that subsequent reads
of the particular entity (EG. user session) on `site2` will see the updates from `site1` . Also it's needed if you want data 
consistency as with `ASYNC` the caller is not notified at all if backup to the other site failed.

For some caches, it's even possible to not backup at all and completely skip writing data to the JDG server. For setup this, you can avoid
to use `remote-store` element for the particular cache on the Keycloak side (file `KEYCLOAK_HOME/standalone/configuration/standalone-ha.xml`) and 
the particular `replicated-cache` element is also not needed on the JDG side then.

By default, all 7 caches are configured with `SYNC` backup, which is the safest option. Few things to consider:

* If you are using active/passive mode (all Keycloak servers are in single site `site1` and the JDG server in `site2` is used purely as 
backup. More details [here](#modes)), then it's usually fine to use `ASYNC` strategy for all the caches to save the performance.

* The `work` cache is used mainly to send some messages (EG. cache invalidation events) to the other site. It's also used to ensure that some 
special events (EG. userStorage synchronizations) happen just on single site. It's recommended to keep it in `SYNC` strategy.

* The `actionTokens` cache is used as single-use cache to track that some tokens/tickets were used just once. For example 
[Action tokens](#infinispan-caches) or OAuth2 codes. It's possible to switch it to `ASYNC` to save some performance, but then it's not 
guaranteed that particular ticket is really single-use. For example if there is concurrent request for same ticket in both sites, then 
it's possible that both requests will be successful with the `ASYNC` strategy. So it depends if you prefer better 
security (`SYNC` strategy) or better performance (`ASYNC` strategy). 

* The `loginFailures` cache may be possibly used in all 3 modes. If there is no backup at all, it means that count of login failures for user 
(See [here](#infinispan-caches) for details) will be counted separately for every site. This has some security implications, 
however it has some performance advantages. Also it mitigates the possible risk of DoS. For example if attacker 
simulates 1000 concurrent requests of trying username/password of the user on both sites, it will mean lots of the messages 
between the sites, which may result in network congestion. The `ASYNC` strategy might be even worse as the attacker 
requests won't be blocked by waiting for the backup to the other site, resulting in potentially even bigger network traffic. 
The count of login failures also won't be accurate with the `ASYNC` strategy.

For the environments with slower network between datacenters and probability of DoS, it's recommended to not backup `loginFailures` cache at all.

* Caches `sessions` and `clientSessions` are usually recommended to keep in `SYNC` strategy. Switching them to `ASYNC` strategy is possible just
if you are sure that user requests and backchannel requests (requests from client applications to Keycloak as described [here](#request-processing)) 
will be always processed on same site. This is true for example if:
    * You use active/passive mode as described [here](#modes).
    * All your client applications are using Keycloak [Javascript Adapter](http://www.keycloak.org/docs/latest/securing_apps/index.html#_javascript_adapter). 
    Javascript adapter sends the backchannel requests within browser and hence they participate on the browser sticky session and 
    will end on same cluster node (hence on same site) as the other browser requests of this user.
    *  Loadbalancer is able to serve the requests based on client IP address (location) and the client applications are deployed on both sites.
 For example you have 2 sites LON and NYC. As long as your applications are deployed in both LON and NYC sites too, you can ensure 
 that all the user requests from London users will be redirected to the applications in LON site and also to the Keycloak servers in LON site.
 Backchannel requests from the LON site client deployments will end on Keycloak servers in LON site too. On the other hand, for the American
 users, all the Keycloak requests, application requests and backchannel requests will be processed on NYC site.
 
* For `offlineSessions` and `offlineClientSessions` it's similar. With the difference, that you even don't need to backup them at all
if you never plan to use offline tokens for any of your client applications.


Generally, if you are in doubt and the performance is not a blocker for you, it's safer to keep the caches in `SYNC` strategy.


WARNING: Regarding the switch to SYNC/ASYNC backup, make sure that you edit the `strategy` attribute of the the `backup` element. For example
like this:
```
<backup site="site2" failure-policy="FAIL" strategy="ASYNC" enabled="true">
```

Not the `mode` attribute of cache-configuration element.
ENDWARNING


Troubleshooting
===============

Few tips:

* It's recommended to go through the [example setup](#basic-setup) and have this one working first, so that you have some understanding
of how things work. It's also good to read The whole chapter to have some understanding of things.  

* Check in jconsole cluster status (GMS) and the JGroups status (RELAY) of JDG as described in [the part for JDG setup](#jdg-server-setup) .
If it doesn't look as expected, then the issue is likely in the setup of JDG servers. 

* For the keycloak servers, you should see some message like this during the server startup

```
18:09:30,156 INFO  [org.keycloak.connections.infinispan.DefaultInfinispanConnectionProviderFactory] (ServerService Thread Pool -- 54) 
Node name: node11, Site name: site1
```

Make sure that the site name and the node name looks as expected during the startup of Keycloak server.

* Make sure that Keycloak servers are in cluster as expected. Hence only the Keycloak servers from same datacenter are in cluster with each other.
This can be also checked in JConsole through the GMS view. Also look 
at [cluster troubleshooting](http://www.keycloak.org/docs/latest/server_installation/index.html#troubleshooting) for the additional details.

* Check the infinispan statistics, which are again available through JMX. For example, you can try to login and then see if the new session
was successfully written to both JDG servers and is available in the `sessions` cache there. This can be done indirectly by checking
the count of elements in the `sessions` cache for the MBean `jboss.datagrid-infinispan:type=Cache,name="sessions(repl_sync)",manager="clustered",component=Statistics` .
and attribute `numberOfEntries` . After login, there should be one more entry for `numberOfEntries` on both JDG servers on both sites.

* Enable DEBUG logging as described [here](#keycloak-servers-setup) . For example if you login and you think that the new session is not
available on the second site, it's good to see the Keycloak server logs and check that listeners were triggered as described in 
the [the setup section](#keycloak-servers-setup). If you don't know and want to ask on keycloak-user mailing list, it's good to send the log
files from Keycloak servers on both datacenters to the email. Either add the log snippets to the mails or put the logs somewhere and reference them from mail
to avoid put big attachements to the mail sent to the mailing list.

* If you updated the entity (EG. user) on Keycloak server on `site1` and you don't see that entity updated on the Keycloak server on `site2`, then
the issue can be either in the replication of the synchronous database itself or just that Keycloak caches are not properly invalidated. You may
try to temporarily disable the Keycloak caches as described [here](http://www.keycloak.org/docs/latest/server_installation/index.html#disabling-caching)
to nail down if the issue is in replicated database. Also it may help to manually connect to the database and check if data are updated 
as expected. This is specific to every database, so we won't describe here.  

* Sometimes you may see the exceptions related to locks like this in JDG log:

```
(HotRodServerHandler-6-35) ISPN000136: Error executing command ReplaceCommand, 
writing keys [[B0x033E243034396234..[39]]: org.infinispan.util.concurrent.TimeoutException: ISPN000299: Unable to acquire lock after 
0 milliseconds for key [B0x033E243034396234..[39] and requestor GlobalTx:jdg1:4353. Lock is held by GlobalTx:jdg1:4352
```

Those exceptions are not necessarily an issue. They may happen anytime when concurrent edit of same
entity is triggered on both DCs. Which can be the often case in some deployment. Usually the Keycloak is notified about the failed operation
and will retry it, so from the user's point of view, there is usually not any issue.