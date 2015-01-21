How to secure Fuse admin services
=================================

Fuse admin console authentication
---------------------------------
Fuse admin console is Hawt.io. See [Hawt.io documentation](http://hawt.io/docs/index.html) for more info about how to secure it with keycloak.


SSH authentication with keycloak credentials on JBoss Fuse 6.1
--------------------------------------------------------------

Keycloak mainly addresses usecases for authentication of web applications, however if your admin services (like fuse admin console) are protected
with Keycloak, it may be good to protect non-web services like SSH with Keycloak credentials too. It's possible to do it by using JAAS login module, which
allows to remotely connect to Keycloak and verify credentials based on [Direct access grants](http://docs.jboss.org/keycloak/docs/1.1.0.Beta2/userguide/html/direct-access-grants.html).
  
Example steps for enable SSH authentication:

1) Import 'demo' realm as mentioned in [Base steps](../README.md#base-steps) . It contains client `sh-jmx-admin-client`, which is used for SSH authentication.
Skip this step if you installed demo already. 

2) Then you need to update/specify these 2 properties in file `$FUSE_HOME/etc/org.apache.karaf.shell.cfg`:

```
sshRealm=keycloak
sshRole=org.keycloak.adapters.jaas.RolePrincipal:admin
```

3) Copy file from Keycloak fuse examples `examples/fuse/fuse-admin/keycloak-direct-access.json` to `$FUSE_HOME/etc/` directory. 
This file contains configuration of the client application, which is used by JAAS DirectAccessGrantsLoginModule from `keycloak` JAAS realm for SSH authentication.
 
4) Start Fuse and install `keycloak` JAAS realm into Fuse. This could be done easily by installing `keycloak-jaas` feature, which has JAAS realm predefined 
(you are able to override it by using your own `keycloak` JAAS realm with higher ranking). As long as you already installed `keycloak-fuse-example` feature as mentioned 
in [examples readme](../README.md), you can skip this step as `keycloak-jaas` is installed already. Otherwise use those commands:

```
features:addurl mvn:org.keycloak/keycloak-osgi-features/1.1.0.Final/xml/features
features:install keycloak-jaas
```

5) Now let's type this from your terminal:

```
ssh -o PubkeyAuthentication=no -p 8101 admin@localhost
```

And login with password `password` . Note that other users from "demo" realm like bburke@redhat.com don't have SSH access as they don't have `admin` role.
 

JMX authentication with keycloak credentials on JBoss Fuse 6.1
--------------------------------------------------------------

This may be needed just in case if you really want to use jconsole or other external tool to perform remote connection to JMX through RMI. Otherwise it may 
be better to use just hawt.io/jolokia as jolokia agent is installed in hawt.io by default.
 
1) In file `$FUSE_HOME/etc/org.apache.karaf.management.cfg` you can change these 2 properties:

```
jmxRealm=keycloak
jmxRole=org.keycloak.adapters.jaas.RolePrincipal:admin
```

2) In jconsole you can fill URL like:

```
service:jmx:rmi://localhost:44444/jndi/rmi://localhost:1099/karaf-root
```

and credentials: admin/password

Note again that users without `admin` role are not able to login as they are not authorized. However users with access to Hawt.io admin console 
may be still able to access MBeans remotely via HTTP (Hawtio). So make sure to protect Hawt.io web console with same roles like JMX through RMI to 
really protect JMX mbeans.


JMX on JBoss Fuse 6.2 and Apache Karaf 3.0.2
--------------------------------------------

For JMX, the steps are similar like above, however there is more fine grained authorization for JMX access in Fuse 6.2. You still need to configure jmxRealm in
`$FUSE_HOME/etc/org.apache.karaf.management.cfg`, however jmxRole is not needed. So in config should be just this:

```
jmxRealm=keycloak
```
And then you need to configure authorization for all specific MBeans, which you want to access with authenticated user. For generic access 
you can configure the most generic ACL in file `$FUSE_HOME/etc/jmx.acl.cfg` like this: 

```
list* = org.keycloak.adapters.jaas.RolePrincipal:admin
get* = org.keycloak.adapters.jaas.RolePrincipal:admin
is* = org.keycloak.adapters.jaas.RolePrincipal:admin
set* = org.keycloak.adapters.jaas.RolePrincipal:admin
* = org.keycloak.adapters.jaas.RolePrincipal:admin
```

Then admin will have permissions to see all the attributes and trigger all JMX operations, which don't have their own ACL. You can configure
those specific ACLs too. For example if you want to allow operation `lookupAgents` in MBean `jolokia:type=Discovery` you can configure 
in file `$FUSE_HOME/etc/jmx.acl.jolokia.Discovery.cfg` the property like this:
  
```
lookupAgents = org.keycloak.adapters.jaas.RolePrincipal:admin
```
