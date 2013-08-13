package org.keycloak.services.resources;

import java.net.URI;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.JspRequestParameters;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;

public class OAuthUtil {

    private static final Logger log = Logger.getLogger(OAuthUtil.class);

    public final static String securityFailurePath = "/saas/securityFailure.jsp";
    public final static String loginFormPath = "/sdk/login.xhtml";
    public final static String oauthFormPath = "/saas/oauthGrantForm.jsp";

    public static Response processAccessCode(RealmModel realm, TokenManager tokenManager, AuthenticationManager authManager,
            HttpRequest request, UriInfo uriInfo,
            String scopeParam, String state,
            String redirect,
            UserModel client, UserModel user) {
        RoleModel resourceRole = realm.getRole(RealmManager.RESOURCE_ROLE);
        RoleModel identityRequestRole = realm.getRole(RealmManager.IDENTITY_REQUESTER_ROLE);
        boolean isResource = realm.hasRole(client, resourceRole);
        if (!isResource && !realm.hasRole(client, identityRequestRole)) {
            securityFailureForward(request, "Login requester not allowed to request login.");
            return null;
        }
        AccessCodeEntry accessCode = tokenManager.createAccessCode(scopeParam, state, redirect, realm, client, user);
        log.info("processAccessCode: isResource: " + isResource);
        log.info(
                "processAccessCode: go to oauth page?: "
                        + (!isResource && (accessCode.getRealmRolesRequested().size() > 0 || accessCode
                                .getResourceRolesRequested().size() > 0)));
        if (!isResource
                && (accessCode.getRealmRolesRequested().size() > 0 || accessCode.getResourceRolesRequested().size() > 0)) {
            oauthGrantPage(realm, request, uriInfo, accessCode, client);
            return null;
        }
        return redirectAccessCode(realm, authManager, uriInfo, accessCode, state, redirect);
    }

    public static void securityFailureForward(HttpRequest request, String message) {
        log.error(message);
        request.setAttribute(JspRequestParameters.KEYCLOAK_SECURITY_FAILURE_MESSAGE, message);
        request.forward(securityFailurePath);
    }

    public static Response redirectAccessCode(RealmModel realm, AuthenticationManager authManager, UriInfo uriInfo,
            AccessCodeEntry accessCode,
            String state, String redirect) {
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

    public static void forwardToLoginForm(RealmModel realm, HttpRequest request, UriInfo uriInfo, String redirect,
            String clientId, String scopeParam, String state) {
        request.setAttribute(RealmModel.class.getName(), realm);
        request.setAttribute("KEYCLOAK_LOGIN_ACTION", TokenService.processLoginUrl(uriInfo).build(realm.getId()));
        request.setAttribute("KEYCLOAK_SOCIAL_LOGIN", SocialResource.redirectToProviderAuthUrl(uriInfo).build(realm.getId()));
        request.setAttribute("KEYCLOAK_REGISTRATION_PAGE", URI.create("not-implemented-yet"));

        // RESTEASY eats the form data, so we send via an attribute
        request.setAttribute("redirect_uri", redirect);
        request.setAttribute("client_id", clientId);
        request.setAttribute("scope", scopeParam);
        request.setAttribute("state", state);
        request.forward(loginFormPath);
    }

    public static void oauthGrantPage(RealmModel realm, HttpRequest request, UriInfo uriInfo, AccessCodeEntry accessCode,
            UserModel client) {
        request.setAttribute("realmRolesRequested", accessCode.getRealmRolesRequested());
        request.setAttribute("resourceRolesRequested", accessCode.getResourceRolesRequested());
        request.setAttribute("client", client);
        request.setAttribute("action", TokenService.processOAuthUrl(uriInfo).build(realm.getId()).toString());
        request.setAttribute("code", accessCode.getCode());

        request.forward(oauthFormPath);
    }

}
