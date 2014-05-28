Example Authentication Provider based on property file values
=============================================================

* To deploy copy "target/authentication-properties-example.jar" to "standalone/deployments/auth-server.war/WEB-INF/lib" . Then edit "standalone/configuration/keycloak-server.json" and add this:
````shell
    "authentication": {
        "properties": {
            "propertiesFileLocation": "users.properties"
        }
    }
````

* Then start (or restart)the server. Once started open the admin console, select your realm, then click on "Authentication" and then "Add provider" and select "properties" from the list.
This will mean that realm will use PropertiesAuthenticationProvider for authentication.

* Once you try to login to realm, you can login with username/password like "joe/password1" or "james/password2" . Once joe is authenticated,
you can see in Keycloak admin console in "Users" list that user "joe" was added to the list.

* You can try to login as joe and then go to [http://localhost:8080/auth/realms/keycloak-admin/account/password](http://localhost:8080/auth/realms/keycloak-admin/account/password) and change the password.
You will then be able to logout and login with new password because properties were updated. But this is just in memory-properties, so after server restart the password will be again "password1" .
