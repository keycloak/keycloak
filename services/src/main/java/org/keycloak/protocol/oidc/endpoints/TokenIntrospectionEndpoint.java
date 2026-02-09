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

import java.util.List;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProviderFactory;
import org.keycloak.protocol.oidc.TokenIntrospectionProvider;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenIntrospectContext;

import org.jboss.resteasy.reactive.NoCache;

/**
 * A token introspection endpoint based on RFC-7662.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TokenIntrospectionEndpoint {

    public static final String PARAM_TOKEN_TYPE_HINT = "token_type_hint";
    public static final String PARAM_TOKEN = "token";

    private final KeycloakSession session;

    private final HttpRequest request;

    private final ClientConnection clientConnection;

    private final RealmModel realm;
    private final EventBuilder event;

    public TokenIntrospectionEndpoint(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.realm = session.getContext().getRealm();
        this.event = event;
        this.request = session.getContext().getHttpRequest();
    }

    @POST
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, org.keycloak.utils.MediaType.APPLICATION_JWT})
    public Response introspect() {
        event.event(EventType.INTROSPECT_TOKEN);

        checkSsl();
        checkRealm();
        authorizeClient();

        MultivaluedMap<String, String> formParams = request.getDecodedFormParameters();

        checkParameterDuplicated(formParams);

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
            session.clientPolicy().triggerOnEvent(new TokenIntrospectContext(formParams));
            token = formParams.getFirst(PARAM_TOKEN);
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw throwErrorResponseException(Errors.INVALID_REQUEST, cpe.getErrorDetail(), Status.BAD_REQUEST);
        }

        try {
            return provider.introspect(token, event);
        } catch (ErrorResponseException ere) {
            throw ere;
        } catch (Exception e) {
            throw throwErrorResponseException(Errors.INVALID_REQUEST, "Failed to introspect token.", Status.BAD_REQUEST);
        }
    }

    private void authorizeClient() {
        try {
            ClientModel client = AuthorizeClientUtil.authorizeClient(session, event, null).getClient();

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


    private void checkParameterDuplicated(MultivaluedMap<String, String> formParams) {
        for (List<String> strings : formParams.values()) {
            if (strings.size() != 1) {
                throw throwErrorResponseException(Errors.INVALID_REQUEST, "duplicated parameter", Status.BAD_REQUEST);
            }
        }
    }

    private ErrorResponseException throwErrorResponseException(String error, String detail, Status status) {
        this.event.detail("detail", detail).error(error);
        return new ErrorResponseException(error, detail, status);
    }
}
