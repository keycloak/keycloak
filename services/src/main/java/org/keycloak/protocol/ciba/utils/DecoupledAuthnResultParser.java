package org.keycloak.protocol.ciba.utils;

import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.CodeToTokenStoreProvider;
import org.keycloak.models.KeycloakSession;

public class DecoupledAuthnResultParser {

    private static final Logger logger = Logger.getLogger(DecoupledAuthnResultParser.class);

    public static void persistDecoupledAuthnResult(KeycloakSession session, String id, DecoupledAuthnResult decoupledAuthnResultData, int expires_in) {
        CodeToTokenStoreProvider codeStore = session.getProvider(CodeToTokenStoreProvider.class);

        if (id == null) {
            throw new IllegalStateException("ID not present in the data");
        }
        UUID key = UUID.fromString(id);

        Map<String, String> serialized = decoupledAuthnResultData.serializeCode();
        codeStore.put(key, expires_in, serialized);
    }

    public static ParseResult parseDecoupledAuthnResult(KeycloakSession session, String id) {
        ParseResult result = new ParseResult(id);

        // Parse UUID
        UUID storeKeyUUID;
        try {
            storeKeyUUID = UUID.fromString(id);
        } catch (IllegalArgumentException re) {
            logger.warn("Invalid format of the UUID in the code");
            return null;
        }

        CodeToTokenStoreProvider decoupledAuthnResultStore = session.getProvider(CodeToTokenStoreProvider.class);
        Map<String, String> decoupledAuthnResultData = decoupledAuthnResultStore.remove(storeKeyUUID);

        // Either code not available or was already used
        if (decoupledAuthnResultData == null) {
            logger.warnf("Decoupled Authn not yet completed. code = '%s'", storeKeyUUID);
            return result.notYetDecoupledAuthnResult();
        }

        result.decoupledAuthnResultData = DecoupledAuthnResult.deserializeCode(decoupledAuthnResultData);

        // Finally doublecheck if code is not expired
        int currentTime = Time.currentTime();
        if (currentTime > result.decoupledAuthnResultData.getExpiration()) {
            return result.expiredDecoupledAuthnResult();
        }

        return result;
    }

    public static class ParseResult {

        private final String id;
        private DecoupledAuthnResult decoupledAuthnResultData;

        private boolean isNotYetDecoupledAuthnResult = false;
        private boolean isExpiredDecoupledAuthnResult = false;

        private ParseResult(String id) {
            this.id = id;
        }

        public DecoupledAuthnResult decoupledAuthnResultData() {
            return decoupledAuthnResultData;
        }

        public boolean isNotYetDecoupledAuthnResult() {
            return isNotYetDecoupledAuthnResult;
        }

        public boolean isExpiredDecoupledAuthnResult() {
            return isExpiredDecoupledAuthnResult;
        }


        private ParseResult notYetDecoupledAuthnResult() {
            this.isNotYetDecoupledAuthnResult = true;
            return this;
        }

        private ParseResult expiredDecoupledAuthnResult() {
            this.isExpiredDecoupledAuthnResult = true;
            return this;
        }
    }
}
