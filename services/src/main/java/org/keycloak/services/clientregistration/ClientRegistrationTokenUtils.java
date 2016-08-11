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

package org.keycloak.services.clientregistration;

import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationTokenUtils {

    public static final String TYPE_INITIAL_ACCESS_TOKEN = "InitialAccessToken";
    public static final String TYPE_REGISTRATION_ACCESS_TOKEN = "RegistrationAccessToken";

    public static String updateRegistrationAccessToken(KeycloakSession session, ClientModel client) {
        return updateRegistrationAccessToken(session.getContext().getRealm(), session.getContext().getUri(), client);
    }

    public static String updateRegistrationAccessToken(RealmModel realm, UriInfo uri, ClientModel client) {
        String id = KeycloakModelUtils.generateId();
        client.setRegistrationToken(id);
        String token = createToken(realm, uri, id, TYPE_REGISTRATION_ACCESS_TOKEN, 0);
        return token;
    }

    public static String createInitialAccessToken(RealmModel realm, UriInfo uri, ClientInitialAccessModel model) {
        return createToken(realm, uri, model.getId(), TYPE_INITIAL_ACCESS_TOKEN, model.getExpiration() > 0 ? model.getTimestamp() + model.getExpiration() : 0);
    }

    public static TokenVerification verifyToken(RealmModel realm, UriInfo uri, String token) {
        if (token == null) {
            return TokenVerification.error(new RuntimeException("Missing token"));
        }

        JWSInput input;
        try {
            input = new JWSInput(token);
        } catch (JWSInputException e) {
            return TokenVerification.error(new RuntimeException("Invalid token", e));
        }

        if (!RSAProvider.verify(input, realm.getPublicKey())) {
            return TokenVerification.error(new RuntimeException("Failed verify token"));
        }

        JsonWebToken jwt;
        try {
            jwt = input.readJsonContent(JsonWebToken.class);
        } catch (JWSInputException e) {
            return TokenVerification.error(new RuntimeException("Token is not JWT", e));
        }

        if (!getIssuer(realm, uri).equals(jwt.getIssuer())) {
            return TokenVerification.error(new RuntimeException("Issuer from token don't match with the realm issuer."));
        }

        if (!jwt.isActive()) {
            return TokenVerification.error(new RuntimeException("Token not active."));
        }

        if (!(TokenUtil.TOKEN_TYPE_BEARER.equals(jwt.getType()) ||
                TYPE_INITIAL_ACCESS_TOKEN.equals(jwt.getType()) ||
                TYPE_REGISTRATION_ACCESS_TOKEN.equals(jwt.getType()))) {
            return TokenVerification.error(new RuntimeException("Invalid type of token"));
        }

        return TokenVerification.success(jwt);
    }

    private static String createToken(RealmModel realm, UriInfo uri, String id, String type, int expiration) {
        JsonWebToken jwt = new JsonWebToken();

        String issuer = getIssuer(realm, uri);

        jwt.type(type);
        jwt.id(id);
        jwt.issuedAt(Time.currentTime());
        jwt.expiration(expiration);
        jwt.issuer(issuer);
        jwt.audience(issuer);

        String token = new JWSBuilder().jsonContent(jwt).rsa256(realm.getPrivateKey());
        return token;
    }

    private static String getIssuer(RealmModel realm, UriInfo uri) {
        return Urls.realmIssuer(uri.getBaseUri(), realm.getName());
    }

    protected static class TokenVerification {

        private final JsonWebToken jwt;
        private final RuntimeException error;

        public static TokenVerification success(JsonWebToken jwt) {
            return new TokenVerification(jwt, null);
        }

        public static TokenVerification error(RuntimeException error) {
            return new TokenVerification(null, error);
        }

        private TokenVerification(JsonWebToken jwt, RuntimeException error) {
            this.jwt = jwt;
            this.error = error;
        }

        public JsonWebToken getJwt() {
            return jwt;
        }

        public RuntimeException getError() {
            return error;
        }
    }

}
