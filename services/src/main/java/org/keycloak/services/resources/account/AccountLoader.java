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

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.enums.AccountRestApiVersion;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.theme.Theme;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountLoader {

    private KeycloakSession session;
    private EventBuilder event;

    @Context
    private HttpRequest request;
    @Context
    private HttpResponse response;

    private static final Logger logger = Logger.getLogger(AccountLoader.class);

    public AccountLoader(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
    }

    @Path("/")
    public Object getAccountService() {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = getAccountManagementClient(realm);

        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        HttpHeaders headers = session.getContext().getRequestHeaders();
        MediaType content = headers.getMediaType();
        List<MediaType> accepts = headers.getAcceptableMediaTypes();

        Theme theme = getTheme(session);
        boolean deprecatedAccount = isDeprecatedFormsAccountConsole(theme);
        UriInfo uriInfo = session.getContext().getUri();

        if (request.getHttpMethod().equals(HttpMethod.OPTIONS)) {
            return new CorsPreflightService(request);
        } else if ((accepts.contains(MediaType.APPLICATION_JSON_TYPE) || MediaType.APPLICATION_JSON_TYPE.equals(content)) && !uriInfo.getPath().endsWith("keycloak.json")) {
            return getAccountRestService(client, null);
        } else {
            if (deprecatedAccount) {
                AccountFormService accountFormService = new AccountFormService(realm, client, event);
                ResteasyProviderFactory.getInstance().injectProperties(accountFormService);
                accountFormService.init();
                return accountFormService;
            } else {
                AccountConsole console = new AccountConsole(realm, client, theme);
                ResteasyProviderFactory.getInstance().injectProperties(console);
                console.init();
                return console;
            }
        }
    }

    @Path("{version : v\\d[0-9a-zA-Z_\\-]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getVersionedAccountRestService(final @PathParam("version") String version) {
        if (request.getHttpMethod().equals(HttpMethod.OPTIONS)) {
            return new CorsPreflightService(request);
        }
        return getAccountRestService(getAccountManagementClient(session.getContext().getRealm()), version);
    }

    private Theme getTheme(KeycloakSession session) {
        try {
            return session.theme().getTheme(Theme.Type.ACCOUNT);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private boolean isDeprecatedFormsAccountConsole(Theme theme) {
        try {
            return Boolean.parseBoolean(theme.getProperties().getProperty("deprecatedMode", "true"));
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private AccountRestService getAccountRestService(ClientModel client, String versionStr) {
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setAudience(client.getClientId())
                .authenticate();

        if (authResult == null) {
            throw new NotAuthorizedException("Bearer token required");
        }
        Auth auth = new Auth(session.getContext().getRealm(), authResult.getToken(), authResult.getUser(), client, authResult.getSession(), false);

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

        AccountRestService accountRestService = new AccountRestService(session, auth, client, event, version);
        ResteasyProviderFactory.getInstance().injectProperties(accountRestService);
        accountRestService.init();
        return accountRestService;
    }

    private ClientModel getAccountManagementClient(RealmModel realm) {
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client == null || !client.isEnabled()) {
            logger.debug("account management not enabled");
            throw new NotFoundException("account management not enabled");
        }
        return client;
    }

}
