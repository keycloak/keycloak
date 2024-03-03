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
package org.keycloak.services.resources.account;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.jboss.logging.Logger;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.common.enums.AccountRestApiVersion;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProvider;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProviderFactory;
import org.keycloak.protocol.oidc.TokenIntrospectionProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.AccountResourceProvider;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.theme.Theme;

import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ACCOUNT, value = "")
public class AccountLoader {

    private final KeycloakSession session;
    private final EventBuilder event;

    private final HttpRequest request;
    private final HttpResponse response;

    private static final Logger logger = Logger.getLogger(AccountLoader.class);

    public AccountLoader(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
        this.request = session.getContext().getHttpRequest();
        this.response = session.getContext().getHttpResponse();
    }

    @Path("/")
    public Object getAccountService() {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = getAccountManagementClient(realm);

        HttpRequest request = session.getContext().getHttpRequest();
        HttpHeaders headers = session.getContext().getRequestHeaders();
        MediaType content = headers.getMediaType();
        List<MediaType> accepts = headers.getAcceptableMediaTypes();

        Theme theme = getTheme(session);
        UriInfo uriInfo = session.getContext().getUri();

        AccountResourceProvider accountResourceProvider = getAccountResourceProvider(theme);

        if (request.getHttpMethod().equals(HttpMethod.OPTIONS)) {
            return new CorsPreflightService(request);
        } else if ((accepts.contains(MediaType.APPLICATION_JSON_TYPE) || MediaType.APPLICATION_JSON_TYPE.equals(content)) && !uriInfo.getPath().endsWith("keycloak.json")) {
            return getAccountRestService(client, null);
        } else if (accountResourceProvider != null) {
            return accountResourceProvider.getResource();
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * This needed to provide examples for Account API.
     * Here we return a specific sub resource, so OpenAPI generator able to scan it.
     */
    @Path("/account-api-cors-example")
    public CorsPreflightService getAccountApiCorsExample() {
        throw new ForbiddenException();
    }

    /**
     * This needed to provide examples for Account API.
     * Here we return a specific sub resource, so OpenAPI generator able to scan it.
     */
    @Path("/account-api-example")
    public AccountRestService getAccountApiExample() {
        throw new ForbiddenException();
    }

    @Path("{version : v\\d[0-9a-zA-Z_\\-]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getVersionedAccountRestService(final @PathParam("version") String version) {
        if (request.getHttpMethod().equals(HttpMethod.OPTIONS)) {
            return new CorsPreflightService(request);
        }
        return getAccountRestService(getAccountManagementClient(session.getContext().getRealm()), version);
    }

    /**
     * This needed to provide examples for Account API.
     * Here we return a specific sub resource, so OpenAPI generator able to scan it.
     */
    @Path("{version : v\\d[0-9a-zA-Z_\\-]*}/account-api-cors-example")
    @Produces(MediaType.APPLICATION_JSON)
    public CorsPreflightService getVersionedAccountApiCorsExample(final @PathParam("version") String version) {
        throw new ForbiddenException();
    }

    /**
     * This needed to provide examples for Account API.
     * Here we return a specific sub resource, so OpenAPI generator able to scan it.
     */
    @Path("{version : v\\d[0-9a-zA-Z_\\-]*}/account-api-example")
    @Produces(MediaType.APPLICATION_JSON)
    public AccountRestService getVersionedAccountApiExample(final @PathParam("version") String version) {
        throw new ForbiddenException();
    }

    private Theme getTheme(KeycloakSession session) {
        try {
            return session.theme().getTheme(Theme.Type.ACCOUNT);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private AccountRestService getAccountRestService(ClientModel client, String versionStr) {
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .authenticate();
        if (authResult == null) {
            throw new NotAuthorizedException("Bearer token required");
        }

        AccessToken accessToken = authResult.getToken();
        if (accessToken.getAudience() == null || accessToken.getResourceAccess(client.getClientId()) == null) {
            // transform for introspection to get the required claims
            AccessTokenIntrospectionProvider provider = (AccessTokenIntrospectionProvider) session.getProvider(TokenIntrospectionProvider.class,
                    AccessTokenIntrospectionProviderFactory.ACCESS_TOKEN_TYPE);
            accessToken = provider.transformAccessToken(accessToken);
        }

        if (!accessToken.hasAudience(client.getClientId())) {
            throw new NotAuthorizedException("Invalid audience for client " + client.getClientId());
        }

        Auth auth = new Auth(session.getContext().getRealm(), accessToken, authResult.getUser(), client, authResult.getSession(), false);

        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").auth().build(response);

        if (authResult.getUser().getServiceAccountClientLink() != null) {
            throw new NotAuthorizedException("Service accounts are not allowed to access this service");
        }

        AccountRestApiVersion version;
        if (versionStr == null) {
            version = AccountRestApiVersion.DEFAULT;
        }
        else {
            version = AccountRestApiVersion.get(versionStr);
            if (version == null) {
                throw new NotFoundException("API version not found");
            }
        }

        return new AccountRestService(session, auth, event, version);
    }

    private ClientModel getAccountManagementClient(RealmModel realm) {
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client == null || !client.isEnabled()) {
            logger.debug("account management not enabled");
            throw new NotFoundException("account management not enabled");
        }
        return client;
    }

    private AccountResourceProvider getAccountResourceProvider(Theme theme) {
        try {
            if (theme.getProperties().containsKey(Theme.ACCOUNT_RESOURCE_PROVIDER_KEY)) {
                return session.getProvider(AccountResourceProvider.class, theme.getProperties().getProperty(Theme.ACCOUNT_RESOURCE_PROVIDER_KEY));
            }
        } catch (IOException ignore) {}
        return session.getProvider(AccountResourceProvider.class);
    }
}
