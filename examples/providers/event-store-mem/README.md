Example Event Store that stores events in memory
================================================

To deploy copy target/event-store-mem-example.jar to providers directory. Alternatively you can deploy as a module by running:

    KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.examples.event-inmem --resources=target/event-store-mem-example.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-server-spi"

Then registering the provider by editing keycloak-server.json and adding the module to the providers field:

    "providers": [
        ....
        "module:org.keycloak.examples.event-inmem"
    ],

Then edit standalone/configuration/keycloak-server.json, change:

   "eventsStore": {
     "provider": "jpa"
   }

to:

   "eventsStore": {
     "provider": "in-mem"
   }

Then start (or restart)the server. Once started open the admin console, select your realm, then click on Events, 
followed by config. Set the toggle for Enabled to ON. After this try to logout and login again then open the Events tab 
again in the admin console to view events from the in-mem provider.
