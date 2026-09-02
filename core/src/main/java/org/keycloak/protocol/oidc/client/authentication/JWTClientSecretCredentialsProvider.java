/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.client.authentication;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.adapters.config.AdapterConfig;

import org.jboss.logging.Logger;

/**
 * Client authentication based on JWT signed by client secret instead of private key .
 * See <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">specs</a> for more details.
 *
 */
public class JWTClientSecretCredentialsProvider implements ClientCredentialsProvider {

    private static final Logger logger = Logger.getLogger(JWTClientSecretCredentialsProvider.class);

    public static final String PROVIDER_ID = "secret-jwt";

    private SecretKey clientSecret;

    private String clientSecretJwtAlg = Algorithm.HS256;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(AdapterConfig deployment, Object config) {
        if (!(config instanceof Map)) {
            throw new RuntimeException("Configuration of jwt credentials by client secret is missing or incorrect for client '" + deployment.getResource() + "'. Check your adapter configuration");
        }

        Map<String, Object> cfg = (Map<String, Object>) config;
        String clientSecretString = (String) cfg.get("secret");
        if (clientSecretString == null) {
            throw new RuntimeException("Missing parameter secret-jwt in configuration of jwt for client " + deployment.getResource());
        }

        String clientSecretJwtAlg = (String) cfg.get("algorithm");
        if (clientSecretJwtAlg == null) {
            // "algorithm" field is optional. fallback to HS256.
            setClientSecret(clientSecretString); 
        } else if (isValidClientSecretJwtAlg(clientSecretJwtAlg)) {
            setClientSecret(clientSecretString, clientSecretJwtAlg); 
        } else {
            // invalid "algorithm" field
            throw new RuntimeException("Invalid parameter secret-jwt in configuration of jwt for client " + deployment.getResource());
        }
    }

    private boolean isValidClientSecretJwtAlg(String clientSecretJwtAlg) {
        boolean ret = false;
        if (Algorithm.HS256.equals(clientSecretJwtAlg) || Algorithm.HS384.equals(clientSecretJwtAlg) || Algorithm.HS512.equals(clientSecretJwtAlg))
            ret = true;
        return ret;
    }

    @Override
    public void setClientCredentials(AdapterConfig deployment, Map<String, String> requestHeaders, Map<String, String> formParams) {
        String signedToken = createSignedRequestToken(deployment.getResource(), deployment.getRealmInfoUrl());
        formParams.put(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT);
        formParams.put(OAuth2Constants.CLIENT_ASSERTION, signedToken);
    }

    public void setClientSecret(String clientSecretString) {
        // Get client secret and validate signature
        // According to <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">OIDC's client authentication spec</a>,
        // The HMAC (Hash-based Message Authentication Code) is calculated using the octets of the UTF-8 representation of the client_secret as the shared key. 
        // Use "HmacSHA256" consulting <a href="https://docs.oracle.com/javase/jp/8/docs/api/javax/crypto/Mac.html">java8 api</a>
        // because it must be implemented in every java platform.
        setClientSecret(clientSecretString, Algorithm.HS256);
    }

    public void setClientSecret(String clientSecretString, String algorithm) {
        clientSecret = new SecretKeySpec(clientSecretString.getBytes(StandardCharsets.UTF_8), JavaAlgorithm.getJavaAlgorithm(algorithm));
        clientSecretJwtAlg = algorithm;
    }

    public String createSignedRequestToken(String clientId, String realmInfoUrl) {
        return createSignedRequestToken(clientId, realmInfoUrl, clientSecretJwtAlg);
    }

    public String createSignedRequestToken(String clientId, String realmInfoUrl, String algorithm) {
        JsonWebToken jwt = createRequestToken(clientId, realmInfoUrl);
        String signedRequestToken = null;
        if (Algorithm.HS512.equals(algorithm)) {
            signedRequestToken = new JWSBuilder().jsonContent(jwt).hmac512(clientSecret);
        } else if (Algorithm.HS384.equals(algorithm)) {
            signedRequestToken = new JWSBuilder().jsonContent(jwt).hmac384(clientSecret);
        } else {
            signedRequestToken = new JWSBuilder().jsonContent(jwt).hmac256(clientSecret);
        }
        return signedRequestToken;
    }

    protected JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
        // According to <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">OIDC's client authentication spec</a>,
        // JWT claims is the same as one by private_key_jwt

        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(SecretGenerator.getInstance().generateSecureID());
        reqToken.issuer(clientId);
        reqToken.subject(clientId);
        reqToken.audience(realmInfoUrl);

        long now = Time.currentTime();
        reqToken.iat(now);
        // the same as in KEYCLOAK-2986, JWTClientCredentialsProvider's timeout field
        reqToken.exp(now + 10);
        reqToken.nbf(now);
        return reqToken;
    }

}
