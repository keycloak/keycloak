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

import org.keycloak.OAuth2Constants;
import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.config.ConfigHandler;
import org.keycloak.client.registration.cli.config.ConfigUpdateOperation;
import org.keycloak.client.registration.cli.config.InMemoryConfigHandler;
import org.keycloak.client.registration.cli.config.RealmConfigData;
import org.keycloak.representations.AccessTokenResponse;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ConfigUtil {

    public static final String DEFAULT_CONFIG_FILE_STRING = OsUtil.OS_ARCH.isWindows() ? "%HOMEDRIVE%%HOMEPATH%\\.keycloak\\kcreg.config" : "~/.keycloak/kcreg.config";

    public static final String DEFAULT_CONFIG_FILE_PATH = System.getProperty("user.home") + "/.keycloak/kcreg.config";

    private static ConfigHandler handler;

    public static ConfigHandler getHandler() {
        return handler;
    }

    public static void setHandler(ConfigHandler handler) {
        ConfigUtil.handler = handler;
    }

    public static String getRegistrationToken(RealmConfigData data, String clientId) {
        String token = data.getClients().get(clientId);
        return token == null || token.length() == 0 ? null : token;
    }

    public static void setRegistrationToken(RealmConfigData data, String clientId, String token) {
        data.getClients().put(clientId, token == null ? "" : token);
    }

    public static void saveTokens(AccessTokenResponse tokens, String endpoint, String realm, String clientId, String signKey, Long sigExpiresAt, String secret,
                                  String grantTypeForAuthentication) {
        handler.saveMergeConfig(config -> {
            config.setServerUrl(endpoint);
            config.setRealm(realm);

            RealmConfigData realmConfig = config.ensureRealmConfigData(endpoint, realm);
            realmConfig.setToken(tokens.getToken());
            realmConfig.setRefreshToken(tokens.getRefreshToken());
            realmConfig.setSigningToken(signKey);
            realmConfig.setSecret(secret);
            realmConfig.setExpiresAt(System.currentTimeMillis() + tokens.getExpiresIn() * 1000);
            if (realmConfig.getRefreshToken() != null) {
                realmConfig.setRefreshExpiresAt(tokens.getRefreshExpiresIn() == 0 ?
                        Long.MAX_VALUE : System.currentTimeMillis() + tokens.getRefreshExpiresIn() * 1000);
            }
            realmConfig.setSigExpiresAt(sigExpiresAt);
            realmConfig.setClientId(clientId);
            realmConfig.setGrantTypeForAuthentication(grantTypeForAuthentication);
        });
    }

    public static void checkServerInfo(ConfigData config) {
        if (config.getServerUrl() == null || config.getRealm() == null) {
            throw new RuntimeException("No server or realm specified. Use --server, --realm, or '" + OsUtil.CMD + " config credentials'.");
        }
    }

    public static void checkAuthInfo(ConfigData config) {
        checkServerInfo(config);
    }

    public static boolean credentialsAvailable(ConfigData config) {
        // Just supporting "client_credentials" grant type for the case when refresh token is missing
        boolean credsAvailable = config.getServerUrl() != null && config.getRealm() != null
                && config.sessionRealmConfigData() != null &&
                (config.sessionRealmConfigData().getRefreshToken() != null || (config.sessionRealmConfigData().getToken() != null && OAuth2Constants.CLIENT_CREDENTIALS.equals(config.sessionRealmConfigData().getGrantTypeForAuthentication())));
        return credsAvailable;
    }

    public static ConfigData loadConfig() {
        if (handler == null) {
            throw new RuntimeException("No ConfigHandler set");
        }

        return handler.loadConfig();
    }

    public static void saveMergeConfig(ConfigUpdateOperation op) {
        if (handler == null) {
            throw new RuntimeException("No ConfigHandler set");
        }

        handler.saveMergeConfig(op);
    }

    public static void setupInMemoryHandler(ConfigData config) {
        InMemoryConfigHandler memhandler = null;
        if (handler instanceof InMemoryConfigHandler) {
            memhandler = (InMemoryConfigHandler) handler;
        } else {
            memhandler = new InMemoryConfigHandler();
            handler = memhandler;
        }
        memhandler.setConfigData(config);
    }
}
