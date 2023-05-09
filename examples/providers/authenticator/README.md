Example Custom Authenticator
===================================================

1. Download the keycloak distribution from Releases: https://github.com/keycloak/keycloak/releases/

2. Download this example's source code, navigate to this directory, and [build this provider from source](https://github.com/keycloak/keycloak/blob/main/docs/building.md). You can do this by running the following from this directory:

   `$ mvn clean compile assembly:single`

3. Copy the built JAR file into the providers directory of the compiled keycloak distribution. For example, if the compiled keycloak distribution is located in `/opt/keycloak` then run this command:

   `$ mv ./target/example-custom-spi.jar /opt/keycloak/providers/example-custom-spi.jar`

4. Copy the `secret-question.ftl` and `secret-question-config.ftl` files to the `themes/base/login` directory. For example, if the compiled keycloak distribution is located in `/opt/keycloak` then run this command:

   `$ mv ./*.ftl /opt/keycloak/themes/base/login`

5. Build and run Keycloak

6. Login to admin console.  Hit browser refresh if you are already logged in so that the new providers show up.

7. Go to the **Authentication** menu item and the **Flows** tab, where you will be able to view the currently
   defined flows.  You cannot modify built-in flows, so to add the Authenticator you
   have to copy an existing flow or create your own.  Copy the "Browser" flow.

8. In your copy, click the **Actions** menu item in **Forms** subflow and **Add Execution**.  Pick `Secret Question` and change 
   the **Requirement** choice.
   
9. Go to the **Bindings** tab in **Authentication** menu and change the default **Browser Flow** to your copy of the browser flow 
   and click `Save`.

10. Next you have to register the required action that you created. Click on the **Required Actions** tab in the **Authentication** menu.
   Click on the `Register` button and find your new Required Action. Set "Secret Question" to "On".

