package org.keycloak.protocol.ciba.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CIBAAuthReqId {
    private static final String EXPIRATION_NOTE = "exp";
    private static final String SCOPE_NOTE = "scope";
    private static final String USER_SESSION_ID_NOTE = "user_session_id";
    private static final String CLIENT_ID_NOTE = "client_id";
    private static final String AUTH_RESULT_ID_NOTE = "auth_result_id";
    private static final String THROTTLING_ID_NOTE = "throttling_id";

    private final int expiration;
    private final String scope;
    private final String userSessionIdWillBeCreated;
    private final String clientId;
    private final String authResultId;
    private String throttlingId;

    public CIBAAuthReqId(int expiration, String scope, String userSessionIdWillBeCreated, String clientId, String authResultId, String throttlingId) {
        this.expiration = expiration;
        this.scope = scope;
        this.userSessionIdWillBeCreated = userSessionIdWillBeCreated;
        this.clientId = clientId;
        this.authResultId = authResultId;
        this.throttlingId = throttlingId;
    }

    private CIBAAuthReqId(Map<String, String> data) {
        expiration = Integer.parseInt(data.get(EXPIRATION_NOTE));
        scope = data.get(SCOPE_NOTE);
        userSessionIdWillBeCreated = data.get(USER_SESSION_ID_NOTE);
        clientId = data.get(CLIENT_ID_NOTE);
        authResultId = data.get(AUTH_RESULT_ID_NOTE);
        if (data.containsKey(THROTTLING_ID_NOTE)) throttlingId = data.get(THROTTLING_ID_NOTE);
    }

    public static final CIBAAuthReqId deserializeCode(Map<String, String> data) {
        return new CIBAAuthReqId(data);
    }

    public Map<String, String> serializeCode() {
        Map<String, String> result = new HashMap<>();
        result.put(EXPIRATION_NOTE, String.valueOf(expiration));
        result.put(SCOPE_NOTE, scope);
        result.put(USER_SESSION_ID_NOTE, userSessionIdWillBeCreated);
        result.put(CLIENT_ID_NOTE, clientId);
        result.put(AUTH_RESULT_ID_NOTE, authResultId.toString());
        if (throttlingId != null) result.put(THROTTLING_ID_NOTE, throttlingId.toString());
        return result;
    }

    public int getExpiration() {
        return expiration;
    }

    public String getScope() {
        return scope;
    }

    public String getUserSessionId() {
        return userSessionIdWillBeCreated;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAuthResultId() {
        return authResultId;
    }

    public String getThrottlingId() {
        return throttlingId;
    }
}
