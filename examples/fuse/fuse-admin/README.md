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

1) Import 'demo' realm as mentioned in [Base steps](../README.md#base-steps) . It contains client `ssh-jmx-admin-client`, which is used for SSH authentication.
Skip this step if you installed demo already. 

2) Then you need to update/specify this property in file `$FUSE_HOME/etc/org.apache.karaf.shell.cfg`:

```
sshRealm=keycloak
```

3) Copy file from Keycloak fuse examples `keycloak-examples-<VERSION>/fuse/fuse-admin/keycloak-direct-access.json` to `$FUSE_HOME/etc/` directory.
This file contains configuration of the client application, which is used by JAAS DirectAccessGrantsLoginModule from `keycloak` JAAS realm for SSH authentication.
 
4) Start Fuse and install `keycloak` JAAS realm into Fuse. This could be done easily by installing `keycloak-jaas` feature, which has JAAS realm predefined 
(you are able to override it by using your own `keycloak` JAAS realm with higher ranking). As long as you already installed `keycloak-fuse-example` feature as mentioned 
in [examples readme](../README.md), you can skip this step as `keycloak-jaas` is installed already. Otherwise use those commands (replace Keycloak version in this command with the current version):

```
features:addurl mvn:org.keycloak/keycloak-osgi-features/1.2.0.Beta1/xml/features
features:install keycloak-jaas
```

5) Now let's type this from your terminal to login via SSH as `admin` user:

```
ssh -o PubkeyAuthentication=no -p 8101 admin@localhost
```

And login with password `password` . Note that other users from "demo" realm like bburke@redhat.com don't have SSH access as they don't have `admin` role.
 

JMX authentication with keycloak credentials on JBoss Fuse 6.1
--------------------------------------------------------------

This may be needed in case if you really want to use jconsole or other external tool to perform remote connection to JMX through RMI. Otherwise it may 
be better to use just hawt.io/jolokia as jolokia agent is installed in hawt.io by default.
 
1) In file `$FUSE_HOME/etc/org.apache.karaf.management.cfg` you can change this property:

```
jmxRealm=keycloak
```

2) In jconsole you can fill URL like:

```
service:jmx:rmi://localhost:44444/jndi/rmi://localhost:1099/karaf-root
```

and credentials: admin/password

Note again that users without `admin` role are not able to login as they are not authorized. However users with access to Hawt.io admin console 
may be still able to access MBeans remotely via HTTP (Hawtio). So make sure to protect Hawt.io web console with same roles like JMX through RMI to 
really protect JMX mbeans.


SSH and JMX on JBoss Fuse 6.2 
-----------------------------
For SSH steps are very similar to above for 6.1. In JBoss Fuse 6.2 you may need to install `ssh` feature as it doesn't seem to be installed here by default.

```
features:install ssh
```

For JMX, the steps are similar like for Fuse 6.1, however there is more fine grained authorization for JMX access in Fuse 6.2.

Actually if you login as user `admin`, you have very limited privileges without possibility to do much JMX operations as this user has just `admin` role, which is not allowed to do much in JMX.

However if you login as user `jmxadmin` with password `password`, you will have all JMX privileges! This user has composite role `jmxAdmin`, which is mapped to
all possible roles used in JMX authorization files like `etc/jmx.acl.*.cfg` . See karaf documentation for more info about fine grained JMX authorization.

