# Keycloak SAML Client Adapter using HTTP POST Binding With Signatures

## Introduction

Basic example that demonstrates how to setup an application as a SAML v2.0 Service Provider using SAML HTTP POST Binding with Signature Support.  This example uses the Keycloak Servlet Filter to accomplish this
Note that no role checks are done with the servlet filter.  You would have to do this manually.

## Import the test realm

If you haven't already done so, you need to import the test realm for this examples.  Clicking on the below link will bring you to the
create realm page in the Admin UI.  The username/password is admin/admin to login in.  Keycloak will ask you to
create a new admin password before you can go to the create realm page.

[http://localhost:8080/auth/admin/master/console/#/create/realm](http://localhost:8080/auth/admin/master/console/#/create/realm)

Import the testsaml.json file that is in the saml/ example directory.

## Build and Deploy

```
$ mvn clean install wildfly:deploy
```

## Access the application

The application will be running at the following URL: <http://localhost:8080/saml-servlet-filter>.  Login with:

    username: bburke
    password: password


