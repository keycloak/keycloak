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
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.UriInfo;
import java.security.PublicKey;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationTokenUtils {

    public static final String TYPE_INITIAL_ACCESS_TOKEN = "InitialAccessToken";
    public static final String TYPE_REGISTRATION_ACCESS_TOKEN = "RegistrationAccessToken";

    public static String updateTokenSignature(KeycloakSession session, ClientRegistrationAuth auth) {
        KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(session.getContext().getRealm());

        if (keys.getKid().equals(auth.getKid())) {
            return auth.getToken();
        } else {
            RegistrationAccessToken regToken = new RegistrationAccessToken();
            regToken.setRegistrationAuth(auth.getRegistrationAuth().toString().toLowerCase());

            regToken.type(auth.getJwt().getType());
            regToken.id(auth.getJwt().getId());
            regToken.issuedAt(Time.currentTime());
            regToken.expiration(0);
            regToken.issuer(auth.getJwt().getIssuer());
            regToken.audience(auth.getJwt().getIssuer());

            String token = new JWSBuilder().kid(keys.getKid()).jsonContent(regToken).rsa256(keys.getPrivateKey());
            return token;
        }
    }

    public static String updateRegistrationAccessToken(KeycloakSession session, ClientModel client, RegistrationAuth registrationAuth) {
        return updateRegistrationAccessToken(session, session.getContext().getRealm(), session.getContext().getUri(), client, registrationAuth);
    }

    public static String updateRegistrationAccessToken(KeycloakSession session, RealmModel realm, UriInfo uri, ClientModel client, RegistrationAuth registrationAuth) {
        String id = KeycloakModelUtils.generateId();
        client.setRegistrationToken(id);

        RegistrationAccessToken regToken = new RegistrationAccessToken();
        regToken.setRegistrationAuth(registrationAuth.toString().toLowerCase());

        return setupToken(regToken, session, realm, uri, id, TYPE_REGISTRATION_ACCESS_TOKEN, 0);
    }

    public static String createInitialAccessToken(KeycloakSession session, RealmModel realm, UriInfo uri, ClientInitialAccessModel model) {
        JsonWebToken initialToken = new JsonWebToken();
        return setupToken(initialToken, session, realm, uri, model.getId(), TYPE_INITIAL_ACCESS_TOKEN, model.getExpiration() > 0 ? model.getTimestamp() + model.getExpiration() : 0);
    }

    public static TokenVerification verifyToken(KeycloakSession session, RealmModel realm, UriInfo uri, String token) {
        if (token == null) {
            return TokenVerification.error(new RuntimeException("Missing token"));
        }

        JWSInput input;
        try {
            input = new JWSInput(token);
        } catch (JWSInputException e) {
            return TokenVerification.error(new RuntimeException("Invalid token", e));
        }

        String kid = input.getHeader().getKeyId();
        PublicKey publicKey = session.keys().getRsaPublicKey(realm, kid);

        if (!RSAProvider.verify(input, publicKey)) {
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

        return TokenVerification.success(kid, jwt);
    }

    private static String setupToken(JsonWebToken jwt, KeycloakSession session, RealmModel realm, UriInfo uri, String id, String type, int expiration) {
        String issuer = getIssuer(realm, uri);

        jwt.type(type);
        jwt.id(id);
        jwt.issuedAt(Time.currentTime());
        jwt.expiration(expiration);
        jwt.issuer(issuer);
        jwt.audience(issuer);

        KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);

        String token = new JWSBuilder().kid(keys.getKid()).jsonContent(jwt).rsa256(keys.getPrivateKey());
        return token;
    }

    private static String getIssuer(RealmModel realm, UriInfo uri) {
        return Urls.realmIssuer(uri.getBaseUri(), realm.getName());
    }

    protected static class TokenVerification {

        private final String kid;
        private final JsonWebToken jwt;
        private final RuntimeException error;

        public static TokenVerification success(String kid, JsonWebToken jwt) {
            return new TokenVerification(kid, jwt, null);
        }

        public static TokenVerification error(RuntimeException error) {
            return new TokenVerification(null,null, error);
        }

        private TokenVerification(String kid, JsonWebToken jwt, RuntimeException error) {
            this.kid = kid;
            this.jwt = jwt;
            this.error = error;
        }

        public String getKid() {
            return kid;
        }

        public JsonWebToken getJwt() {
            return jwt;
        }

        public RuntimeException getError() {
            return error;
        }
    }

}
