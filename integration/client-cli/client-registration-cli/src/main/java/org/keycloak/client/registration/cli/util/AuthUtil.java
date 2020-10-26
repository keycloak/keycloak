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

package org.keycloak.client.registration.cli.util;

import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.config.RealmConfigData;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.UUID;

import static java.lang.System.currentTimeMillis;
import static org.keycloak.client.registration.cli.util.ConfigUtil.checkAuthInfo;
import static org.keycloak.client.registration.cli.util.ConfigUtil.saveMergeConfig;
import static org.keycloak.client.registration.cli.util.HttpUtil.APPLICATION_FORM_URL_ENCODED;
import static org.keycloak.client.registration.cli.util.HttpUtil.APPLICATION_JSON;
import static org.keycloak.client.registration.cli.util.HttpUtil.doPost;
import static org.keycloak.client.registration.cli.util.HttpUtil.urlencode;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class AuthUtil {

    public static String ensureToken(ConfigData config) {

        checkAuthInfo(config);

        RealmConfigData realmConfig = config.sessionRealmConfigData();

        long now = currentTimeMillis();

        // check expires of access_token against time
        // if it's less than 5s to expiry, renew it
        if (realmConfig.getExpiresAt() - now < 5000) {

            // check refresh_token against expiry time
            // if it's less than 5s to expiry, fail with credentials expired
            if (realmConfig.getRefreshExpiresAt() != null && realmConfig.getRefreshExpiresAt() - now < 5000) {
                throw new RuntimeException("Session has expired. Login again with '" + OsUtil.CMD + " config credentials'");
            }

            if (realmConfig.getSigExpiresAt() != null && realmConfig.getSigExpiresAt() - now < 5000) {
                throw new RuntimeException("Session has expired. Login again with '" + OsUtil.CMD + " config credentials'");
            }

            try {
                String authorization = null;

                StringBuilder body = new StringBuilder();
                if (realmConfig.getRefreshToken() != null) {
                    body.append("grant_type=refresh_token")
                            .append("&refresh_token=").append(realmConfig.getRefreshToken());
                } else {
                    body.append("grant_type=").append(realmConfig.getGrantTypeForAuthentication());
                }

                body.append("&client_id=").append(urlencode(realmConfig.getClientId()));

                if (realmConfig.getSigningToken() != null) {
                    body.append("&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                            .append("&client_assertion=").append(realmConfig.getSigningToken());
                } else if (realmConfig.getSecret() != null) {
                    authorization = BasicAuthHelper.createHeader(realmConfig.getClientId(), realmConfig.getSecret());
                }

                InputStream result = doPost(realmConfig.serverUrl() + "/realms/" + realmConfig.realm() + "/protocol/openid-connect/token",
                        APPLICATION_FORM_URL_ENCODED, APPLICATION_JSON, body.toString(), authorization);

                AccessTokenResponse token = JsonSerialization.readValue(result, AccessTokenResponse.class);

                saveMergeConfig(cfg -> {
                    RealmConfigData realmData = cfg.sessionRealmConfigData();
                    realmData.setToken(token.getToken());
                    realmData.setRefreshToken(token.getRefreshToken());
                    realmData.setExpiresAt(currentTimeMillis() + token.getExpiresIn() * 1000);
                    if (token.getRefreshToken() != null) {
                        realmData.setRefreshExpiresAt(currentTimeMillis() + token.getRefreshExpiresIn() * 1000);
                    }
                });
                return token.getToken();

            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unexpected error", e);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read Refresh Token response", e);
            }
        }

        return realmConfig.getToken();
    }

    public static AccessTokenResponse getAuthTokens(String server, String realm, String user, String password, String clientId) {
        StringBuilder body = new StringBuilder();
        try {
            body.append("grant_type=password")
                    .append("&username=").append(urlencode(user))
                    .append("&password=").append(urlencode(password))
                    .append("&client_id=").append(urlencode(clientId));

            InputStream result = doPost(server + "/realms/" + realm + "/protocol/openid-connect/token",
                    APPLICATION_FORM_URL_ENCODED, APPLICATION_JSON, body.toString(), null);
            return JsonSerialization.readValue(result, AccessTokenResponse.class);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected error: ", e);
        } catch (IOException e) {
            throw new RuntimeException("Error receiving response: ", e);
        }
    }

    public static AccessTokenResponse getAuthTokensByJWT(String server, String realm, String user, String password, String clientId, String signedRequestToken) {
        StringBuilder body = new StringBuilder();
        try {
            body.append("client_id=").append(urlencode(clientId))
                    .append("&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                    .append("&client_assertion=").append(signedRequestToken);

            if (user != null) {
                if (password == null) {
                    throw new RuntimeException("No password specified");
                }
                body.append("&grant_type=password")
                        .append("&username=").append(urlencode(user))
                        .append("&password=").append(urlencode(password));
            } else {
                body.append("&grant_type=client_credentials");
            }

            InputStream result = doPost(server + "/realms/" + realm + "/protocol/openid-connect/token",
                    APPLICATION_FORM_URL_ENCODED, APPLICATION_JSON, body.toString(), null);
            return JsonSerialization.readValue(result, AccessTokenResponse.class);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected error: ", e);
        } catch (IOException e) {
            throw new RuntimeException("Error receiving response: ", e);
        }
    }

    public static AccessTokenResponse getAuthTokensBySecret(String server, String realm, String user, String password, String clientId, String secret) {

        StringBuilder body = new StringBuilder();
        try {
            if (user != null) {
                if (password == null) {
                    throw new RuntimeException("No password specified");
                }

                body.append("client_id=").append(urlencode(clientId))
                        .append("&grant_type=password")
                        .append("&username=").append(urlencode(user))
                        .append("&password=").append(urlencode(password));
            } else {
                body.append("grant_type=client_credentials");
            }

            InputStream result = doPost(server + "/realms/" + realm + "/protocol/openid-connect/token",
                    APPLICATION_FORM_URL_ENCODED, APPLICATION_JSON, body.toString(), BasicAuthHelper.createHeader(clientId, secret));
            return JsonSerialization.readValue(result, AccessTokenResponse.class);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected error: ", e);
        } catch (IOException e) {
            throw new RuntimeException("Error receiving response: ", e);
        }
    }

    public static String getSignedRequestToken(String keystore, String storePass, String keyPass, String alias, int sigLifetime, String clientId, String realmInfoUrl) {

        KeyPair keypair = KeystoreUtil.loadKeyPairFromKeystore(keystore, storePass, keyPass, alias, KeystoreUtil.KeystoreFormat.JKS);

        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(UUID.randomUUID().toString());
        reqToken.issuer(clientId);
        reqToken.subject(clientId);
        reqToken.audience(realmInfoUrl);

        int now = Time.currentTime();
        reqToken.issuedAt(now);
        reqToken.expiration(now + sigLifetime);
        reqToken.notBefore(now);

        String signedRequestToken = new JWSBuilder()
                .jsonContent(reqToken)
                .rsa256(keypair.getPrivate());
        return signedRequestToken;
    }
}
