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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Shared test context to pass data between Cucumber steps.
 */
public class TestContext {
    
    private static TestContext instance;
    private static Properties config;
    
    // Keycloak configuration
    private String keycloakBaseUrl;
    private String adminUsername;
    private String adminPassword;
    
    // Redis connection
    private String redisHost;
    private int redisPort;
    private boolean clusterEnabled;
    
    // Test configuration
    private String defaultTestPassword;
    
    // Standalone Redis
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> redisConnection;
    
    // Cluster Redis
    private RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> redisClusterConnection;
    
    // Unified command interface (works for both modes)
    private Object redisCommands; // Will be either RedisCommands or RedisAdvancedClusterCommands
    
    // Test data storage
    private final Map<String, Object> data = new HashMap<>();
    private Map<String, Object> testData = new HashMap<>();
    
    // Current test entities
    private String currentRealmId;
    private String currentClientId;
    private String currentUserId;
    private String currentSessionId;
    private String currentAuthSessionId;
    private String currentAccessToken;
    
    private TestContext() {
        // Private constructor for singleton
        loadConfiguration();
    }
    
    /**
     * Load configuration from application-test.properties files.
     * First loads common properties from application-test.properties,
     * then loads environment-specific overrides based on test.environment system property.
     * 
     * test.environment values:
     *   - "standalone" (default): loads application-test-standalone.properties
     *   - "docker-cluster": loads application-test-docker-cluster.properties
     */
    private void loadConfiguration() {
        config = new Properties();
        
        // Step 1: Load common properties
        try (InputStream commonInput = getClass().getClassLoader().getResourceAsStream("application-test.properties")) {
            if (commonInput == null) {
                System.err.println("WARNING: application-test.properties not found, using defaults");
                setDefaults();
                return;
            }
            config.load(commonInput);
            System.out.println("Loaded common test properties from application-test.properties");
        } catch (IOException e) {
            System.err.println("Error loading application-test.properties: " + e.getMessage());
            setDefaults();
            return;
        }
        
        // Step 2: Load environment-specific properties (override common ones)
        String testEnvironment = System.getProperty("test.environment", "standalone");
        String envPropertiesFile = "application-test-" + testEnvironment + ".properties";
        
        try (InputStream envInput = getClass().getClassLoader().getResourceAsStream(envPropertiesFile)) {
            if (envInput == null) {
                System.err.println("WARNING: " + envPropertiesFile + " not found, using only common properties");
            } else {
                config.load(envInput);
                System.out.println("Loaded environment-specific properties from " + envPropertiesFile);
            }
        } catch (IOException e) {
            System.err.println("Error loading " + envPropertiesFile + ": " + e.getMessage());
        }
        
        // Step 3: Extract properties into fields
        try {
            // Load Redis configuration
            this.redisHost = config.getProperty("redis.host", "localhost");
            this.redisPort = Integer.parseInt(config.getProperty("redis.port", "16379"));
            this.clusterEnabled = Boolean.parseBoolean(config.getProperty("redis.cluster.enabled", "false"));
            
            // Load Keycloak configuration
            this.keycloakBaseUrl = config.getProperty("keycloak.base.url", "http://localhost:18080");
            this.adminUsername = config.getProperty("keycloak.admin.username", "admin");
            this.adminPassword = config.getProperty("keycloak.admin.password", "admin");
            
            // Load test configuration
            this.defaultTestPassword = config.getProperty("test.user.default.password", "Test123!");
            
            System.out.println("Test environment: " + testEnvironment);
            System.out.println("Redis: " + redisHost + ":" + redisPort + " (cluster=" + clusterEnabled + ")");
            System.out.println("Keycloak: " + keycloakBaseUrl);
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid configuration value: " + e.getMessage());
            setDefaults();
        }
    }
    
    /**
     * Set default values if properties file is missing
     */
    private void setDefaults() {
        this.redisHost = "localhost";
        this.redisPort = 16379;
        this.clusterEnabled = false;
        this.keycloakBaseUrl = "http://localhost:18080";
        this.adminUsername = "admin";
        this.adminPassword = "admin";
        this.defaultTestPassword = "Test123!";
    }
    
    public static TestContext getInstance() {
        if (instance == null) {
            instance = new TestContext();
        }
        return instance;
    }
    
    public void reset() {
        data.clear();
        testData.clear();
        currentRealmId = null;
        currentClientId = null;
        currentUserId = null;
        currentSessionId = null;
        currentAuthSessionId = null;
        currentAccessToken = null;
    }
    
