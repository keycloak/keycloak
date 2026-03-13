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

import java.util.concurrent.TimeUnit;

/**
 * Centralized constants for test classes to avoid hard-coded literals.
 */
public final class TestConstants {
    
    private TestConstants() {
        // Utility class - prevent instantiation
    }
    
    // Test Realm Constants
    public static final String TEST_REALM_ID = "test-realm";
    public static final String TEST_REALM_NAME = "TestRealm";
    
    // Test User Constants
    public static final String TEST_USER_ID = "test-user";
    public static final String TEST_USER_ID_2 = "test-user-2";
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_EMAIL = "user@test.com";
    
    // Test Client Constants
    public static final String TEST_CLIENT_ID = "test-client";
    public static final String TEST_CLIENT_ID_2 = "test-client-2";
    
    // Test Session Constants
    public static final String TEST_SESSION_ID = "session-1";
    public static final String TEST_SESSION_ID_2 = "session-2";
    public static final String TEST_AUTH_SESSION_ID = "auth-session-1";
    public static final String CUSTOM_SESSION_ID = "custom-session-id";
    
    // IP Addresses
    public static final String TEST_IP_ADDRESS = "192.168.1.1";
    public static final String TEST_IP_ADDRESS_2 = "10.0.0.1";
    
    // Authentication Methods
    public static final String AUTH_METHOD_PASSWORD = "password";
    public static final String AUTH_METHOD_OTP = "otp";
    
    // Redis Key Prefixes
    public static final String REDIS_KEY_PREFIX = "kc:";
    public static final String REDIS_TEST_PREFIX = "test:";
    public static final String LOCK_KEY_PREFIX = "lock:";
    
    // Redis Connection Info
    public static final String REDIS_LOCALHOST_URL = "redis://localhost:6379";
    public static final String REDIS_CLUSTER_URL = "redis://cluster:6379";
    
    // TTL and Timeout Constants (in seconds)
    public static final long TTL_300_SECONDS = 300L;
    public static final long TTL_600_SECONDS = 600L;
    public static final long TTL_900_SECONDS = 900L;
    public static final long TTL_1800_SECONDS = 1800L;
    public static final long TTL_3600_SECONDS = 3600L;
    public static final long TTL_86400_SECONDS = 86400L;
    
    public static final int SSO_SESSION_MAX_LIFESPAN = 3600;
    public static final int OFFLINE_SESSION_IDLE_TIMEOUT = 86400;
    public static final int AUTH_SESSION_TTL = 300;

    // Realm-based auth session TTL constants (mirrors Keycloak default realm config)
    public static final int DEFAULT_REALM_AUTH_SESSION_LIFESPAN = 1800; // max(accessCodeLifespanLogin=1800, accessCodeLifespanUserAction=300, accessCodeLifespan=60)
    public static final int CUSTOM_REALM_AUTH_SESSION_LIFESPAN = 600;
    public static final int LOGIN_FAILURE_TTL = 900;
    public static final int LOCK_TIMEOUT = 60;
    
    // TTL in milliseconds
    public static final long TTL_300_MILLIS = 300000L;
    public static final long TTL_3600_MILLIS = 3600000L;
    public static final long TTL_86400_MILLIS = 86400000L;
    
    // Test Task/Event Names
    public static final String TASK_NAME_1 = "task1";
    public static final String TASK_NAME_2 = "task2";
    public static final String TASK_KEY = "my-task-key";
    
    // Test Data Values
    public static final String TEST_VALUE = "test-value";
    public static final String TEST_DATA_NAME = "test";
    public static final String TEST_RESULT = "result";
    public static final String TEST_ACTION = "test-action";
    
    // Note Keys
    public static final String NOTE_KEY_1 = "key1";
    public static final String NOTE_KEY_2 = "key2";
    public static final String NOTE_VALUE_1 = "value1";
    public static final String NOTE_VALUE_2 = "value2";
    public static final String UPDATED_VALUE = "updated";
    
    // Authentication/Authorization
    public static final String REDIRECT_URI = "https://example.com/callback";
    public static final String ACTION_VERIFY_EMAIL = "VERIFY_EMAIL";
    public static final String ACTION_UPDATE_PASSWORD = "UPDATE_PASSWORD";
    
    // Browser/Client Info
    public static final String BROWSER_FIREFOX = "Firefox";
    public static final String BROWSER_CHROME = "Chrome";
    public static final String OS_LINUX = "Linux";
    
    // Node Identifiers
    public static final String NODE_ID = "node-123";
    public static final String NODE_ID_2 = "node-456";
    
    // Error Messages
    public static final String ERROR_LISTENER_FAILED = "Listener failed";
    public static final String ERROR_PUBLISH_FAILED = "Publish failed";
    public static final String ERROR_TASK_FAILED = "Task failed";
    public static final String ERROR_SERIALIZATION_FAILED = "Serialization failed";
    
    // Broker/Identity Provider
    public static final String BROKER_SESSION_ID = "broker-session-1";
    public static final String BROKER_USER_ID = "broker-user-1";
    
    // Time Units
    public static final TimeUnit TIME_UNIT_SECONDS = TimeUnit.SECONDS;
    public static final TimeUnit TIME_UNIT_MILLISECONDS = TimeUnit.MILLISECONDS;
    
    // Numeric Test Values
    public static final int TEST_NUMERIC_VALUE_1 = 123;
    public static final int TEST_NUMERIC_VALUE_2 = 456;
    public static final int TEST_NUMERIC_VALUE_3 = 999;
    
    // Failure Counts
    public static final int FAILURE_COUNT_0 = 0;
    public static final int FAILURE_COUNT_1 = 1;
    public static final int FAILURE_COUNT_3 = 3;
    public static final int FAILURE_COUNT_5 = 5;
    
    // Test Strings
    public static final String REDIS_OK = "OK";
    public static final String EXISTING_VALUE = "existing";
    public static final String EXISTING_LOCK = "existing-lock";
    
    // Tab IDs
    public static final String TAB_ID_1 = "tab-1";
    public static final String TAB_ID_2 = "tab-2";
    
    // Protocol Names
    public static final String PROTOCOL_OPENID_CONNECT = "openid-connect";
    public static final String PROTOCOL_SAML = "saml";
}
