How to secure Fuse admin services
=================================

Fuse admin console authentication on JBoss Fuse 6.3.0 Rollup 1 or newer
---------------------------------
Fuse admin console is Hawt.io. Follow the instructions in [Docs](https://keycloak.gitbooks.io/securing-client-applications-guide/content/v/latest/topics/oidc/java/fuse/hawtio.html) for details on how to integrate it.

Example steps:

1) Import `demo` realm as mentioned in [Base steps](../README.md#base-steps) . It contains `hawtio-client` and some example users.

2) Copy files [keycloak-hawtio.json](keycloak-hawtio.json) and [keycloak-hawtio-client.json](keycloak-hawtio-client.json) to the `$FUSE_HOME/etc/` directory.

3) Edit properties in `$FUSE_HOME/etc/system.properties` as described in the documentation pointed above.

3) Run Fuse and install `keycloak` feature in the terminal as described in the documentation pointed above.

4) Test. After going to `http://localhost:8181/hawtio`  you can login as any of these users. Password of all the sample users is `password` :
* root - He has role `admin` . He can access to everything in Hawtio
* john - He has role `viewer` . He can access to man functionalities in Hawtio.
* mary - She is not able to successfully authenticate to Hawtio


SSH authentication with keycloak credentials on JBoss Fuse 6.3.0 Rollup 1 or newer
-----------------------------------------------------------------------

Follow the instructions in [Docs](https://keycloak.gitbooks.io/securing-client-applications-guide/content/v/latest/topics/oidc/java/fuse/fuse-admin.html) for details
  
Example steps for enable SSH authentication:

1) Import `demo` realm as mentioned in [Base steps](../README.md#base-steps) . It contains `ssh-jmx-admin-client` and some example users.

2) Then you need to update/specify this property in file `$FUSE_HOME/etc/org.apache.karaf.shell.cfg` as mentioned in the docs pointed above.

3) Copy file from Keycloak fuse examples [keycloak-direct-access.json](keycloak-direct-access.json) to `$FUSE_HOME/etc/` directory.
 
4) Start Fuse and install `keycloak` JAAS realm into Fuse as mentioned in the docs pointed above.

5) Try to login into SSH as different users with the command shown in the docs pointed above. Password of all the sample users is `password` :
* root - He can run any command in Fuse Karaf SSH terminal
* john - He can run just read-only commands (eg. `features:list` ) but not write command (eg. `features:addurl` ).
* mary - She is not able to successfully authenticate to SSH
 

JMX authentication with keycloak credentials on JBoss Fuse 6.3.0 Rollup 1 or newer
-----------------------------------------------------------------------

See [Docs](https://keycloak.gitbooks.io/securing-client-applications-guide/content/v/latest/topics/oidc/java/fuse/fuse-admin.html) for details
 
You can use file [keycloak-direct-access.json](keycloak-direct-access.json) to be copied into `$FUSE_HOME/etc/` as mentioned above in the SSH section. You can 
also test with same users.
