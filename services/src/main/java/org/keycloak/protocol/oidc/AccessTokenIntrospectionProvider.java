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

import java.io.IOException;
import java.security.PublicKey;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.jboss.logging.Logger;
import org.keycloak.JWSTokenVerifier;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AccessTokenIntrospectionProvider implements TokenIntrospectionProvider {
    // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
    private static final Logger logger = Logger.getLogger(AccessTokenIntrospectionProvider.class);

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
            AccessToken accessToken = verifyAccessToken(token);
            ObjectNode tokenMetadata;

            if (accessToken != null) {
                tokenMetadata = JsonSerialization.createObjectNode(accessToken);
                tokenMetadata.put("client_id", accessToken.getIssuedFor());
                tokenMetadata.put("username", accessToken.getPreferredUsername());
            } else {
                tokenMetadata = JsonSerialization.createObjectNode();
            }

            tokenMetadata.put("active", accessToken != null);

            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    protected AccessToken verifyAccessToken(String token) throws OAuthErrorException, IOException {
        AccessToken accessToken;

        try {
            // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
            JWSInput jws = new JWSInput(token);

            JWSTokenVerifier verifier = JWSTokenVerifier.create(token)
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
            PublicKey publicKey = session.keys().getPublicKey(realm, jws.getHeader().getAlgorithm().getType(), jws.getHeader().getKeyId());

            if (publicKey == null) {
                return null;
            }

            accessToken = verifier.publicKey(publicKey).verify().getToken();
        } catch (VerificationException | JWSInputException e) {
            return null;
        }

        RealmModel realm = this.session.getContext().getRealm();

        return tokenManager.isTokenValid(session, realm, accessToken) ? accessToken : null;
    }

    protected AccessToken toAccessToken(String token) {
        try {
            // KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
            JWSInput jws = new JWSInput(token);

            JWSTokenVerifier verifier = JWSTokenVerifier.create(token)
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
            PublicKey publicKey = session.keys().getPublicKey(realm, jws.getHeader().getAlgorithm().getType(), jws.getHeader().getKeyId());

            verifier.publicKey(publicKey);

            return verifier.verify().getToken();
        } catch (VerificationException | JWSInputException e) {
            throw new ErrorResponseException("invalid_request", "Invalid token.", Response.Status.UNAUTHORIZED);
        }
    }

    @Override
    public void close() {

    }
}
