Example Audit Provider that stores events in a List
===================================================

To deploy copy target/audit-provider-mem-example.jar to standalone/deployments/auth-server.war/WEB-INF/lib. Then edit standalone/configuration/keycloak-server.json, change:

   "audit": {
     "provider": "jpa"
   }

to:

   "audit": {
     "provider": "in-mem"
   }

Then start (or restart)the server. Once started open the admin console, select your realm, then click on Audit, followed by config. Set the toggle for Enabled to ON. After this try to logout and login again then open the Audit tab again in the admin console to view events from the in-mem provider.
