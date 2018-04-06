Example User Storage Provider with EJB and JPA
===================================================

This is an example of the User Storage SPI implemented using EJB and JPA.  You must first deploy the datasource it uses.
Start up the Keycloak server.  Then in the directory of this example type the following maven command:

    mvn -Padd-datasource install

You only need to execute this maven command once.  If you execute this again, then you will get an error message that the datasource
already exists.

If you open the pom.xml file you'll see that the add-datasource profile creates an XA datasource using the built
in H2 database that comes with the server.  An XA datasource is required because you cannot use two non-xa datasources
in the same transaction.  The Keycloak database is non-xa.

Another thing to note is that the xa-datasource created is in-memory only.  If you reboot the server, any users you've
added or changes you've made to users loaded by this provider will be wiped clean.

To deploy the provider, run the following maven command:

    mvn clean install wildfly:deploy

You can run as many times as you want and the provider will be redeployed.

Login and go to the User Federation tab and you should now see your deployed provider in the add-provider list box.
Add the provider, save it, then any new user you create will be stored and in the custom store you implemented.  You
can modify the example and hot deploy it using the above maven command again.

