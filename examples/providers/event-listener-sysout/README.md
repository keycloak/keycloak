Example Event Listener that prints events to System.out
=======================================================

To deploy copy target/event-listener-sysout-example.jar to standalone/configuration/providers. 
Then start (or restart) the server. Once started open the admin console, select your realm, then click on Events, 
followed by config. Click on Listeners select box, then pick sysout from the dropdown. After this try to logout and 
login again to see events printed to System.out.

The example event listener can be configured to exclude certain events, for example to exclude REFRESH_TOKEN and
CODE_TO_TOKEN events add the following to keycloak-server.json:

    ...
    "eventsListener": {
        "sysout" {
            "exclude": [ "REFRESH_TOKEN", "CODE_TO_TOKEN" ]
        }
    }
