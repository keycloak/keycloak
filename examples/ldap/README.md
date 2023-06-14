Keycloak Example - LDAP
=======================

This example shows how to configure Keycloak with LDAP and use LDAP for authenticating users into Keycloak and provision data about users.

Example is using built-in ApacheDS LDAP server with preconfigured LDIF file with some example LDAP data (you can override with your own LDIF file) 
and preconfigured realm JSON file with LDAP Federation provider and sample set of preconfigured LDAP mappers and protocol mappers.
This shows mapping of basic LDAP data (username, firstName, lastName, email), but also some custom attributes ( postal code, street) and also
propagation of role mappings from LDAP to the Keycloak. LDAP mappers are used to import data from LDAP user records into user records in Keycloak database.
Then there are protocol mappers, which are used to propagate the data from user record in Keycloak database into the IDToken and Access Token, which is then shown in the example application.

The example application is then showing all the basic claims of current user together with custom claims and role mappings.

Detailed steps how to make the example working:

**1)** Build and deploy this sample's WAR file in `target/ldap-portal.war` . For this example, deploy on the same server that is running the Keycloak Server, 
although this is not required for real world scenarios.


**2)**  Run ApacheDS based LDAP server. You can run the command like this (assuming you're in the "ldap" directory with this example): 

```
mvn exec:java -Pldap
```

This will also automatically import the LDIF from `ldap-example-users.ldif` into the LDAP server. Replace with your own LDIF file if you want different users.
 
 
**3)** Run Keycloak server and import `ldaprealm.json` into it through admin console. This contains the realm with preconfigured LDAP federation provider and LDAP mappers 
and protocol mappers. Note that there are not any roles or users in this file. All of users, roles and role mappings data will be imported automatically from LDAP. 
 
 
**4)** Test the example. In your web browser open `http://localhost:8080/ldap-portal` . You can log in either as `jbrown` with password `password` or as
`bwilson` with password `password` . You can see that access token contains all the claims and role mappings corresponding to the LDAP data provided in LDIF.
