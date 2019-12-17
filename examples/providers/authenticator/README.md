Example Custom Authenticator
===================================================

1. First, Keycloak must be running.

2. Execute the follow.  This will build the example and deploy it

   $ mvn clean install wildfly:deploy

3. Copy the secret-question.ftl and secret-question-config.ftl files to the themes/base/login directory.

4. Login to admin console.  Hit browser refresh if you are already logged in so that the new providers show up.

5. Go to the Authentication menu item and go to the Flow tab, you will be able to view the currently
   defined flows.  You cannot modify an built in flows, so, to add the Authenticator you
   have to copy an existing flow or create your own.  Copy the "Browser" flow.

6. In your copy, click the "Actions" menu item and "Add Execution".  Pick Secret Question

7. Next you have to register the required action that you created. Click on the Required Actions tab in the Authentication menu.
   Click on the Register button and choose your new Required Action.
   Your new required action should now be displayed and enabled in the required actions list.

