package org.keycloak.admin.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.token.TokenManager;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class Keycloak {

    private final Config config;
    private final TokenManager tokenManager;
    private final ResteasyWebTarget target;
    private final ResteasyClient client;

    private Keycloak(String serverUrl, String realm, String username, String password, String clientId, String clientSecret){
        config = new Config(serverUrl, realm, username, password, clientId, clientSecret);
        tokenManager = new TokenManager(config);

        client = new ResteasyClientBuilder().build();
        target = client.target(config.getServerUrl());

        target.register(new BearerAuthFilter(tokenManager.getAccessTokenString()));
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret){
        return new Keycloak(serverUrl, realm, username, password, clientId, clientSecret);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId){
        return new Keycloak(serverUrl, realm, username, password, clientId, null);
    }

    public RealmsResource realms(){
        return target.proxy(RealmsResource.class);
    }

    public RealmResource realm(String realmName){
        return realms().realm(realmName);
    }

    public TokenManager tokenManager(){
        return tokenManager;
    }

    public void close() {
        client.close();
    }

}