    // Redis connection management
    public void initRedisConnection() {
        if (clusterEnabled) {
            if (redisClusterClient == null) {
                RedisURI redisURI = RedisURI.Builder.redis(redisHost, redisPort).build();
                redisClusterClient = RedisClusterClient.create(redisURI);
                redisClusterConnection = redisClusterClient.connect();
                redisCommands = redisClusterConnection.sync();
            }
        } else {
            if (redisClient == null) {
                redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
                redisConnection = redisClient.connect();
                redisCommands = redisConnection.sync();
            }
        }
    }
    
    public void closeRedisConnection() {
        if (redisClusterConnection != null) {
            redisClusterConnection.close();
            redisClusterConnection = null;
        }
        if (redisClusterClient != null) {
            redisClusterClient.shutdown();
            redisClusterClient = null;
        }
        if (redisConnection != null) {
            redisConnection.close();
            redisConnection = null;
        }
        if (redisClient != null) {
            redisClient.shutdown();
            redisClient = null;
        }
        redisCommands = null;
    }
    
    // Getters and setters
    public String getKeycloakBaseUrl() {
        return keycloakBaseUrl;
    }
    
    public String getDefaultTestPassword() {
        return defaultTestPassword;
    }
    
    /**
     * Get default test realm name from properties
     */
    public String getDefaultTestRealm() {
        return config != null ? config.getProperty("test.realm.name", "test-realm") : "test-realm";
    }
    
    /**
     * Get default test client from properties
     */
    public String getDefaultTestClient() {
        return config != null ? config.getProperty("test.client.default", "test-client") : "test-client";
    }
    
    /**
     * Get test client by number (1, 2, etc.)
     */
    public String getTestClient(int number) {
        return config != null ? config.getProperty("test.client." + number, "client-" + number) : "client-" + number;
    }
    
    /**
     * Get default test user from properties
     */
    public String getDefaultTestUser() {
        return config != null ? config.getProperty("test.user.default", "testuser") : "testuser";
    }
    
    /**
     * Get test user by number (2, 3, etc.)
     */
    public String getTestUser(int number) {
        return config != null ? config.getProperty("test.user." + number, "testuser" + number) : "testuser" + number;
    }
    
    /**
     * Get short timeout for tests (default 5 seconds)
     */
    public int getShortTimeout() {
        return config != null ? Integer.parseInt(config.getProperty("test.timeout.short", "5")) : 5;
    }
    
    /**
     * Get medium timeout for tests (default 6 seconds)
     */
    public int getMediumTimeout() {
        return config != null ? Integer.parseInt(config.getProperty("test.timeout.medium", "6")) : 6;
    }
    
    /**
     * Get long timeout for tests (default 180 seconds)
     */
    public int getLongTimeout() {
        return config != null ? Integer.parseInt(config.getProperty("test.timeout.long", "180")) : 180;
    }
    
    public void setKeycloakBaseUrl(String keycloakBaseUrl) {
        this.keycloakBaseUrl = keycloakBaseUrl;
    }
    
    public String getAdminUsername() {
        return adminUsername;
    }
    
    public String getAdminPassword() {
        return adminPassword;
    }
    
