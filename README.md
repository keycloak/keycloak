Keycloak Documentation
======================

Open Source Identity and Access Management for modern Applications and Services.

For more information about Keycloak visit [Keycloak homepage](http://keycloak.org) and [Keycloak blog](http://blog.keycloak.org).


Building Keycloak Documentation
-------------------------------

Ensure you have [Maven installed](https://maven.apache.org/)

First clone the Keycloak Documentation repository:
    
    git clone https://github.com/keycloak/keycloak-documentation.git
    cd keycloak-documentation
    
To build Keycloak Documentation run:

    mvn clean install build
    
You can then view the documentation by opening target/frames.html or target/index.html.


Building RH-SSO Documentation
-----------------------------

Keycloak is the basis of [Red Hat Single Sign-On](https://access.redhat.com/products/red-hat-single-sign-on). The same documentation sources are used, but they are built slighty differently.

To build the documentation for RH-SSO run:

    mvn clean install build -Dproduct

You can then view the documentation by opening target/frames.html or target/index.html.


License
-------

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
