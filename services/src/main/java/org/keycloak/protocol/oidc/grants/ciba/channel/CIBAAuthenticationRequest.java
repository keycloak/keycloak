/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */
package org.keycloak.protocol.oidc.grants.ciba.channel;

import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.keycloak.OAuth2Constants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.services.Urls;
import org.keycloak.util.TokenUtil;

/**
 * <p>Represents an authentication request sent by a consumption device (CD).
 *
 * <p>A authentication request can be serialized to a JWE so that it can be exchanged with authentication devices (AD)
 * to communicate and authorize the authentication request made by consumption devices (CDs).
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class CIBAAuthenticationRequest extends JsonWebToken {

    /**
     * Deserialize the given {@code jwe} to a {@link CIBAAuthenticationRequest} instance.
     *
     * @param session the session
     * @param jwe the authentication request in JWE format.
     * @return the authentication request instance
     * @throws Exception
     */
    public static CIBAAuthenticationRequest deserialize(KeycloakSession session, String jwe) {
        SecretKey aesKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, Algorithm.AES).getSecretKey();
        SecretKey hmacKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, Algorithm.HS256).getSecretKey();

        try {
            byte[] contentBytes = TokenUtil.jweDirectVerifyAndDecode(aesKey, hmacKey, jwe);
            jwe = new String(contentBytes, "UTF-8");
        } catch (JWEException | UnsupportedEncodingException e) {
            throw new RuntimeException("Error decoding auth_req_id.", e);
        }

        return session.tokens().decode(jwe, CIBAAuthenticationRequest.class);
    }

    public static final String SESSION_STATE = IDToken.SESSION_STATE;
    public static final String AUTH_RESULT_ID = "auth_result_id";

    @JsonProperty(OAuth2Constants.SCOPE)
    protected String scope;

    @JsonProperty(AUTH_RESULT_ID)
    protected String authResultId;

    @JsonProperty(CibaGrantType.BINDING_MESSAGE)
    protected String bindingMessage;

    @JsonProperty(OAuth2Constants.ACR_VALUES)
    protected String acrValues;

    @JsonIgnore
    protected ClientModel client;

    @JsonIgnore
    protected String clientNotificationToken;

    @JsonIgnore
    protected UserModel user;

    public CIBAAuthenticationRequest() {
        // for reflection
    }

    public CIBAAuthenticationRequest(KeycloakSession session, UserModel user, ClientModel client) {
        id(KeycloakModelUtils.generateId());
        issuedNow();
        RealmModel realm = session.getContext().getRealm();
        issuer(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        audience(getIssuer());
        subject(user.getId());
        issuedFor(client.getClientId());
        setAuthResultId(KeycloakModelUtils.generateId());
        setClient(client);
        setUser(user);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getAuthResultId() {
        return authResultId;
    }

    public void setAuthResultId(String authResultId) {
        this.authResultId = authResultId;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String binding_message) {
        this.bindingMessage = binding_message;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    /**
     * Serializes this instance to a JWE.
     *
     * @param session the session
     * @return the JWE
     */
    public String serialize(KeycloakSession session) {
        try {
            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, Algorithm.HS256);
            SignatureSignerContext signer = signatureProvider.signer();
            String encodedJwt = new JWSBuilder().type("JWT").jsonContent(this).sign(signer);
            SecretKey aesKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.ENC, Algorithm.AES).getSecretKey();
            SecretKey hmacKey = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, Algorithm.HS256).getSecretKey();

            return TokenUtil.jweDirectEncode(aesKey, hmacKey, encodedJwt.getBytes("UTF-8"));
        } catch (JWEException | UnsupportedEncodingException e) {
            throw new RuntimeException("Error encoding auth_req_id.", e);
        }
    }

    public void setClient(ClientModel client) {
        this.client = client;
    }

    public ClientModel getClient() {
        return client;
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public UserModel getUser() {
        return user;
    }
}
