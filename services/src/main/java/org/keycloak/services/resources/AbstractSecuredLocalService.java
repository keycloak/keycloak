/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.AbstractOAuthClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Set;

/**
 * Helper class for securing local services.  Provides login basics as well as CSRF check basics
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractSecuredLocalService {
    private static final Logger logger = Logger.getLogger(AbstractSecuredLocalService.class);

    private static final String KEYCLOAK_STATE_CHECKER = "KEYCLOAK_STATE_CHECKER";

    protected final ClientModel client;
    protected RealmModel realm;

    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;
    @Context
    protected ClientConnection clientConnection;
    protected String stateChecker;
    @Context
    protected KeycloakSession session;
    @Context
    protected HttpRequest request;
    protected Auth auth;

    public AbstractSecuredLocalService(RealmModel realm, ClientModel client) {
        this.realm = realm;
        this.client = client;
    }

    @Path("login-redirect")
    @GET
    public Response loginRedirect(@QueryParam("code") String code,
                                  @QueryParam("state") String state,
                                  @QueryParam("error") String error,
                                  @QueryParam("path") String path,
                                  @QueryParam("referrer") String referrer,
                                  @Context HttpHeaders headers) {
        try {
            if (error != null) {
                logger.debug("error from oauth");
                throw new ForbiddenException("error");
            }
            if (path != null && !getValidPaths().contains(path)) {
                throw new BadRequestException("Invalid path");
            }
            if (!realm.isEnabled()) {
                logger.debug("realm not enabled");
                throw new ForbiddenException();
            }
            if (!client.isEnabled()) {
                logger.debug("account management app not enabled");
                throw new ForbiddenException();
            }
            if (code == null) {
                logger.debug("code not specified");
                throw new BadRequestException("code not specified");
            }
            if (state == null) {
                logger.debug("state not specified");
                throw new BadRequestException("state not specified");
            }

            KeycloakUriBuilder redirect = KeycloakUriBuilder.fromUri(getBaseRedirectUri());
            if (path != null) {
                redirect.path(path);
            }
            if (referrer != null) {
                redirect.queryParam("referrer", referrer);
            }

            return Response.status(302).location(redirect.build()).build();
        } finally {
        }
    }

    protected void updateCsrfChecks() {
        Cookie cookie = headers.getCookies().get(KEYCLOAK_STATE_CHECKER);
        if (cookie != null) {
            stateChecker = cookie.getValue();
        } else {
            stateChecker = KeycloakModelUtils.generateSecret();
            String cookiePath = AuthenticationManager.getRealmCookiePath(realm, uriInfo);
            boolean secureOnly = realm.getSslRequired().isRequired(clientConnection);
            CookieHelper.addCookie(KEYCLOAK_STATE_CHECKER, stateChecker, cookiePath, null, null, -1, secureOnly, true);
        }
    }

    protected abstract Set<String> getValidPaths();

    /**
     * Check to see if form post has sessionId hidden field and match it against the session id.
     *
     * @param formData
     */
    protected void csrfCheck(final MultivaluedMap<String, String> formData) {
        if (!auth.isCookieAuthenticated()) return;
        String stateChecker = formData.getFirst("stateChecker");
        if (!this.stateChecker.equals(stateChecker)) {
            throw new ForbiddenException();
        }

    }

    /**
     * Check to see if form post has sessionId hidden field and match it against the session id.
     *
     */
    protected void csrfCheck(String stateChecker) {
        if (!auth.isCookieAuthenticated()) return;
        if (auth.getSession() == null) return;
        if (!this.stateChecker.equals(stateChecker)) {
            throw new ForbiddenException();
        }

    }

    protected abstract URI getBaseRedirectUri();

    protected Response login(String path) {
        OAuthRedirect oauth = new OAuthRedirect();
        String authUrl = OIDCLoginProtocolService.authUrl(uriInfo).build(realm.getName()).toString();
        oauth.setAuthUrl(authUrl);

        oauth.setClientId(client.getClientId());

        oauth.setSecure(realm.getSslRequired().isRequired(clientConnection));

        UriBuilder uriBuilder = UriBuilder.fromUri(getBaseRedirectUri()).path("login-redirect");

        if (path != null) {
            uriBuilder.queryParam("path", path);
        }

        String referrer = uriInfo.getQueryParameters().getFirst("referrer");
        if (referrer != null) {
            uriBuilder.queryParam("referrer", referrer);
        }

        String referrerUri = uriInfo.getQueryParameters().getFirst("referrer_uri");
        if (referrerUri != null) {
            uriBuilder.queryParam("referrer_uri", referrerUri);
        }

        URI accountUri = uriBuilder.build(realm.getName());

        oauth.setStateCookiePath(accountUri.getRawPath());
        return oauth.redirect(uriInfo, accountUri.toString());
    }

    static class OAuthRedirect extends AbstractOAuthClient {

        /**
         * closes client
         */
        public void stop() {
        }

        public Response redirect(UriInfo uriInfo, String redirectUri) {
            String state = getStateCode();
            String scopeParam = TokenUtil.attachOIDCScope(scope);

            UriBuilder uriBuilder = UriBuilder.fromUri(authUrl)
                    .queryParam(OAuth2Constants.CLIENT_ID, clientId)
                    .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                    .queryParam(OAuth2Constants.STATE, state)
                    .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                    .queryParam(OAuth2Constants.SCOPE, scopeParam);

            URI url = uriBuilder.build();

            NewCookie cookie = new NewCookie(getStateCookieName(), state, getStateCookiePath(uriInfo), null, null, -1, isSecure, true);
            logger.debug("NewCookie: " + cookie.toString());
            logger.debug("Oauth Redirect to: " + url);
            return Response.status(302)
                    .location(url)
                    .cookie(cookie).build();
        }

        private String getStateCookiePath(UriInfo uriInfo) {
            if (stateCookiePath != null) return stateCookiePath;
            return uriInfo.getBaseUri().getRawPath();
        }

    }


}
