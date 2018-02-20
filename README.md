Keycloak Documentation
======================

Open Source Identity and Access Management for modern Applications and Services.

For more information about Keycloak visit the [Keycloak homepage](http://keycloak.org) and [Keycloak blog](http://blog.keycloak.org).


Contributing to Keycloak Documentation
--------------------------------------

See our [Contributor's Guide](internal_resources/contributing.adoc). The directory also includes a set of templates and other resources to help you get started.

If you want to file a bug report or tell us about any other issue with this documentation, you are invited to please use our [issue tracker](https://issues.jboss.org/projects/KEYCLOAK/).


Building Keycloak Documentation
-------------------------------

Ensure that you have [Maven installed](https://maven.apache.org/).

First, clone the Keycloak Documentation repository:

    git clone https://github.com/keycloak/keycloak-documentation.git
    cd keycloak-documentation

To build Keycloak Documentation run:

    mvn clean install

Or to build a specific guide run:

    mvn clean install -f GUIDE_DIR

You can then view the documentation by opening GUIDE_DIR/target/frames.html or GUIDE_DIR/target/index.html.


Building RH-SSO Documentation
-----------------------------

Keycloak is the basis of [Red Hat Single Sign-On](https://access.redhat.com/products/red-hat-single-sign-on). The same documentation sources are used, but they are built slighty differently.

To build the documentation for RH-SSO run:

    mvn clean install -Dproduct

Or to build a specific guide run:

    mvn clean install -Dproduct -f GUIDE_DIR

You can then view the documentation by opening GUIDE_DIR/target/frames.html or GUIDE_DIR/target/index.html.

If you have ccutil installed you can build the guides with:

    ./build-product.sh

Or individual guides with:

    ./build-product.sh GUIDE_DIR

License
-------

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
