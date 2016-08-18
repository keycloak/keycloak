Keycloak Fuse demo
==================

Currently Keycloak supports securing your web applications running inside [JBoss Fuse](http://www.jboss.org/products/fuse/overview/) or [Apache Karaf](http://karaf.apache.org/). It leverages:
- Jetty8 adapter for both JBoss Fuse 6.2 and Apache Karaf 3, that include [Jetty8](http://eclipse.org/jetty/) server under the covers and Jetty is used for running various kinds of web applications
- Jetty9 adapter for both JBoss Fuse 6.3 and Apache Karaf 4, that include [Jetty9](http://eclipse.org/jetty/) server under the covers and Jetty is used for running various kinds of web applications

**WARNING:** Running your applications inside standalone Apache Karaf may work, however we are testing just with JBoss Fuse and not with standalone Karaf server. 
So if you really want adapter on standalone Karaf server, it's up to you to figure exact steps to have it working.

The Fuse example is slightly modified version of Keycloak base demo applications. The main difference among base demo is that for Fuse demo 
are applications running on separate Fuse server. Keycloak server is supposed to run separately on Wildfly.

What is supported for Fuse is:
* Security for classic WAR applications deployed on Fuse with [pax-war extender](https://ops4j1.jira.com/wiki/display/ops4j/Pax+Web+Extender+-+War). 
* Security for servlets deployed on Fuse as OSGI services with [pax-whiteboard extender](https://ops4j1.jira.com/wiki/display/ops4j/Pax+Web+Extender+-+Whiteboard).
* Security for [Apache Camel](http://camel.apache.org/) Jetty endpoints running with [camel-jetty](http://camel.apache.org/jetty.html) component.
* Security for [Apache CXF](http://cxf.apache.org/) endpoints running on their own separate [Jetty engine](http://cxf.apache.org/docs/jetty-configuration.html). 
Supports both securing JAX-RS and JAX-WS endpoints.
* Security for [Apache CXF](http://cxf.apache.org/) endpoints running on default engine provided by CXF servlet on [http://localhost:8181/cxf](http://localhost:8181/cxf) . NOTE: Actually It's better and
more secure to use the separate Jetty Engine instead of the default one. The default engine works fine, but I would recommend the separate one.
 
Fuse demo contains those basic applications:
* **customer-app-fuse** A WAR application that is deployed with [pax-war extender](https://ops4j1.jira.com/wiki/display/ops4j/Pax+Web+Extender+-+War)
* **product-app-fuse** A servlet application deployed with [pax-whiteboard extender](https://ops4j1.jira.com/wiki/display/ops4j/Pax+Web+Extender+-+Whiteboard)
* **camel** [Apache Camel](http://camel.apache.org/) endpoint running on separate Jetty engine on [http://localhost:8383/admin-camel-endpoint](http://localhost:8383/admin-camel-endpoint). 
The customer-app-fuse invokes the endpoint to get data.     
* **cxf-jaxrs** [Apache CXF](http://cxf.apache.org/) JAX-RS endpoint running on default Jetty on [http://localhost:8181/cxf/customerservice](http://localhost:8181/cxf/customerservice). 
The customer-app-fuse invokes the endpoint to get data 
* **cxf-jaxws** [Apache CXF](http://cxf.apache.org/) JAX-WS endpoint running on separate Jetty engine on [http://localhost:8282/PersonServiceCF](http://localhost:8282/PersonServiceCF). 
The product-app-fuse invokes the endpoint to get data.

Running of demo consists of 2 steps. First you need to run separate Keycloak server and then Fuse server with the applications

Base steps
----------

* Run external instance of Keycloak server on WildFly . It's easiest to run and download Keycloak standalone server. Fuse demo suppose that server is running on [http://localhost:8080/auth](http://localhost:8080/auth)
* Import realm `demo` from the file testrealm.json on `examples/fuse/testrealm.json` . See [here](../demo-template/README.md#step-3-import-the-test-realm) 
the details on how to import the realm
* Then download Keycloak examples and build Fuse example, which is needed so the feature repository is added to your local maven repo:

```
unzip -q keycloak-examples-<VERSION>.zip
cd keycloak-examples-<VERSION>/fuse
mvn clean install
```

Running demo on JBoss Fuse 6.2.1 or JBoss Fuse 6.2.0
----------------------------------------------------
You just need to download and run JBoss Fuse and then run those commands from the karaf terminal to install the needed features and Keycloak fuse demo (Replace Keycloak versions with the current Keycloak version number):

```
features:addurl mvn:org.keycloak/keycloak-osgi-features/1.9.4.Final/xml/features
features:addurl mvn:org.keycloak.example.demo/keycloak-fuse-example-features/1.9.4.Final/xml/features
features:install keycloak-fuse-6.2-example
```

After that you can test running on [http://localhost:8181/customer-portal](http://localhost:8181/customer-portal) and login as "bburke@redhat.com" with password "password". Customer-portal is able to
receive the response from the endpoints provided by `cxf-jaxrs` and `camel` applications. Note that camel endpoint is available just for users with role `admin`
in this demo, so "bburke@redhat.com" can't access it. You may login as "admin" with password "password" in order to invoke camel endpoint.

From [http://localhost:8181/product-portal](http://localhost:8181/product-portal) you will see servlet endpoint, which invokes JAX-WS provided by `cxf-jaxws` application.

Note that this demo also secures whole default CXF endpoint on [http://localhost:8181/cxf](http://localhost:8181/cxf) hence every application running under it is secured too.


Running demo on JBoss Fuse 6.3
------------------------------
Similar steps to the instructions for JBoss Fuse 6.2.1 but you need to install a different feature (due to the usage of Jetty9 instead of Jetty8): `keycloak-fuse-6.3-example`

You just need to download and run JBoss Fuse and then run those commands from the karaf terminal to install the needed features and Keycloak fuse demo (Replace Keycloak versions with the current Keycloak version number):

```
features:addurl mvn:org.keycloak/keycloak-osgi-features/1.9.4.Final/xml/features
features:addurl mvn:org.keycloak.example.demo/keycloak-fuse-example-features/1.9.4.Final/xml/features
features:install keycloak-fuse-6.3-example
```


How to secure your own applications
-----------------------------------
Most of the steps should be understandable from testing and understanding the demo. Basically all mentioned applications require to
 inject Keycloak Jetty authenticator into underlying Jetty server . The steps are bit different according to application type.

**Classic WAR application** - Take a look at `customer-portal-app` for inspiration. The needed steps are:
* Declare needed constraints in `/WEB-INF/web.xml` 
* Add `jetty-web.xml` file with the authenticator to `/WEB-INF/jetty-web.xml` and add `/WEB-INF/keycloak.json` with your Keycloak configuration
* Make sure your WAR imports `org.keycloak.adapters.jetty` and maybe some more packages in MANIFEST.MF file in header `Import-Package`. It's 
recommended to use maven-bundle-plugin similarly like Fuse examples are doing, but note that "*" resolution for package doesn't import `org.keycloak.adapters.jetty` package 
as it's not used by application or Blueprint or Spring descriptor, but it's used just in jetty-web.xml file.
 
**Servlet web application deployed by pax-whiteboard-extender** - Take a look at `product-portal-app` for inspiration. The needed steps are:
* Keycloak provides PaxWebIntegrationService, which allows to inject jetty-web.xml and configure security constraints for your application. 
Example `product-portal-app` declares this in `OSGI-INF/blueprint/blueprint.xml` . Note that your servlet needs to depend on it. 
* Steps 2,3 are same like for classic WAR
 
**Apache camel application** - You can secure your Apache camel endpoint using [camel-jetty](http://camel.apache.org/jetty.html) endpoint by adding securityHandler with KeycloakJettyAuthenticator and
proper security constraints injected. Take a look at `OSGI-INF/blueprint/blueprint.xml` configuration in `camel` application on example of how it can be done. 

**Apache CXF endpoint** - It's recommended to run your CXF endpoints secured by Keycloak on separate Jetty engine. Application `cxf-ws` is using separate endpoint on
[http://localhost:8282](http://localhost:8282) . All the important configuration is declared in cxf-jaxws app in `META-INF/spring/beans.xml` .
     
**Builtin web applications** - Some services automatically come with deployed servlets on startup. One of such examples is CXF servlet running on 
[http://localhost:8181/cxf](http://localhost:8181/cxf) context. Securing such endpoints is quite tricky. The approach, which Keycloak is currently using, 
is providing ServletReregistrationService, which undeploys builtin servlet at startup, so you are able to re-deploy it again on context secured by Keycloak. 
You can see the `OSGI-INF/blueprint/blueprint.xml` inside `cxf-jaxrs` project, which adds JAX-RS "customerservice" endpoint and more importantly, it secures whole `/cxf` context. 

As a side effect, all other CXF services running on default CXF HTTP destination will be secured too. Once you uninstall feature "keycloak-fuse-example" the 
original unsecured servlet on `/cxf` context is deployed back and hence context will become unsecured again. 

It's recommended to use your own Jetty engine for your apps (similarly like `cxf-jaxws` application is doing).


How to secure WAR application with external keycloak.json configuration
-----------------------------------------------------------------------
It's possible to secure your WAR application with the `keycloak.json` configuration provided outside of the WAR bundle itself. 
See [external-config](external-config/README.md) for more details. This is supported on JBoss Fuse 6.3.


How to secure Fuse admin services
---------------------------------
It's possible to secure fuse admin services with Keycloak too. See [fuse-admin](fuse-admin/README.md) for info on how to secure
Fuse admin console, remote SSH and JMX access with Keycloak.

  