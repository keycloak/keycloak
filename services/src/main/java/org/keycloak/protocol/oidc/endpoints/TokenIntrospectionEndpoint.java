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
package org.keycloak.protocol.oidc.endpoints;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProviderFactory;
import org.keycloak.protocol.oidc.TokenIntrospectionProvider;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * A token introspection endpoint based on RFC-7662.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TokenIntrospectionEndpoint {

    private static final String PARAM_TOKEN_TYPE_HINT = "token_type_hint";
    private static final String PARAM_TOKEN = "token";

    @Context
    private KeycloakSession session;
    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private ClientConnection clientConnection;

    private final RealmModel realm;
    private final EventBuilder event;

    public TokenIntrospectionEndpoint(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    @POST
    @NoCache
    public Response introspect() {
        event.event(EventType.INTROSPECT_TOKEN);

        checkSsl();
        checkRealm();
        authorizeClient();

        MultivaluedMap<String, String> formParams = request.getDecodedFormParameters();
        String tokenTypeHint = formParams.getFirst(PARAM_TOKEN_TYPE_HINT);

        if (tokenTypeHint == null) {
            tokenTypeHint = AccessTokenIntrospectionProviderFactory.ACCESS_TOKEN_TYPE;
        }

        String token = formParams.getFirst(PARAM_TOKEN);

        if (token == null) {
            throw throwErrorResponseException(Errors.INVALID_REQUEST, "Token not provided.", Status.BAD_REQUEST);
        }

        TokenIntrospectionProvider provider = this.session.getProvider(TokenIntrospectionProvider.class, tokenTypeHint);

        if (provider == null) {
            throw throwErrorResponseException(Errors.INVALID_REQUEST, "Unsupported token type [" + tokenTypeHint + "].", Status.BAD_REQUEST);
        }

        try {

            Response response = provider.introspect(token);

            this.event.success();

            return response;
        } catch (ErrorResponseException ere) {
            throw ere;
        } catch (Exception e) {
            throw throwErrorResponseException(Errors.INVALID_REQUEST, "Failed to introspect token.", Status.BAD_REQUEST);
        }
    }

    private void authorizeClient() {
        try {
            ClientModel client = AuthorizeClientUtil.authorizeClient(session, event).getClient();

            this.event.client(client);

            if (client == null || client.isPublicClient()) {
                throw throwErrorResponseException(Errors.INVALID_REQUEST, "Client not allowed.", Status.FORBIDDEN);
            }

        } catch (ErrorResponseException ere) {
            throw ere;
        } catch (Exception e) {
            throw throwErrorResponseException(Errors.INVALID_REQUEST, "Authentication failed.", Status.UNAUTHORIZED);
        }
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException("invalid_request", "HTTPS required", Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new ErrorResponseException("access_denied", "Realm not enabled", Status.FORBIDDEN);
        }
    }

    private ErrorResponseException throwErrorResponseException(String error, String detail, Status status) {
        this.event.detail("detail", detail).error(error);
        return new ErrorResponseException(error, detail, status);
    }
}