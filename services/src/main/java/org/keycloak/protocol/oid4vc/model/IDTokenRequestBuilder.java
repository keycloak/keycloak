/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.model;

import java.util.UUID;

import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureSignerContext;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.JsonWebToken;

/**
 * Build an IDTokenRequest as a response to an AuthorizationRequest
 * <p>
 * https://hub.ebsi.eu/conformance/build-solutions/issue-to-holder-functional-flows#in-time-issuance
 * <p>
 * openid://
 *   client_id=https://my-issuer.rocks/auth
 *   response_type=id_token
 *   scope=openid
 *   redirect_uri=https://my-issuer.rocks/auth/direct_post
 *   request=eyJ0eXAiOiJKV1Qi...hc9pFWsewRQ
 * <p>
 * JWT Header: {
 *   typ: 'JWT',
 *   alg: 'ES256',
 *   kid: 'c4KrepJXzmBMW-qo2ntDCwkTgLm2Cb_5eabzkljTh_0'
 * }
 * JWT Payload: {
 *   iss: 'https://my-issuer.rocks/auth',
 *   aud: 'did:key:z2dmzD81cgPx8Vki7JbuuMmFYrWPgYoytykUZ3eyqht1j9KbsEYvdrjxMjQ4tpnje9BDBTzuNDP3knn6qLZErzd4bJ5go2CChoPjd5GAH3zpFJP5fuwSk66U5Pq6EhF4nKnHzDnznEP8fX99nZGgwbAh1o7Gj1X52Tdhf7U4KTk66xsA5r',
 *   exp: 1589699162,
 *   response_type: 'id_token',
 *   response_mode: 'direct_post',
 *   client_id: 'https://my-issuer.rocks/auth',
 *   redirect_uri: 'https://my-issuer.rocks/auth/direct_post',
 *   scope: 'openid',
 *   nonce: 'n-0S6_WzA2Mj'
 * }
 * <p>
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class IDTokenRequestBuilder {

    private String clientId;
    private String responseType = "id_token";
    private String scope = "openid";
    private String redirectUri;
    private final JsonWebToken jwt = new JsonWebToken();

    public IDTokenRequestBuilder withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public IDTokenRequestBuilder withRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public IDTokenRequestBuilder withResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public IDTokenRequestBuilder withScope(String scope) {
        this.scope = scope;
        return this;
    }

    public IDTokenRequestBuilder withJwtAudience(String aud) {
        jwt.audience(aud);
        return this;
    }

    public IDTokenRequestBuilder withJwtIssuer(String iss) {
        jwt.issuer(iss);
        return this;
    }

    public IDTokenRequestBuilder withJwtNonce(String nonce) {
        jwt.getOtherClaims().put("nonce", nonce);
        return this;
    }

    public IDTokenRequestBuilder withJwtSubject(String sub) {
        jwt.subject(sub);
        return this;
    }

    public IDTokenRequest buildAndSign(KeycloakSession session, RealmModel realm) {

        IDTokenRequest idTokenReq = new IDTokenRequest();
        idTokenReq.setScope(scope);
        idTokenReq.setClientId(clientId);
        idTokenReq.setRedirectUri(redirectUri);
        idTokenReq.setResponseType(responseType);

        jwt.exp((long) Time.currentTime() + 300); // 5min
        jwt.id(String.format("urn:uuid:%s", UUID.randomUUID()));

        String algo = Algorithm.ES256;
        KeyManager keyManager = session.keys();
        KeyWrapper signingKey = keyManager.getActiveKey(realm, KeyUse.SIG, algo);
        if (signingKey == null)
            throw new IllegalStateException("No active signing key in for: " + algo);

        SignatureSignerContext signer = new ServerECDSASignatureSignerContext(signingKey);
        String signedJwt = new JWSBuilder()
                .kid(signingKey.getKid())
                .jsonContent(jwt)
                .sign(signer);

        idTokenReq.setRequest(signedJwt);
        return idTokenReq;
    }
}
