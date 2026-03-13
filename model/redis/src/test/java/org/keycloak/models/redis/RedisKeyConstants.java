/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
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

package org.keycloak.models.redis;

/**
 * Constants for Redis key prefixes used in ATDD tests.
 * Centralizes key naming to avoid duplication and typos.
 */
public class RedisKeyConstants {
    
    // Key prefixes
    public static final String USER_SESSION_PREFIX = "kc:sessions:";
    public static final String AUTH_SESSION_PREFIX = "kc:authSessions:";
    public static final String ACTION_TOKEN_PREFIX = "kc:actionTokens:";
    public static final String LOGIN_FAILURE_PREFIX = "kc:loginFailures:";
    public static final String CLIENT_SESSION_PREFIX = "kc:clientSessions:";
    public static final String OFFLINE_SESSION_PREFIX = "kc:offlineSessions:";
    public static final String OFFLINE_CLIENT_SESSION_PREFIX = "kc:offlineClientSessions:";
    
    // Pattern queries
    public static final String USER_SESSION_PATTERN = USER_SESSION_PREFIX + "*";
    public static final String AUTH_SESSION_PATTERN = AUTH_SESSION_PREFIX + "*";
    public static final String ACTION_TOKEN_PATTERN = ACTION_TOKEN_PREFIX + "*";
    public static final String LOGIN_FAILURE_PATTERN = LOGIN_FAILURE_PREFIX + "*";
    public static final String CLIENT_SESSION_PATTERN = CLIENT_SESSION_PREFIX + "*";
    public static final String OFFLINE_SESSION_PATTERN = OFFLINE_SESSION_PREFIX + "*";
    
    // Helper methods for building keys
    public static String userSessionKey(String sessionId) {
        return USER_SESSION_PREFIX + sessionId;
    }
    
    public static String authSessionKey(String sessionId) {
        return AUTH_SESSION_PREFIX + sessionId;
    }
    
    public static String actionTokenKey(String tokenId) {
        return ACTION_TOKEN_PREFIX + tokenId;
    }
    
    public static String loginFailureKey(String userId) {
        return LOGIN_FAILURE_PREFIX + userId;
    }
    
    public static String loginFailureIpKey(String ipAddress) {
        return LOGIN_FAILURE_PREFIX + "ip:" + ipAddress;
    }
    
    public static String clientSessionKey(String userSessionId, String clientId) {
        return CLIENT_SESSION_PREFIX + userSessionId + ":" + clientId;
    }
    
    public static String clientSessionPattern(String userSessionId) {
        return CLIENT_SESSION_PREFIX + userSessionId + ":*";
    }
    
    public static String offlineSessionKey(String sessionId) {
        return OFFLINE_SESSION_PREFIX + sessionId;
    }
    
    public static String offlineClientSessionKey(String userSessionId, String clientId) {
        return OFFLINE_CLIENT_SESSION_PREFIX + userSessionId + ":" + clientId;
    }
    
    // Private constructor to prevent instantiation
    private RedisKeyConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}
