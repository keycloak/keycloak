# Keycloak SAML

## Introduction

These examples show using Keycloak Server's SAML support with Keycloak's SAML client adapter.  These examples run on JBoss Enterprise Application Platform 6 or WildFly.

We recommend using the Keycloak Demo Distribution to test the examples as it has already some things pre-set for you. 
There is individual README.md file specific for each example. Here are just some general info about the requirements for running the examples.

## Start the Demo Distribution

See the documentation for more details.

## Import the test realm

Next thing you have to do is import the test realm for these examples.  Clicking on the below link will bring you to the
create realm page in the Admin UI.  The username/password is admin/admin to login in.  Keycloak will ask you to
create a new admin password before you can go to the create realm page.

[http://localhost:8080/auth/admin/master/console/#/create/realm](http://localhost:8080/auth/admin/master/console/#/create/realm)

Import the testsaml.json file that is in the saml/ example directory.

## Build and Deploy

```
$ mvn clean install wildfly:deploy
```

