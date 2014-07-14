package org.keycloak.admin.client.resource;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.token.TokenManager;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakAdminFactory {

    private KeycloakAdminFactory(){}

    public static RealmResource getRealm(Config config, TokenManager tokenManager, String realmName){
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(config.getServerUrl());

        target.register(new BearerAuthFilter(tokenManager.getAccessTokenString()));

        RealmsResource adminRoot = target.proxy(RealmsResource.class);

        return adminRoot.realm(realmName);
    }

}
