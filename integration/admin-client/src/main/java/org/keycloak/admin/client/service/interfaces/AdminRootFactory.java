package org.keycloak.admin.client.service.interfaces;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.token.TokenManager;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class AdminRootFactory {

    private AdminRootFactory(){}

    public static AdminRoot getAdminRoot(Config config, TokenManager tokenManager){
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(config.getServerUrl());

        target.register(new BearerAuthFilter(tokenManager.getAccessTokenString()));

        return target.proxy(AdminRoot.class);
    }

}
