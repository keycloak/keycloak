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

package org.keycloak.adapters.authentication;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.Time;

/**
 * Client authentication based on JWT signed by client private key .
 * See <a href="https://tools.ietf.org/html/rfc7519">specs</a> for more details.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWTClientCredentialsProvider implements ClientCredentialsProvider {

    public static final String PROVIDER_ID = "jwt";

    private KeyPair keyPair;
    private JWK publicKeyJwk;

    private int tokenTimeout;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    public void setupKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
        this.publicKeyJwk = JWKBuilder.create().rs256(keyPair.getPublic());
    }

    public void setTokenTimeout(int tokenTimeout) {
        this.tokenTimeout = tokenTimeout;
    }

    protected int getTokenTimeout() {
        return tokenTimeout;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    @Override
    public void init(KeycloakDeployment deployment, Object config) {
        if (!(config instanceof Map)) {
            throw new RuntimeException("Configuration of jwt credentials is missing or incorrect for client '" + deployment.getResourceName() + "'. Check your adapter configuration");
        }

        Map<String, Object> cfg = (Map<String, Object>) config;

        String clientKeystoreFile =  (String) cfg.get("client-keystore-file");
        if (clientKeystoreFile == null) {
            throw new RuntimeException("Missing parameter client-keystore-file in configuration of jwt for client " + deployment.getResourceName());
        }

        String clientKeystoreType = (String) cfg.get("client-keystore-type");
        KeystoreUtil.KeystoreFormat clientKeystoreFormat = clientKeystoreType==null ? KeystoreUtil.KeystoreFormat.JKS : Enum.valueOf(KeystoreUtil.KeystoreFormat.class, clientKeystoreType.toUpperCase());

        String clientKeystorePassword =  (String) cfg.get("client-keystore-password");
        if (clientKeystorePassword == null) {
            throw new RuntimeException("Missing parameter client-keystore-password in configuration of jwt for client " + deployment.getResourceName());
        }

        String clientKeyPassword = (String) cfg.get("client-key-password");
        if (clientKeyPassword == null) {
            clientKeyPassword = clientKeystorePassword;
        }

        String clientKeyAlias =  (String) cfg.get("client-key-alias");
        if (clientKeyAlias == null) {
            clientKeyAlias = deployment.getResourceName();
        }

        KeyPair keyPair = KeystoreUtil.loadKeyPairFromKeystore(clientKeystoreFile, clientKeystorePassword, clientKeyPassword, clientKeyAlias, clientKeystoreFormat);
        setupKeyPair(keyPair);

        this.tokenTimeout = asInt(cfg, "token-timeout", 10);
    }

    // TODO: Generic method for this?
    private Integer asInt(Map<String, Object> cfg, String cfgKey, int defaultValue) {
        Object cfgObj = cfg.get(cfgKey);
        if (cfgObj == null) {
            return defaultValue;
        }

        if (cfgObj instanceof String) {
            return Integer.parseInt(cfgObj.toString());
        } else if (cfgObj instanceof Number) {
            return ((Number) cfgObj).intValue();
        } else {
            throw new IllegalArgumentException("Can't parse " + cfgKey + " from the config. Value is " + cfgObj);
        }
    }

    @Override
    public void setClientCredentials(KeycloakDeployment deployment, Map<String, String> requestHeaders, Map<String, String> formParams) {
        String signedToken = createSignedRequestToken(deployment.getResourceName(), deployment.getRealmInfoUrl());

        formParams.put(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT);
        formParams.put(OAuth2Constants.CLIENT_ASSERTION, signedToken);
    }

    public String createSignedRequestToken(String clientId, String realmInfoUrl) {
        JsonWebToken jwt = createRequestToken(clientId, realmInfoUrl);
        return new JWSBuilder()
                .kid(publicKeyJwk.getKeyId())
                .jsonContent(jwt)
                .rsa256(keyPair.getPrivate());
    }

    protected JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(AdapterUtils.generateId());
        reqToken.issuer(clientId);
        reqToken.subject(clientId);
        reqToken.audience(realmInfoUrl);

        int now = Time.currentTime();
        reqToken.issuedAt(now);
        reqToken.expiration(now + this.tokenTimeout);
        reqToken.notBefore(now);

        return reqToken;
    }
}
