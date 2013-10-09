/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.services.resources.flows;

import java.util.HashSet;
import java.util.Set;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.resources.TokenService;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
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
        RoleModel resourceRole = realm.getRole(RealmManager.APPLICATION_ROLE);
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

        Set<RequiredAction> requiredActions = user.getRequiredActions();
        if (!requiredActions.isEmpty()) {
            accessCode.setRequiredActions(new HashSet<UserModel.RequiredAction>(requiredActions));
            accessCode.setExpiration(System.currentTimeMillis() / 1000 + realm.getAccessCodeLifespanUserAction());
            return Flows.forms(realm, request, uriInfo).setAccessCode(accessCode).setUser(user)
                    .forwardToAction(user.getRequiredActions().iterator().next());
        }

        if (!isResource
                && (accessCode.getRealmRolesRequested().size() > 0 || accessCode.getResourceRolesRequested().size() > 0)) {
            accessCode.setExpiration(System.currentTimeMillis() / 1000 + realm.getAccessCodeLifespanUserAction());
            return oauthGrantPage(accessCode, client);
        }

        if (redirect != null) {
            return redirectAccessCode(accessCode, state, redirect);
        } else {
            return null;
        }
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
        return Flows.forms(realm, request, uriInfo).setError(message).forwardToErrorPage();
    }

}
