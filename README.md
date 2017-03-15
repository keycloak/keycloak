Keycloak Documentation
======================

Open Source Identity and Access Management for modern Applications and Services.

For more information about Keycloak visit [Keycloak homepage](http://keycloak.org) and [Keycloak blog](http://blog.keycloak.org).


Building Keycloak Documentation
-------------------------------

Ensure you have [GitBook installed](https://github.com/GitbookIO/gitbook/blob/master/docs/setup.md)

First clone the Keycloak Documentation repository:
    
    git clone https://github.com/keycloak/keycloak-documentation.git
    cd keycloak-documentation
    
To build Keycloak Documentation run:

    gitbook build
    
You can then view the documentation by opening _book/index.html.

Alternatively you can also have the Gitbook tools continiously re-build when you do changes. To do this run:

    gitbook serve

You can then view the documentation on http://localhost:4000.



Building RH-SSO Documentation
-----------------------------

Keycloak is the basis of [Red Hat Single Sign-On](https://access.redhat.com/products/red-hat-single-sign-on). The same documentation sources are used, but they are built slighty differently.

To build the documentation for RH-SSO go into the directory of the specific guide you want to build. For example to build Server Admin guide run:

    cd server_admin
    python gitlab-conversion.py
    cd target
    asciidoctor master.adoc

You can then view the documentation by opening server_admin/target/master.html

This will not create documentation that looks exactly as the official Red Hat Single Sign-On documentation, but the content will be the same.


License
-------

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
