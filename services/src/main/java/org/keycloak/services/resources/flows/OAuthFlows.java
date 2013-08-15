package org.keycloak.services.resources.flows;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.TokenService;

public class OAuthFlows {

    private static final Logger log = Logger.getLogger(OAuthFlows.class);

    private RealmModel realm;

    private HttpRequest request;

    private UriInfo uriInfo;

    private AuthenticationManager authManager;

    private TokenManager tokenManager;

    OAuthFlows(RealmModel realm, HttpRequest request, UriInfo uriInfo, AuthenticationManager authManager,
            TokenManager tokenManager) {
        this.realm = realm;
        this.request = request;
        this.uriInfo = uriInfo;
        this.authManager = authManager;
        this.tokenManager = tokenManager;
    }

    public Response redirectAccessCode(AccessCodeEntry accessCode, String state, String redirect) {
        String code = accessCode.getCode();
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam("code", code);
        log.info("redirectAccessCode: state: " + state);
        if (state != null)
            redirectUri.queryParam("state", state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());
        if (realm.isCookieLoginAllowed()) {
            location.cookie(authManager.createLoginCookie(realm, accessCode.getUser(), uriInfo));
        }
        return location.build();
    }

    public Response processAccessCode(String scopeParam, String state, String redirect, UserModel client, UserModel user) {
        RoleModel resourceRole = realm.getRole(RealmManager.RESOURCE_ROLE);
        RoleModel identityRequestRole = realm.getRole(RealmManager.IDENTITY_REQUESTER_ROLE);
        boolean isResource = realm.hasRole(client, resourceRole);
        if (!isResource && !realm.hasRole(client, identityRequestRole)) {
            return forwardToSecurityFailure("Login requester not allowed to request login.");
        }
        AccessCodeEntry accessCode = tokenManager.createAccessCode(scopeParam, state, redirect, realm, client, user);
        log.info("processAccessCode: isResource: " + isResource);
        log.info("processAccessCode: go to oauth page?: "
                + (!isResource && (accessCode.getRealmRolesRequested().size() > 0 || accessCode.getResourceRolesRequested()
                        .size() > 0)));
        if (!isResource
                && (accessCode.getRealmRolesRequested().size() > 0 || accessCode.getResourceRolesRequested().size() > 0)) {
            return oauthGrantPage(accessCode, client);
        }

        return redirectAccessCode(accessCode, state, redirect);
    }

    public Response oauthGrantPage(AccessCodeEntry accessCode, UserModel client) {
        request.setAttribute("realmRolesRequested", accessCode.getRealmRolesRequested());
        request.setAttribute("resourceRolesRequested", accessCode.getResourceRolesRequested());
        request.setAttribute("client", client);
        request.setAttribute("action", TokenService.processOAuthUrl(uriInfo).build(realm.getId()).toString());
        request.setAttribute("code", accessCode.getCode());

        request.forward(Pages.OAUTH_GRANT);
        return null;
    }

    public Response forwardToSecurityFailure(String message) {
        return Flows.pages(request).forwardToSecurityFailure(message);
    }

}
