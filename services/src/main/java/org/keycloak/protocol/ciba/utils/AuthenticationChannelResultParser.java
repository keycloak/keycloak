/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.ciba.utils;

import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.CodeToTokenStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ciba.AuthenticationChannelResult;

public class AuthenticationChannelResultParser {

    private static final Logger logger = Logger.getLogger(AuthenticationChannelResultParser.class);

    public static void persistAuthenticationChannelResult(KeycloakSession session, String id, AuthenticationChannelResult authenticationChannelResultData, int expires_in) {
        if (id == null) {
            throw new IllegalStateException("ID not present in the data");
        }
        UUID key = UUID.fromString(id);

        CodeToTokenStoreProvider codeStore = session.getProvider(CodeToTokenStoreProvider.class);

        Map<String, String> serialized = authenticationChannelResultData.serializeCode();
        codeStore.put(key, expires_in, serialized);
    }

    public static ParseResult parseAuthenticationChannelResult(KeycloakSession session, String id) {
        ParseResult result = new ParseResult();

        // Parse UUID
        UUID storeKeyUUID;
        try {
            storeKeyUUID = UUID.fromString(id);
        } catch (IllegalArgumentException re) {
            logger.warn("Invalid format of the UUID in the code");
            return null;
        }

        CodeToTokenStoreProvider codeStore = session.getProvider(CodeToTokenStoreProvider.class);
        Map<String, String> authenticationChannelResultData = codeStore.remove(storeKeyUUID);

        // Either code not available or was already used
        if (authenticationChannelResultData == null) {
            logger.warnf("Authentication Channel not yet completed. code = '%s'", storeKeyUUID);
            return result.notYetAuthenticationChannelResult();
        }

        result.authenticationChannelResultData = AuthenticationChannelResult.deserializeCode(authenticationChannelResultData);

        // Finally doublecheck if code is not expired
        if (Time.currentTime() > result.authenticationChannelResultData.getExpiration()) {
            return result.expiredAuthenticationChannelResult();
        }

        return result;
    }

    public static class ParseResult {

        private AuthenticationChannelResult authenticationChannelResultData;

        private boolean isNotYetAuthenticationChannelResult = false;
        private boolean isExpiredAuthenticationChannelResult = false;

        public AuthenticationChannelResult authenticationChannelResultData() {
            return authenticationChannelResultData;
        }

        public boolean isNotYetAuthenticationChannelResult() {
            return isNotYetAuthenticationChannelResult;
        }

        public boolean isExpiredAuthenticationChannelResult() {
            return isExpiredAuthenticationChannelResult;
        }


        private ParseResult notYetAuthenticationChannelResult() {
            this.isNotYetAuthenticationChannelResult = true;
            return this;
        }

        private ParseResult expiredAuthenticationChannelResult() {
            this.isExpiredAuthenticationChannelResult = true;
            return this;
        }
    }
}
