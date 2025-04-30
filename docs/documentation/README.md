Keycloak Documentation
======================

Open Source Identity and Access Management for modern Applications and Services.

For more information about Keycloak visit the [Keycloak homepage](http://keycloak.org) and [Keycloak blog](https://www.keycloak.org/blog).


Contributing to Keycloak Documentation
----------------------------------------

See our [Contributor's Guide](internal_resources/contributing.adoc). The directory also includes a set of templates and other resources to help you get started.

If you want to file a bug report or tell us about any other issue with this documentation, you are invited to please use our [issue tracker](https://github.com/keycloak/keycloak/issues/).


Building Keycloak Documentation
---------------------------------

Ensure that you have [Maven installed](https://maven.apache.org/).

First, clone the Keycloak repository:

    git clone https://github.com/keycloak/keycloak.git
    cd keycloak/docs/documentation

If you are using Windows, you need to run the following command with administrator privilege because this project uses symbolic links:

    git clone -c core.symlinks=true https://github.com/keycloak/keycloak.git

To build Keycloak Documentation run:

    ./mvnw clean install -am -pl docs/documentation/dist -Pdocumentation

Or to build a specific guide run:

    ./mvnw clean install -pl docs/documentation/GUIDE_DIR -Pdocumentation
    
By default, an archive version of the documentation is built. To build the latest build run:

    ./mvnw clean install ... -Platest,documentation

You can then view the documentation by opening `docs/documentation/GUIDE_DIR/target/generated-docs/index.html`.


License
-------

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
