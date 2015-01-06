1) First step is to run Keycloak server on localhost:8080 and import realm "demo" from the file testrealm.json in this directory (Directory "fuse").

Running example on Karaf 3.0.2
------------------------------

feature:repo-add mvn:org.apache.camel.karaf/apache-camel/2.12.5/xml/features
feature:repo-add mvn:org.apache.cxf.karaf/apache-cxf/2.7.14/xml/features
feature:repo-add mvn:org.keycloak/keycloak-osgi-features/1.1.0.Final/xml/features
feature:repo-add mvn:org.keycloak.example.demo/keycloak-fuse-example-features/1.1.0.Final/xml/features
feature:install keycloak-fuse-example

Running example on JBoss Fuse 6.1.0.redhat-379
----------------------------------------------

features:uninstall pax-war
features:uninstall pax-http-whiteboard 
features:uninstall pax-http
features:uninstall pax-jetty
features:removeurl mvn:org.ops4j.pax.web/pax-web-features/3.0.6/xml/features
features:addurl mvn:org.ops4j.pax.web/pax-web-features/3.1.2/xml/features

features:addurl mvn:org.keycloak/keycloak-osgi-features/1.1.0.Final/xml/features
features:addurl mvn:org.keycloak.example.demo/keycloak-fuse-example-features/1.1.0.Final/xml/features

features:install keycloak-pax-web-upgrade
features:install pax-http-whiteboard/3.1.2
features:install pax-war/3.1.2

features:uninstall cxf
features:install cxf

features:install keycloak-fuse-example

