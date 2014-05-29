Example Authentication Provider based on property file values
=============================================================

* To deploy copy "target/authentication-properties-example.jar" to "standalone/deployments/auth-server.war/WEB-INF/lib" .

* Then you can configure location of property file, from which will be users and their passwords retrieved. If you omit it and won't configure, provider will use default "users.properties" file bundled inside this example.
So for custom location, edit "standalone/configuration/keycloak-server.json" and add this:
````
"authentication": {
    "properties": {
        "propertiesFileLocation": "/tmp/your-own-property-file.properties"
    }
}
````

Assumption is that your file has format like:

````
username1=password1
username2=password2
````

* Then start (or restart)the server. Once started open the admin console, select your realm, then click on "Authentication" and then "Add provider" and select "properties" from the list.
This will mean that realm will use PropertiesAuthenticationProvider for authentication.

* Once you try to login to realm, you can login with username/password like "joe/password1" or "james/password2" (In case that you use default users.properties file) . Once joe is authenticated,
you can see in Keycloak admin console in "Users" list that user "joe" was added to the list.

* You can try to login as joe and then go to [http://localhost:8080/auth/realms/demo/account/password](http://localhost:8080/auth/realms/demo/account/password) and change the password.
You will then be able to logout and login with new password because properties were updated.
WARNING: If you use default location, properties will be updated just in memory and won't survive server restart. So in this case, you will have again "joe" with password "password1" after restart.
