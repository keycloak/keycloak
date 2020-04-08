package org.keycloak.protocol.ciba.utils;

import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.CodeToTokenStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ciba.utils.DecoupledAuthnResultParser.ParseResult;

public class EarlyAccessBlockerParser {

    private static final Logger logger = Logger.getLogger(EarlyAccessBlockerParser.class);

    public static void persistEarlyAccessBlocker(KeycloakSession session, String id, EarlyAccessBlocker earlyAccessBlockerData, int expires_in) {
        CodeToTokenStoreProvider codeStore = session.getProvider(CodeToTokenStoreProvider.class);

        if (id == null) {
            throw new IllegalStateException("ID not present in the data");
        }
        UUID key = UUID.fromString(id);

        Map<String, String> serialized = earlyAccessBlockerData.serializeCode();
        codeStore.put(key, expires_in, serialized);
    }

    public static ParseResult parseEarlyAccessBlocker(KeycloakSession session, String id) {
        ParseResult result = new ParseResult(id);

        // Parse UUID
        UUID storeKeyUUID;
        try {
            storeKeyUUID = UUID.fromString(id);
        } catch (IllegalArgumentException re) {
            logger.warn("Invalid format of the UUID in the code");
            return null;
        }

        CodeToTokenStoreProvider earlyAccessBlockerStore = session.getProvider(CodeToTokenStoreProvider.class);
        Map<String, String> earlyAccessBlockerData = earlyAccessBlockerStore.remove(storeKeyUUID);

        if (earlyAccessBlockerData == null) return result.notFoundEarlyAccessBlocker();

        result.earlyAccessBlockerData = EarlyAccessBlocker.deserializeCode(earlyAccessBlockerData);

        // Finally doublecheck if code is not expired
        int currentTime = Time.currentTime();
        if (currentTime > result.earlyAccessBlockerData.getExpiration()) {
            return result.expiredEarlyAccessBlocker();
        }

        return result;
    }

    public static class ParseResult {

        private final String id;
        private EarlyAccessBlocker earlyAccessBlockerData;

        private boolean isNotFoundEarlyAccessBlocker = false;
        private boolean isExpiredEarlyAccessBlocker = false;

        private ParseResult(String id, EarlyAccessBlocker earlyAccessBlockerData) {
            this.id = id;
            this.earlyAccessBlockerData = earlyAccessBlockerData;
        }

        private ParseResult(String id) {
            this.id = id;
        }

        public EarlyAccessBlocker earlyAccessBlockerData() {
            return earlyAccessBlockerData;
        }

        public boolean isNotFoundEarlyAccessBlocker() {
            return isNotFoundEarlyAccessBlocker;
        }

        public boolean isExpiredEarlyAccessBlocker() {
            return isExpiredEarlyAccessBlocker;
        }

        private ParseResult notFoundEarlyAccessBlocker() {
            this.isNotFoundEarlyAccessBlocker = true;
            return this;
        }

        private ParseResult expiredEarlyAccessBlocker() {
            this.isExpiredEarlyAccessBlocker = true;
            return this;
        }
    }
}
