Example Custom Authenticator
===================================================

1. First, Keycloak must be running. See [Getting Started](https://github.com/keycloak/keycloak#getting-started), or you
   can build distribution from [source](https://github.com/keycloak/keycloak/blob/master/docs/building.md).

2. Execute the follow.  This will build the example and deploy it

   `$ mvn clean install wildfly:deploy`

3. Copy the `secret-question.ftl` and `secret-question-config.ftl` files to the `themes/base/login` server directory.

4. Login to admin console.  Hit browser refresh if you are already logged in so that the new providers show up.

5. Go to the **Authentication** menu item and go to the **Flows** tab, you will be able to view the currently
   defined flows.  You cannot modify an built in flows, so, to add the Authenticator you
   have to copy an existing flow or create your own.  Copy the "Browser" flow.

6. In your copy, click the **Actions** menu item in **Forms** subflow and **Add Execution**.  Pick `Secret Question` and change 
   the **Requirement** choice.
   
7. Go to the **Bindings** tab in **Authentication** menu and change the default **Browser Flow** to your copy of the browser flow 
   and click `Save`.

8. Next you have to register the required action that you created. Click on the **Required Actions** tab in the **Authentication** menu.
   Click on the `Register` button and choose your new Required Action. You can also choose the `Default Action` for the Required Action
   and each new user has to set the secret answer.
   Your new required action should now be displayed and enabled in the required actions list.

