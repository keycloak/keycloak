Keycloak Example - Multi Tenancy
================================

The following example was tested on Wildfly 8.1.0.Final and should be compatible with any JBoss AS, JBoss EAP or Wildfly that supports Java EE 7.

This example demonstrates the Keycloak Multi Tenancy support. Multi Tenancy is understood on this context as a single application (WAR) that is deployed on a single or clustered application server, authenticating users from *different realms* against a single or clustered Keycloak server.

The multi tenancy is achieved by having one realm per tenant on the server side and a per-request decision on which realm to authenticate the request against.

This example simulates an application that collects metrics from nodes (machines or virtual machines, for instance) and sends the data to a metrics backend service. Users interested on this service would register an account for his company or department via the "Registration" module and would update the collector agent with the credentials for his account.

This example is composed of the following modules:

- business - The main business module, the target of our application.
  - backend - A JAX-RS service that is protected by Keycloak and that would store the metrics data.
  - collector - The OAuth client that would run as an agent on the nodes.
- registration - A sample module that shows how a registration procedure could be.
  - backend - A JAX-RS service that creates new realms on the Keycloak server based on data from the frontend.
  - frontend - Collects the data from the user and sends to the backend. Note that this is a simple HTML5 application and is not a Maven module.

Please, refer to each module's README files for specific details.


Step 1: Setup a basic Keycloak server
--------------------------------------------------------------
Install Keycloak server and start it on port 8180. Please, refer to the [Reference Guide](http://docs.jboss.org/keycloak/docs/1.0.1.Final/userguide/html_single/index.html) on how to setup a Keycloak server. You can use the _appliance_ distribution or, if you have docker installed, run ``docker run -p 8180:8080 jboss/keycloak`` for a quick start. For the appliance distribution, you might need to pass the following option to start it on port 8180: ``-Djboss.socket.binding.port-offset=100``

Once the Keycloak server is up and running, do the following changes:

1. Create an user named ``registration``, password ``registration``
1. Create an OAuth client named ``registration`` with the ``Direct Grants Only`` enabled.
1. Update the ``KEYCLOAK_OAUTH_SECRET`` constant on ``RegistrationService`` from ``registration/backend`` with the OAuth Secret for the ``registration`` OAuth client.
1. Change the ``master`` realm to enable the ``Direct Grants API``(Settings -> Login)
1. Add the ``create-realm`` role to the ``registration`` user (Users -> view all -> registration -> Role Mappings)
1. Enable "Full Scope Allowed" on the "Scope" properties for the ``registration`` OAuth client (OAuth clients -> registration -> Scope). This effectively means that the OAuth Client has permission to use all the roles available for the user it's representing.

Those changes are required by the Registration module, which creates realms on demand on the Keycloak server. 

Step 2: Deploy the Registration module and register an account
--------------------------------------------------------------

1. Install ``grunt``, required to run the frontend part of the registration module (``yum install -y nodejs-grunt-cli`` on Fedora 20)
1. Build and copy ``registration/backend/target/multitenant-registration-backend.war`` into
the ``standalone/deployments`` of your Wildfly/JBoss EAP/JBoss AS. For this part of the example, the application server doesn't need to have the Keycloak subsystem nor adapter.
1. Run ``grunt serve`` on ``registration/frontend``. At the end of the process, it should open a browser window with the HTML5 application, showing a registration form.
1. Register a new client. After registering, you should see a set of credentials on the screen, including:
	- OAuth Secret (for the collector, not to be mistaken with the OAuth Secret of the ``registration`` application)
	- Node username
	- Node password
	- keycloak.json

Step 3: Deploy the business backend
--------------------------------------------------------------

1. Change the Runner.java file from the ``business/oauthclient``, to use the credentials you obtained from the last part of Step 2:
	1. ``REALM_NAME`` is the "account" name from the registration form
	2. ``KEYCLOAK_NODE_USERNAME`` is the node's username, resembling a UUID.
	3. ``KEYCLOAK_NODE_PASSWORD`` is the node's password
	4. ``KEYCLOAK_OAUTH_CLIENT_SECRET`` is the OAuth Secret for the collector (not to OAuth Secret for the ``registration`` application).
	5. Save the keycloak.json on ``business/backend/src/main/resources/keycloak-REALM_NAME.json``, where REALM_NAME matches the realm name in Keycloak (property "realm" in the JSON itself)
1. Build and copy ``business/backend/target/multitenant-business-backend.war`` into the ``standalone/deployments`` of your Wildfly/JBoss EAP/JBoss AS. For this part of the example, the application server *needs* to be properly configured to use Keycloak's adapter. Please, consult the [Reference Guide](http://docs.jboss.org/keycloak/docs/1.0.1.Final/userguide/html_single/index.html) on how to achieve this.
1. Run the ``Runner`` Java class. If you see "Success" as the output, it has worked.

Step 4: Check the individual module's README files
--------------------------------------------------------------

Now that you checked that it works, it's a good idea to browse the code and see how it all fits together. It's also adviseable to check your Keycloak server. If you did a registration, you should see a new realm there.

Here's a brief description of the flow when ``Runner`` executes:

- The ``Runner`` contacts the Keycloak server as the OAuth client ``metrics-collector``, in the name of the ``KEYCLOAK_NODE_USERNAME`` user and gets an Access Token.
- A request is then made to the backend, sending the Access Token as the Bearer Token. Additionally, this request contains an arbitraty HTTP Header named ``X-Keycloak-Realm``, with the value ``REALM_NAME``.
- The backend instructs Keycloak to defer the realm resolution to the class ``HttpHeaderRealmKeycloakConfigResolver``, via the context parameter ``keycloak.config.resolver``, defined on the Business backend's ``web.xml``. It means that each time a request is received, this resolver is queried and should return a ``KeycloakDeployment``, representing the realm that the request should be authenticated against.
- Keycloak then authenticates the request's Bearer token agains the realm returned by the resolver, allowing it to proceed if everything looks right.
- The business component is then invoked, and all features that you'd expect from Keycloak are available, such as information about the principal and realm.


