Keycloak Fuse demo
==================

Currently Keycloak supports securing your web applications running inside [JBoss Fuse](http://www.jboss.org/products/fuse/overview/) or [Apache Karaf](http://karaf.apache.org/). It leverages:
- Jetty9 adapter for both JBoss Fuse 6.3 and Apache Karaf 4, that include [Jetty9](http://eclipse.org/jetty/) server under the covers and Jetty is used for running various kinds of web applications
- Jetty8 adapter for both JBoss Fuse 6.2 and Apache Karaf 3, that include [Jetty8](http://eclipse.org/jetty/) server under the covers and Jetty is used for running various kinds of web applications

It's highly recommended to use the JBoss Fuse 6.3.0 Rollup 1 or newer for this tutorial.

**WARNING:** Running your applications inside standalone Apache Karaf may work, however we are testing just with JBoss Fuse 6.3.0 Rollup 1 and not with standalone Karaf server. Also we  
did not test with Fuse versions older than 6.3.0 Rollup 1. So if you really want adapter on standalone Karaf server or older Fuse, it's up to you to figure exact steps to have it working.

The Fuse example is slightly modified version of Keycloak base demo applications. The main difference among base demo is that for Fuse demo 
are applications running on separate Fuse server. Keycloak server is supposed to run separately on Wildfly.
 
Fuse demo contains those basic applications:
* **customer-app-fuse** A WAR application that is deployed with [pax-war extender](https://ops4j1.jira.com/wiki/display/ops4j/Pax+Web+Extender+-+War)
* **product-app-fuse** A servlet application deployed with [pax-whiteboard extender](https://ops4j1.jira.com/wiki/display/ops4j/Pax+Web+Extender+-+Whiteboard)
* **camel** [Apache Camel](http://camel.apache.org/) endpoint running on separate Jetty engine on [http://localhost:8383/admin-camel-endpoint](http://localhost:8383/admin-camel-endpoint). 
The customer-app-fuse invokes the endpoint to get data.     
* **cxf-jaxrs** [Apache CXF](http://cxf.apache.org/) JAX-RS endpoint running on default Jetty on [http://localhost:8181/cxf/customerservice](http://localhost:8181/cxf/customerservice). 
The customer-app-fuse invokes the endpoint to get data 
* **cxf-jaxws** [Apache CXF](http://cxf.apache.org/) JAX-WS endpoint running on separate Jetty engine on [http://localhost:8282/PersonServiceCF](http://localhost:8282/PersonServiceCF). 
The product-app-fuse invokes the endpoint to get data.
* **external-config** A WAR application with external adapter configuration not bundled within the application, but instead in `$FUSE_HOME/etc` directory.

Running of demo consists of 2 steps. First you need to run separate Keycloak server and then Fuse server with the applications

Base steps
----------

* Run external instance of Keycloak server on WildFly . It's easiest to run and download Keycloak standalone server. Fuse demo suppose that server is running on [http://localhost:8080/auth](http://localhost:8080/auth)
* Import realm `demo` from the file `demorealm.json` on `examples/fuse/demorealm.json` . See [here](../demo-template/README.md#step-3-import-the-test-realm) 
the details on how to import the realm
* Then download Keycloak examples and build Fuse example, which is needed so the feature repository is added to your local maven repo:

```
unzip -q keycloak-examples-<VERSION>.zip
cd keycloak-examples-<VERSION>/fuse
mvn clean install
```


Running demo on JBoss Fuse 6.3.0 Rollup 1
------------------------------
You just need to download and run JBoss Fuse and then run those commands from the karaf terminal to install the needed features and Keycloak fuse demo (Replace Keycloak versions with the current Keycloak version number):

```
KEYCLOAK_VERSION="2.2.1.Final"
features:addurl mvn:org.keycloak/keycloak-osgi-features/$KEYCLOAK_VERSION/xml/features
features:addurl mvn:org.keycloak.example.demo/keycloak-fuse-example-features/$KEYCLOAK_VERSION/xml/features
features:install keycloak-fuse-6.3-example
```

After that you can test running on [http://localhost:8181/customer-portal](http://localhost:8181/customer-portal) and login as "bburke@redhat.com" with password "password". Customer-portal is able to
receive the response from the endpoints provided by `cxf-jaxrs` and `camel` applications. Note that camel endpoint is available just for users with role `admin`
in this demo, so "bburke@redhat.com" can't access it. You may login as "admin" with password "password" in order to invoke camel endpoint.

From [http://localhost:8181/product-portal](http://localhost:8181/product-portal) you will see servlet endpoint, which invokes JAX-WS provided by `cxf-jaxws` application.

Note that this demo also secures whole default CXF endpoint on [http://localhost:8181/cxf](http://localhost:8181/cxf) hence every application running under it is secured too.

To have the `external-config` example running, you can copy the file `examples/fuse/external-config/external-config-keycloak.json` to the `$FUSE_HOME/etc` directory. 
Then go to [http://localhost:8181/external-config/index.html](http://localhost:8181/external-config/index.html) to test the secured application.

How to secure your own application
----------------------------------
See [Docs](https://keycloak.gitbooks.io/securing-client-applications-guide/content/v/latest/topics/oidc/java/fuse-adapter.html) for more details. 


How to secure Fuse admin services
---------------------------------
It's possible to secure fuse admin services with Keycloak too. See [fuse-admin](fuse-admin/README.md) for info on how to secure
Fuse admin console, remote SSH and JMX access with Keycloak.

  