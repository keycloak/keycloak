How to test Keycloak cluster with Docker
========================================
Docker+Fig allows to easily setup and test the whole environment with:
* Apache HTTPD 2.4 + modcluster 1.3 as Load Balancer
* MySQL 5.6.1 as database
* Various number of Keycloak cluster nodes running on WildFly with "demo" examples deployed. (See below for EAP 6.3 and AS7)

You don't need to setup Apache with modcluster + MySQL on your laptop as Docker will do it for you and all will run in Docker containers.

Steps to setup
--------------
1) Download and install [Docker](https://docs.docker.com/installation) and [Fig](http://www.fig.sh/install.html)

2) Build Keycloak including distribution. This will be used by Docker+Fig. The point is that you can test clustering stuff from latest Keycloak master:
```shell 
$ cd $KEYCLOAK_HOME
$ mvn clean install
$ cd distribution
$ mvn clean install
````

3) Build Docker with maven to ensure that needed data will be accessible to Docker+Fig volumes: 
```shell 
$ cd $KEYCLOAK_HOME/testsuite/docker-cluster
$ mvn clean install
````
 
4) Build fig and run the whole env. By default it will run Apache + MySQL + 1 Keycloak node:
```shell
$ fig build
$ fig up
````

First build will take long time as it need to download bunch of stuff and install into Docker container. Next builds will be much faster due to Docker cache.
After some time, WildFly server is started

Testing
-------

Apache is running in separate container and have 2 ports exposed locally: 10001 and 8000. Port 10001 is for modCluster - you should 
be able to access Apache modCluster status page: [http://localhost:10001/mod_cluster_manager](http://localhost:10001/mod_cluster_manager) and see one node
with deployed "auth-server.war" and few other WARs (keycloak demo). 

Also you can access Keycloak admin console via loadBalancer on [http://localhost:8000/auth/admin](http://localhost:8000/auth/admin) and similarly Account mgmt. 

MySQL can be directly accessed from your machine (if you have MySQL client installed):
```shell
$ mysql -h127.0.0.1 -P33306 -uroot -pmysecretpassword
````
Used database is "keycloak_db"

Remote debugging
----------------

With command:
```shell
$ docker ps
````
 
You can see running ports. For the Keycloak node you may see output similar to this:
```shell
0.0.0.0:49153->8080/tcp, 0.0.0.0:49154->8787/tcp, 0.0.0.0:49155->9990/tcp
````

This means that you can directly access Keycloak (bypass loadbalancer) by going to [http://localhost:49153/auth/admin](http://localhost:49153/auth/admin) . 
Also it means that debugger is mapped From Docker port 8787 to local port 49154 . So in your IDE you can connect with settings similar to:
```shell
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=49154
````

Scale / more cluster nodes
--------------------------

Run this in separate terminal to add more (in this case 2) cluster nodes:
```shell
$ fig scale wfnode=2
````

Now it should be visible on mod_cluster_manager page that they are 2 nodes.

Seeing logs
-----------
It's easiest to do:
```shell
$ fig logs
````
to see output of MySql and Keycloak server consoles.

To see Apache and debug logs of keycloak server:
```shell
$ fig run wfnode /bin/bash
````
  
Then you're in shell inside docker container, which has some mounted volumes with apache logs and keycloak nodes. Apache logs are at:
```shell
$ cd /apachelogs/
````

Keycloak nodes are at (debug logging enabled by default for "org.keycloak"):
```shell
$ cd /keycloak-docker/shared
````
 
Restart whole environment
-------------------------

Just run:
```shell
$ fig stop
$ fig start
````

This will restart apache + MySQL + all nodes, but won't clear data.

Changing configuration and clear data
-------------------------------------
Changing configuration (for example UserSession provider from 'mem' to 'jpa') is possible in
```shell
$KEYCLOAK_HOME/testsuite/docker-cluster/target/keycloak-docker-cluster/deployments/auth-server.war/WEB-INF/classes/META-INF/keycloak-server.json
````

then whole environment needs to be stopped, containers removed (in order to update configuration in nodes) and started again:
```shell 
$ fig stop
$ fig rm
$ fig up
````
 
Rebuilding after changed sources
-------------------------------
In this case you might need to stop and remove existing containers. Then start from step 2 (Rebuild Keycloak or at least 
changed jars, then rebuild distribution and testsuite/docker-cluster 
(or just copy changed JAR into $KEYCLOAK_HOME/testsuite/docker-cluster/target/keycloak-docker-cluster/deployments/auth-server.war/WEB-INF/lib if it's not adapter stuff. 
But 'fig rm' is safer to call anyway)

Test with Keycloak and examples on EAP 6.3
------------------------------------------
Steps are quite similar like for WildFly but we need to pass different file "fig-eap63.yml" instead of default "fig.yml" which is used for WildFly. 
Also name of the node is "eapnode" instead of "wfnode". 
 
So your commands will look like
```shell 
$ fig -f fig-eap63.yml build
$ fig -f fig-eap63.yml up
$ fig -f fig-eap63.yml scale eapnode=2
```` 
and viceversa.
 
Test with Keycloak and examples on AS 7.1.1
-------------------------------------------
Also arguments need to be passed with different fig file and node name: TODO: AS7 cluster setup doesn't work correctly yet
 
 ```shell 
$ fig -f fig-as7.yml build
$ fig -f fig-as7.yml up
$ fig -f fig-as7.yml scale asnode=2
````