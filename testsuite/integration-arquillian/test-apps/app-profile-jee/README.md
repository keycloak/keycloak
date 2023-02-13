You need to create a client in Keycloak. The configuration options when creating the client should be:

* Client ID: You choose
* Access Type: confidential
* Root URL: Root URL for where you're hosting the application (for example http://localhost:8080)
* Valie Redirect URIs: /app-profile-jee/*
* Base URL: /app-profile-jee/
* Admin URL: /app-profile-jee/

Then, build the WAR with Maven and install as per the Adapter configuration for your server as described in the Keycloak documentation.