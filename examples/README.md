Keycloak Examples
=================

This directory contains a number of examples for Keycloak.

Demo
----

This is the our main example, which shows Login, Single-Sign On, Single-Sign Out and OAuth Token Grant. The demo comes in two flavours: 

* Preconfigured - Use this flavour to quickly deploy the demo to a local Keycloak server without having to configure anything other than importing the realm settings into Keycloak
* Unconfigured - Use this flavour to manually configure and deploy the demo to either a local or external Keycloak server

For more information look at `preconfigured-demo/README.md` or `unconfigured-demo/README.md`. For the unconfigured-demo there's an accompanying screencast at [http://keycloak.org/docs](http://keycloak.org/docs) that walks you through the setup process.


Admin Client
------------

Example using the Admin Client. For more information look at `admin-client/README.md`.


Cordova
-------

Example Cordova application. For more information look at `cordova/README.md`.


CORS
----

Example CORS application. For more information look at `cors/README.md`.


JS Console
----------

Example JavaScript application that let's you experiment with the JavaScript adapter. For more information look at `js-console/README.md`.


Providers
---------

Example providers for Event Listener SPI, Event Store SPI and User Federation SPI. For more information look at:

* Event Listener that outputs event detalis to sysout - `providers/event-listener-sysout/README.md`
* Event Store that stores events in memory - `providers/event-store-mem/README.md`
* User Federation that loads users from a text file - `providers/federation-provider/README.md`


Themes
------

Example themes to change the look and feel of login forms, account management console and admin console. For more information look at `themes/README.md`.


Multi tenancy
-------------

A complete application, showing how to achieve multi tenancy of web applications by using one realm per account. For more information look at `multi-tenant/README.md`
