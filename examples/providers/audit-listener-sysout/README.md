Example Audit Listener that prints events to System.out
=======================================================

To deploy copy target/audit-listener-sysout-example.jar to standalone/deployments/auth-server.war/WEB-INF/lib. Then start (or restart) the server. Once started open the admin console, select your realm, then click on Audit, followed by config. Click on Audit Listeners select box, then pick sysout from the dropdown. After this try to logout and login again to see events printed to System.out.
