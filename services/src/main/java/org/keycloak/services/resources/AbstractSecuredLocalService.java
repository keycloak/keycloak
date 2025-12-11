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

import java.net.URI;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.AbstractOAuthClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.messages.Messages;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

/**
 * Helper class for securing local services.  Provides login basics as well as CSRF check basics
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractSecuredLocalService {
    private static final Logger logger = Logger.getLogger(AbstractSecuredLocalService.class);

    protected final ClientModel client;
    protected final RealmModel realm;

    protected final HttpHeaders headers;

    protected final ClientConnection clientConnection;
    protected String stateChecker;

    protected final KeycloakSession session;

    protected final HttpRequest request;
    protected Auth auth;

    public AbstractSecuredLocalService(KeycloakSession session, ClientModel client) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.clientConnection = session.getContext().getConnection();
        this.client = client;
        this.request = session.getContext().getHttpRequest();
        this.headers = session.getContext().getRequestHeaders();
    }

    @Path("login-redirect")
    @GET
    public Response loginRedirect(@QueryParam("code") String code,
                                  @QueryParam("state") String state,
                                  @QueryParam("error") String error,
                                  @QueryParam("path") String path,
                                  @QueryParam("referrer") String referrer) {
        try {
            if (error != null) {
                if (OAuthErrorException.ACCESS_DENIED.equals(error)) {
                    // cased by CANCELLED_BY_USER or CONSENT_DENIED
                    session.getContext().setClient(client);
                    return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
                } else {
                    logger.debug("error from oauth");
                    throw new ForbiddenException("error");
                }
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

    protected abstract Set<String> getValidPaths();

    protected abstract URI getBaseRedirectUri();

    protected Response login(String path) {
        OAuthRedirect oauth = new OAuthRedirect();
        String authUrl = OIDCLoginProtocolService.authUrl(session.getContext().getUri()).build(realm.getName()).toString();
        oauth.setAuthUrl(authUrl);

        oauth.setClientId(client.getClientId());

        oauth.setSecure(realm.getSslRequired().isRequired(clientConnection));

        UriBuilder uriBuilder = UriBuilder.fromUri(getBaseRedirectUri()).path("login-redirect");

        if (path != null) {
            uriBuilder.queryParam("path", path);
        }

        String referrer = session.getContext().getUri().getQueryParameters().getFirst("referrer");
        if (referrer != null) {
            uriBuilder.queryParam("referrer", referrer);
        }

        String referrerUri = session.getContext().getUri().getQueryParameters().getFirst("referrer_uri");
        if (referrerUri != null) {
            uriBuilder.queryParam("referrer_uri", referrerUri);
        }

        URI accountUri = uriBuilder.build(realm.getName());

        oauth.setStateCookiePath(accountUri.getRawPath());
        return oauth.redirect(session.getContext().getUri(), accountUri.toString());
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
            logger.debugf("NewCookie: %s", cookie);
            logger.debugf("Oauth Redirect to: %s", url);
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
