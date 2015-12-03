package org.keycloak.admin.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.admin.client.token.TokenManager;

/**
 * Provides a Keycloak client. By default, this implementation uses a {@link ResteasyClient RESTEasy client} with the
 * default {@link ResteasyClientBuilder} settings. To customize the underling client, use a {@link KeycloakBuilder} to
 * create a Keycloak client.
 *
 * @see KeycloakBuilder
 *
 * @author rodrigo.sasaki@icarros.com.br
 */
public class Keycloak {

    private final Config config;
    private final TokenManager tokenManager;
    private final ResteasyWebTarget target;
    private final ResteasyClient client;

    Keycloak(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, ResteasyClient resteasyClient){
        config = new Config(serverUrl, realm, username, password, clientId, clientSecret);
        client = resteasyClient != null ? resteasyClient : new ResteasyClientBuilder().build();

        tokenManager = new TokenManager(config, client);

        target = client.target(config.getServerUrl());

        target.register(new BearerAuthFilter(tokenManager));
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret){
        return new Keycloak(serverUrl, realm, username, password, clientId, clientSecret, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId){
        return new Keycloak(serverUrl, realm, username, password, clientId, null, null);
    }

    public RealmsResource realms(){
        return target.proxy(RealmsResource.class);
    }

    public RealmResource realm(String realmName){
        return realms().realm(realmName);
    }

    public ServerInfoResource serverInfo(){
        return target.proxy(ServerInfoResource.class);
    }

    public TokenManager tokenManager(){
        return tokenManager;
    }

    /**
     * Closes the underlying client. After calling this method, this <code>Keycloak</code> instance cannot be reused.
     */
    public void close() {
        client.close();
    }

}
