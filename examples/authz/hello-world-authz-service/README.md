# About the Example Application

This is a simple application to get you started with Keycloak Authorization Services.

It provides a single page application which is protected by a policy enforcer that decides whether an user can access
that page or not based on the permissions obtained from a Keycloak Server.

## Create the Example Realm and a Resource Server

Considering that your Keycloak Server is up and running, log in to the Keycloak Administration Console.

Now, create a new realm based on the following configuration file:

    examples/authz/hello-world-authz-service/hello-world-authz-realm.json
    
That will import a pre-configured realm with everything you need to run this example. For more details about how to import a realm 
into Keycloak, check the Keycloak's reference documentation.

After importing that file, you'll have a new realm called ``hello-world-authz``. 

Now, let's import another configuration using the Administration Console in order to configure the client application ``hello-world-authz-service`` as a resource server with all resources, scopes, permissions and policies.

Click on ``Clients`` on the left side menu. Click on the ``hello-world-authz-service`` on the client listing page. This will
open the ``Client Details`` page. Once there, click on the `Authorization` tab. 

Click on the ``Select file`` button, which means you want to import a resource server configuration. Now select the file that is located at:

    examples/authz/hello-world-authz-service/hello-world-authz-service.json
    
Now click ``Upload`` and the resource server will be updated accordingly.

## Deploy and Run the Example Application

To deploy the example application, follow these steps:

    cd examples/authz/hello-world-authz-service
    mvn clean package wildfly:deploy
    
Now, try to access the client application using the following URL:

    http://localhost:8080/hello-world-authz-service

If everything is correct, you will be redirect to Keycloak login page. You can login to the application with the following credentials:

* username: jdoe / password: jdoe
* username: alice / password: alice

