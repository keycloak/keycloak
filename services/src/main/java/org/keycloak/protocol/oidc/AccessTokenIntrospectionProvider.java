/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.protocol.oidc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.RSATokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AccessTokenIntrospectionProvider implements TokenIntrospectionProvider {

    private final KeycloakSession session;
    private final TokenManager tokenManager;
    private final RealmModel realm;

    public AccessTokenIntrospectionProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tokenManager = new TokenManager();
    }

    public Response introspect(String token) {
        try {
            AccessToken toIntrospect = toAccessToken(token);
            RealmModel realm = this.session.getContext().getRealm();
            ObjectNode tokenMetadata;

            boolean active = tokenManager.isTokenValid(session, realm, toIntrospect);

            if (active) {
                tokenMetadata = JsonSerialization.createObjectNode(toIntrospect);
                tokenMetadata.put("client_id", toIntrospect.getIssuedFor());
                tokenMetadata.put("username", toIntrospect.getPreferredUsername());
            } else {
                tokenMetadata = JsonSerialization.createObjectNode();
            }

            tokenMetadata.put("active", active);

            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    protected AccessToken toAccessToken(String token) {
        try {
            return RSATokenVerifier.toAccessToken(token, realm.getPublicKey());
        } catch (VerificationException e) {
            throw new ErrorResponseException("invalid_request", "Invalid token.", Response.Status.UNAUTHORIZED);
        }
    }

    @Override
    public void close() {

    }
}
