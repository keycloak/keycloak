package org.keycloak.services.resources.flows;

import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmModel;

public class Flows {

    private Flows() {
    }

    public static PageFlows pages(HttpRequest request) {
        return new PageFlows(request);
    }

    public static FormFlows forms(RealmModel realm, HttpRequest request) {
        return new FormFlows(realm, request);
    }

    public static OAuthFlows oauth(RealmModel realm, HttpRequest request, UriInfo uriInfo, AuthenticationManager authManager,
            TokenManager tokenManager) {
        return new OAuthFlows(realm, request, uriInfo, authManager, tokenManager);
    }

}
