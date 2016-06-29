# About the Example Application

This is a simple application based on HTML5+AngularJS+JAX-RS that will introduce you to some of the main concepts around Keycloak Authorization Services.

Basically, it is a project containing three modules:
 
* **photoz-restful-api**, a simple RESTFul API based on JAX-RS and acting as a resource server.
* **photoz-html5-client**, a HTML5+AngularJS client that will consume the RESTful API published by a resource resourcer.
* **photoz-authz-policy**, a simple project with some rule-based policies using JBoss Drools.

For this application, users can be regular users or administrators. Regular users can create/view/delete their albums 
and administrators can do anything.

In Keycloak, albums are resources that must be protected based on a set of policies that defines who and how can access them.

The resources are also associated with a set of scopes that defines a specific access context. In this case, albums have three main scopes:

* urn:photoz.com:scopes:album:create
* urn:photoz.com:scopes:album:view
* urn:photoz.com:scopes:album:delete

The authorization requirements for this example application are based on the following assumptions:

* By default, any regular user can perform any operation on his resources.

    * For instance, Alice can create, view and delete her albums. 

* Only the owner and administrators can delete albums. Here we are considering policies based on the *urn:photoz.com:scopes:album:delete* scope

    * For instance, only Alice can delete her album.

* Only administrators can access the Administration API (which basically provides ways to query albums for all users)

* Administrators are only authorized to access resources if the client's ip address is well known

That said, this application will show you how to use the Keycloak to define policies using:

* Role-based Access Control
* Attribute-based Access Control
* Rule-based policies using JBoss Drools
* Rule-based policies using JavaScript 

Beside that, this example demonstrates how to create resources dynamically and how to protected them using the *Protection API* and the *Authorization Client API*. Here you'll see
how to create a resource whose owner is the authenticated user.

It also provides some background on how you can actually protect your JAX-RS endpoints using a *policy enforcer*.

## Create the Example Realm and a Resource Server

Considering that your Keycloak Server is up and running, log in to the Keycloak Administration Console.

Now, create a new realm based on the following configuration file:

    examples/authz/photoz/photoz-realm.json
    
That will import a pre-configured realm with everything you need to run this example. For more details about how to import a realm 
into Keycloak, check the Keycloak's reference documentation.

After importing that file, you'll have a new realm called ``photoz``. 

Back to the command-line, build the example application. This step is necessary given that we're using policies based on
JBoss Drools, which require ``photoz-authz-policy`` artifact installed into your local maven repository.

    cd examples/authz/photoz
    mvn clean install 

Now, let's import another configuration using the Administration Console in order to configure the client application ``photoz-restful-api`` as a resource server with all resources, scopes, permissions and policies.

Click on ``Clients`` on the left side menu. Click on the ``photoz-restful-api`` on the client listing page. This will
open the ``Client Details`` page. Once there, click on the `Authorization` tab. 

Click on the ``Select file`` button, which means you want to import a resource server configuration. Now select the file that is located at:

    examples/authz/photoz/photoz-restful-api/photoz-restful-api-authz-config.json
    
Now click ``Upload`` and the resource server will be updated accordingly.

## Deploy and Run the Example Applications

To deploy the example applications, follow these steps:

    cd examples/authz/photoz/photoz-html5-client
    mvn clean package wildfly:deploy
    
And then:

    cd examples/authz/photoz/photoz-restful-api
    mvn clean package wildfly:deploy
   
Now, try to access the client application using the following URL:

    http://localhost:8080/photoz-html5-client

If everything is correct, you will be redirect to Keycloak login page. You can login to the application with the following credentials:

* username: jdoe / password: jdoe
* username: alice / password: alice
* username: admin / password: admin