    public String getRedisHost() {
        return redisHost;
    }
    
    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }
    
    public int getRedisPort() {
        return redisPort;
    }
    
    public void setRedisPort(int redisPort) {
        this.redisPort = redisPort;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getRedisCommands() {
        if (redisCommands == null) {
            initRedisConnection();
        }
        return (T) redisCommands;
    }
    
    /**
     * Get Redis commands wrapped in a unified interface that works for both standalone and cluster modes.
     * This method should be used in test steps instead of getRedisCommands() to avoid ClassCastException
     * when running tests against a Redis cluster.
     */
    public RedisTestCommands redis() {
        if (redisCommands == null) {
            initRedisConnection();
        }
        return new RedisTestCommandsImpl(redisCommands, clusterEnabled);
    }
    
    /**
     * Find the actual user session ID from Redis, filtering out metadata keys.
     * Keycloak creates various metadata keys (realm:, user:, client:, _ver:) that need to be excluded.
     * 
     * @return actual session UUID or null if not found
     */
    public String findActualSessionId() {
        RedisTestCommands redis = redis();
        java.util.List<String> sessionKeys = redis.keys(RedisKeyConstants.USER_SESSION_PATTERN);
        
        for (String key : sessionKeys) {
            String sessionId = key.replace(RedisKeyConstants.USER_SESSION_PREFIX, "");
            if (isActualSessionKey(sessionId)) {
                return sessionId;
            }
        }
        return null;
    }
    
    /**
     * Check if a session ID represents an actual session (not metadata).
     * Filters out version keys (_ver:) and metadata keys (realm:, user:, client:).
     */
    private boolean isActualSessionKey(String sessionId) {
        return !sessionId.contains(":_ver") && 
               !sessionId.contains(":user:") && 
               !sessionId.contains(":realm:") &&
               !sessionId.contains(":client:") &&
               !sessionId.startsWith("realm:") &&
               !sessionId.startsWith("user:") &&
               !sessionId.startsWith("client:");
    }
    
    public boolean isClusterEnabled() {
        return clusterEnabled;
    }
    
    public void setClusterEnabled(boolean clusterEnabled) {
        this.clusterEnabled = clusterEnabled;
    }
    
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) data.get(key);
    }
    
    public boolean contains(String key) {
        return data.containsKey(key);
    }
    
    public String getCurrentRealmId() {
        return currentRealmId;
    }
    
    public void setCurrentRealmId(String currentRealmId) {
        this.currentRealmId = currentRealmId;
    }
    
    public String getCurrentClientId() {
        return currentClientId;
    }
    
    public void setCurrentClientId(String currentClientId) {
        this.currentClientId = currentClientId;
    }
    
    public String getCurrentUserId() {
        return currentUserId;
    }
    
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }
    
    public String getCurrentSessionId() {
        return currentSessionId;
    }
    
    public void setCurrentSessionId(String currentSessionId) {
        this.currentSessionId = currentSessionId;
    }
    
    public String getCurrentAuthSessionId() {
        return currentAuthSessionId;
    }
    
    public void setCurrentAuthSessionId(String currentAuthSessionId) {
        this.currentAuthSessionId = currentAuthSessionId;
    }
    
    public String getCurrentAccessToken() {
        return currentAccessToken;
    }
    
    public void setCurrentAccessToken(String currentAccessToken) {
        this.currentAccessToken = currentAccessToken;
    }
    
    // Keycloak Admin API integration
    private String adminToken;
    private long adminTokenExpiry;
    
    public String getAdminToken() {
        // Refresh token if expired or not present
        if (adminToken == null || System.currentTimeMillis() > adminTokenExpiry) {
            refreshAdminToken();
        }
        return adminToken;
    }
    
    private void refreshAdminToken() {
        try {
            Response response = RestAssured
                .given()
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("username", adminUsername)
                    .formParam("password", adminPassword)
                    .formParam("grant_type", "password")
                    .formParam("client_id", "admin-cli")
                .when()
                    .post(keycloakBaseUrl + "/realms/master/protocol/openid-connect/token");
            
            if (response.statusCode() == 200) {
                adminToken = response.jsonPath().getString("access_token");
                int expiresIn = response.jsonPath().getInt("expires_in");
                adminTokenExpiry = System.currentTimeMillis() + (expiresIn * 1000) - 10000; // 10s buffer
            } else {
                System.err.println("Failed to get admin token: " + response.statusCode() + " - " + response.body().asString());
                adminToken = null;
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not get admin token - " + e.getMessage());
            adminToken = null;
        }
    }
    
    // User login via Keycloak endpoints
    public String loginUser(String realm, String username, String password, String clientId) {
        try {
            Response response = RestAssured
                .given()
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("username", username)
                    .formParam("password", password)
                    .formParam("grant_type", "password")
                    .formParam("client_id", clientId)
                .when()
                    .post(keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token");
            
            if (response.statusCode() == 200) {
                String accessToken = response.jsonPath().getString("access_token");
                String sessionState = response.jsonPath().getString("session_state");
                String refreshToken = response.jsonPath().getString("refresh_token");
                
                this.currentAccessToken = accessToken;
                this.currentSessionId = sessionState;
                put("access_token", accessToken);
                put("refresh_token", refreshToken);
                put("session_state", sessionState);
                put("login_response", response);
                
                return sessionState;
            } else {
                System.err.println("Login failed: " + response.statusCode() + " - " + response.body().asString());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            return null;
        }
    }
    
    // User logout via Keycloak endpoints
    public boolean logoutUser(String realm, String refreshToken) {
        try {
            Response response = RestAssured
                .given()
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("client_id", currentClientId != null ? currentClientId : "test-client")
                    .formParam("refresh_token", refreshToken)
                .when()
                    .post(keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/logout");
            
            return response.statusCode() == 204 || response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Logout error: " + e.getMessage());
            return false;
        }
    }
    
    public void clear() {
        data.clear();
        testData.clear();
    }
}
