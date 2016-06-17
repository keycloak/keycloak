# About the Example Application

This is a simple Servlet-based application that will introduce you to some of the main concepts around Keycloak Authorization Services.

For this application, users can be regular users, premium users or administrators, where:

* Regular users have very limited access.
* Premium users have access to the *premium area*
* Administrators have access to the *administration area*

In Keycloak, all the paths being protected are resources on the server.

This application will also show you how to create a dynamic menu with the permissions granted to an user.

## Create the Example Realm and a Resource Server

Considering that your AuthZ Server is up and running, log in to the Keycloak Administration Console.

Now, create a new realm based on the following configuration file:

    examples/authz/servlet-authz/servlet-authz-realm.json
    
That will import a pre-configured realm with everything you need to run this example. For more details about how to import a realm 
into Keycloak, check the Keycloak's reference documentation.

After importing that file, you'll have a new realm called ``servlet-authz``. 

Now, let's import another configuration using the Administration Console in order to configure the ``servlet-authz-app`` client application as a resource server with all resources, scopes, permissions and policies.

Click on ``Authorization`` on the left side menu. Click on the ``Create`` button on the top of the resource server table. This will
open the page that allows you to create a new resource server.

Click on the ``Select file`` button, which means you want to import a resource server configuration. Now select the file that is located at:

    examples/authz/servlet-authz/servlet-authz-app-config.json
    
Now click ``Upload`` and a new resource server will be created based on the ``servlet-authz-app`` client application.

## Deploy and Run the Example Applications

To deploy the example applications, follow these steps:

    cd examples/authz/servlet-authz
    mvn wildfly:deploy
    
If everything is correct, you will be redirect to Keycloak login page. You can login to the application with the following credentials:

* username: jdoe / password: jdoe (premium user)
* username: alice / password: alice (regular user)
* username: admin / password: admin (administrator)