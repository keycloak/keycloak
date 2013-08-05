package org.keycloak.services.resources;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.services.JspRequestParameters;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;

public abstract class AbstractLoginService {

    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;
    @Context
    HttpRequest request;
    @Context
    HttpResponse response;

    protected String securityFailurePath = "/saas/securityFailure.jsp";
    protected String loginFormPath = "/sdk/login.xhtml";
    protected String oauthFormPath = "/saas/oauthGrantForm.jsp";

    protected RealmModel realm;
    protected TokenManager tokenManager;
    protected AuthenticationManager authManager = new AuthenticationManager();

    public AbstractLoginService(RealmModel realm, TokenManager tokenManager) {
        this.realm = realm;
        this.tokenManager = tokenManager;
    }

    protected Response processAccessCode(String scopeParam, String state, String redirect, UserModel client, UserModel user) {
        RoleModel resourceRole = realm.getRole(RealmManager.RESOURCE_ROLE);
        RoleModel identityRequestRole = realm.getRole(RealmManager.IDENTITY_REQUESTER_ROLE);
        boolean isResource = realm.hasRole(client, resourceRole);
        if (!isResource && !realm.hasRole(client, identityRequestRole)) {
            securityFailureForward("Login requester not allowed to request login.");
            return null;
        }
        AccessCodeEntry accessCode = tokenManager.createAccessCode(scopeParam, state, redirect, realm, client, user);
        getLogger().info("processAccessCode: isResource: " + isResource);
        getLogger().info("processAccessCode: go to oauth page?: "
                + (!isResource && (accessCode.getRealmRolesRequested().size() > 0 || accessCode.getResourceRolesRequested()
                        .size() > 0)));
        if (!isResource
                && (accessCode.getRealmRolesRequested().size() > 0 || accessCode.getResourceRolesRequested().size() > 0)) {
            oauthGrantPage(accessCode, client);
            return null;
        }
        return redirectAccessCode(accessCode, state, redirect);
    }

    protected Response redirectAccessCode(AccessCodeEntry accessCode, String state, String redirect) {
        String code = accessCode.getCode();
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam("code", code);
        getLogger().info("redirectAccessCode: state: " + state);
        if (state != null)
            redirectUri.queryParam("state", state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());
        if (realm.isCookieLoginAllowed()) {
            location.cookie(authManager.createLoginCookie(realm, accessCode.getUser(), uriInfo));
        }
        return location.build();
    }

    protected void securityFailureForward(String message) {
        getLogger().error(message);
        request.setAttribute(JspRequestParameters.KEYCLOAK_SECURITY_FAILURE_MESSAGE, message);
        request.forward(securityFailurePath);
    }

    protected void forwardToLoginForm(String redirect, String clientId, String scopeParam, String state) {
        request.setAttribute(RealmModel.class.getName(), realm);
        request.setAttribute("KEYCLOAK_LOGIN_ACTION", TokenService.processLoginUrl(uriInfo).build(realm.getId()));
        request.setAttribute("KEYCLOAK_SOCIAL_LOGIN", SocialService.redirectToProviderAuthUrl(uriInfo).build(realm.getId()));

        // RESTEASY eats the form data, so we send via an attribute
        request.setAttribute("redirect_uri", redirect);
        request.setAttribute("client_id", clientId);
        request.setAttribute("scope", scopeParam);
        request.setAttribute("state", state);
        request.forward(loginFormPath);
    }

    protected void oauthGrantPage(AccessCodeEntry accessCode, UserModel client) {
        request.setAttribute("realmRolesRequested", accessCode.getRealmRolesRequested());
        request.setAttribute("resourceRolesRequested", accessCode.getResourceRolesRequested());
        request.setAttribute("client", client);
        request.setAttribute("action", TokenService.processOAuthUrl(uriInfo).build(realm.getId()).toString());
        request.setAttribute("code", accessCode.getCode());

        request.forward(oauthFormPath);
    }

    protected abstract Logger getLogger();
}
