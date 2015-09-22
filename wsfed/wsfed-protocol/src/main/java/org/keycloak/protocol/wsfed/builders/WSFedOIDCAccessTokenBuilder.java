/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.wsfed.builders;

import org.keycloak.protocol.wsfed.mappers.WSFedOIDCAccessTokenMapper;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.common.util.Base64Url;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.io.UnsupportedEncodingException;
import java.util.Set;

public class WSFedOIDCAccessTokenBuilder {
    private KeycloakSession session;
    private UserSessionModel userSession;
    private ClientSessionModel clientSession;
    private ClientSessionCode accessCode;
    private RealmModel realm;
    private ClientModel client;
    private boolean x5tIncluded;

    public KeycloakSession getSession() {
        return session;
    }

    public WSFedOIDCAccessTokenBuilder setSession(KeycloakSession session) {
        this.session = session;
        return this;
    }

    public UserSessionModel getUserSession() {
        return userSession;
    }

    public WSFedOIDCAccessTokenBuilder setUserSession(UserSessionModel userSession) {
        this.userSession = userSession;
        return this;
    }

    public ClientSessionModel getClientSession() {
        return clientSession;
    }

    public WSFedOIDCAccessTokenBuilder setClientSession(ClientSessionModel clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    public ClientSessionCode getAccessCode() {
        return accessCode;
    }

    public WSFedOIDCAccessTokenBuilder setAccessCode(ClientSessionCode accessCode) {
        this.accessCode = accessCode;
        return this;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public WSFedOIDCAccessTokenBuilder setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    public ClientModel getClient() {
        return client;
    }

    public WSFedOIDCAccessTokenBuilder setClient(ClientModel client) {
        this.client = client;
        return this;
    }

    public String build() throws NoSuchAlgorithmException, CertificateEncodingException {
        TokenManager tokenManager = new TokenManager();
        UserModel user = session.users().getUserById(userSession.getUser().getId(), realm);
        AccessToken accessToken = tokenManager.createClientAccessToken(session, accessCode.getRequestedRoles(), realm, client, user, userSession, clientSession);
        accessToken = transformAccessToken(session, accessToken, realm, client, user, userSession, clientSession);
        return encodeToken(realm, accessToken);
    }

    public String encodeToken(RealmModel realm, Object token) throws NoSuchAlgorithmException, CertificateEncodingException {
        JWSBuilderExtended builder = new JWSBuilderExtended().type("JWT");

        if(isX5tIncluded()) {
            builder.x5t(realm.getCertificate());
        }

        String encodedToken = builder.jsonContent(token)
                                     .rsa256(realm.getPrivateKey());

        return encodedToken;
    }

    public boolean isX5tIncluded() {
        return x5tIncluded;
    }

    public void setX5tIncluded(boolean x5tIncluded) {
        this.x5tIncluded = x5tIncluded;
    }

    protected class JWSBuilderExtended extends JWSBuilder {
        String type;
        String contentType;
        String x5t;

        @Override
        public JWSBuilderExtended type(String type) {
            super.type(type);
            this.type = type;
            return this;
        }

        @Override
        public JWSBuilderExtended contentType(String type) {
            super.contentType(type);
            this.contentType = type;
            return this;
        }

        public JWSBuilderExtended x5t(X509Certificate certificate) throws NoSuchAlgorithmException, CertificateEncodingException {
            this.x5t = getThumbPrint(certificate);
            return this;
        }

        public String getThumbPrint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            return Base64Url.encode(digest);
        }

        @Override
        protected String encodeHeader(Algorithm alg) {
            StringBuilder builder = new StringBuilder("{");
            if (type != null) builder.append("\"typ\":\"").append(type).append("\",");
            builder.append("\"alg\":\"").append(alg.toString()).append("\"");

            if (contentType != null) builder.append(",\"cty\":\"").append(contentType).append("\"");
            if (x5t != null) builder.append(",\"x5t\":\"").append(x5t).append("\"");
            builder.append("}");
            try {
                return Base64Url.encode(builder.toString().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public AccessToken transformAccessToken(KeycloakSession session, AccessToken token, RealmModel realm, ClientModel client, UserModel user,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        Set<ProtocolMapperModel> mappings = new ClientSessionCode(realm, clientSession).getRequestedProtocolMappers();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (ProtocolMapperModel mapping : mappings) {

            ProtocolMapper mapper = (ProtocolMapper)sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper == null || !(mapper instanceof WSFedOIDCAccessTokenMapper)) continue;
            token = ((WSFedOIDCAccessTokenMapper)mapper).transformAccessToken(token, mapping, session, userSession, clientSession);

        }
        return token;
    }
}
