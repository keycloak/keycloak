# About the Example Application

This is a simple Servlet-based application that will introduce you to some of the main concepts around Keycloak Authorization Services.

For this application, users can be regular users, premium users or administrators, where:

* Regular users have very limited access.
* Premium users have access to the *premium area*
* Administrators have access to the *administration area*

In Keycloak, all the paths being protected are resources on the server.

This application will also show you how to create a dynamic menu with the permissions granted to an user.

## Create the Example Realm and a Resource Server

Considering that your Keycloak Server is up and running, log in to the Keycloak Administration Console.

Now, create a new realm based on the following configuration file:

    examples/authz/servlet-authz/servlet-authz-realm.json
    
That will import a pre-configured realm with everything you need to run this example. For more details about how to import a realm 
into Keycloak, check the Keycloak's reference documentation.

After importing that file, you'll have a new realm called ``servlet-authz``. 

Now, let's import another configuration using the Administration Console in order to configure the client application ``servlet-authz-app`` as a resource server with all resources, scopes, permissions and policies.

Click on ``Clients`` on the left side menu. Click on the ``servlet-authz-app`` on the client listing page. This will
open the ``Client Details`` page. Once there, click on the `Authorization` tab. 

Click on the ``Select file`` button, which means you want to import a resource server configuration. Now select the file that is located at:

    examples/authz/servlet-authz/servlet-authz-app-config.json
    
Now click ``Upload`` and the resource server will be updated accordingly.

## Deploy and Run the Example Applications

To deploy the example application, follow these steps:

    cd examples/authz/servlet-authz
    mvn clean package wildfly:deploy
    
Now, try to access the client application using the following URL:

    http://localhost:8080/servlet-authz-app

If everything is correct, you will be redirect to Keycloak login page. You can login to the application with the following credentials:

* username: jdoe / password: jdoe
* username: alice / password: alice
* username: admin / password: admin