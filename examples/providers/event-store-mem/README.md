Example Event Store that stores events in memory
================================================

To deploy copy target/event-store-mem-example.jar to standalone/deployments/auth-server.war/WEB-INF/lib. Then edit standalone/configuration/keycloak-server.json, change:

   "eventsStore": {
     "provider": "jpa"
   }

to:

   "eventsStore": {
     "provider": "in-mem"
   }

Then start (or restart)the server. Once started open the admin console, select your realm, then click on Events, followed by config. Set the toggle for Enabled to ON. After this try to logout and login again then open the Events tab again in the admin console to view events from the in-mem provider.
