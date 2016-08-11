Example User Storage Provider with EJB and JPA
===================================================

This is an example of the User Storage SPI implemented using EJB and JPA.  To deploy this provider you must have Keycloak
running in standalone or standalone-ha mode.  Then type the follow maven command:

    mvn clean install wildfly:deploy

Login and go to the User Federation tab and you should now see your deployed provider in the add-provider list box.
Add the provider, save it, then any new user you create will be stored and in the custom store you implemented.  You
can modify the example and hot deploy it using the above maven command again.

This example uses the built in in-memory datasource that comes with keycloak: ExampleDS.
