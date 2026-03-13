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

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for Keycloak Admin API operations.
 */
public class KeycloakAdminHelper {
    
    private final TestContext context;
    private final String baseUrl;
    
    public KeycloakAdminHelper(TestContext context) {
        this.context = context;
        this.baseUrl = context.getKeycloakBaseUrl();
    }
    
    /**
     * Create a realm via Admin API
     */
    public boolean createRealm(String realmName) {
        Map<String, Object> realmConfig = new HashMap<>();
        realmConfig.put("realm", realmName);
        realmConfig.put("enabled", true);
        realmConfig.put("displayName", realmName);
        realmConfig.put("bruteForceProtected", false);
        
        try {
            Response response = RestAssured
                .given()
                    .header("Authorization", "Bearer " + context.getAdminToken())
                    .contentType(ContentType.JSON)
                    .body(realmConfig)
                .when()
                    .post(baseUrl + "/admin/realms");
            
            int statusCode = response.statusCode();
            if (statusCode != 201) {
                System.err.println("Failed to create realm '" + realmName + "': HTTP " + statusCode);
                System.err.println("Response: " + response.body().asString());
                
                if (statusCode == 401) {
                    System.err.println("\n❌ ADMIN ROLE ISSUE DETECTED!");
                    System.err.println("The admin user lacks necessary roles to create realms.");
                    System.err.println("\n🔧 QUICK FIX:");
                    System.err.println("   cd model/redis/docker");
                    System.err.println("   ./fix-admin-roles.sh");
                    System.err.println("\nOr see: model/redis/ADMIN_API_ISSUE.md for manual steps\n");
                }
            }
            return statusCode == 201;
        } catch (Exception e) {
            System.err.println("Failed to create realm '" + realmName + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Create a realm with brute force protection enabled
     */
    public boolean createRealmWithBruteForceProtection(String realmName) {
        Map<String, Object> realmConfig = new HashMap<>();
        realmConfig.put("realm", realmName);
        realmConfig.put("enabled", true);
        realmConfig.put("displayName", realmName);
        realmConfig.put("bruteForceProtected", true);
        realmConfig.put("failureFactor", 3);
        realmConfig.put("maxFailureWaitSeconds", 3600);
        realmConfig.put("minimumQuickLoginWaitSeconds", 60);
        realmConfig.put("waitIncrementSeconds", 60);
        
        try {
            Response response = RestAssured
                .given()
                    .header("Authorization", "Bearer " + context.getAdminToken())
                    .contentType(ContentType.JSON)
                    .body(realmConfig)
                .when()
                    .post(baseUrl + "/admin/realms");
            
            return response.statusCode() == 201;
        } catch (Exception e) {
            System.err.println("Failed to create realm with brute force: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete a realm via Admin API
     */
    public boolean deleteRealm(String realmName) {
        try {
            Response response = RestAssured
                .given()
                    .header("Authorization", "Bearer " + context.getAdminToken())
                .when()
                    .delete(baseUrl + "/admin/realms/" + realmName);
            
            return response.statusCode() == 204;
        } catch (Exception e) {
            System.err.println("Failed to delete realm: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create a user in a realm
     */
    public String createUser(String realmName, String username, String password) {
        Map<String, Object> userConfig = new HashMap<>();
        userConfig.put("username", username);
        userConfig.put("enabled", true);
        userConfig.put("emailVerified", true);
        
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", password);
        credentials.put("temporary", false);
        
        userConfig.put("credentials", new Object[]{credentials});
        
        try {
            Response response = RestAssured
                .given()
                    .header("Authorization", "Bearer " + context.getAdminToken())
                    .contentType(ContentType.JSON)
                    .body(userConfig)
                .when()
                    .post(baseUrl + "/admin/realms/" + realmName + "/users");
            
            if (response.statusCode() == 201) {
                String location = response.header("Location");
                if (location != null) {
                    return location.substring(location.lastIndexOf('/') + 1);
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Failed to create user: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a client in a realm
     */
    public String createClient(String realmName, String clientId) {
        Map<String, Object> clientConfig = new HashMap<>();
        clientConfig.put("clientId", clientId);
        clientConfig.put("enabled", true);
        clientConfig.put("publicClient", true);
        clientConfig.put("directAccessGrantsEnabled", true);
        clientConfig.put("standardFlowEnabled", true);
        clientConfig.put("implicitFlowEnabled", false);
        clientConfig.put("serviceAccountsEnabled", false);
        
        try {
            Response response = RestAssured
                .given()
                    .header("Authorization", "Bearer " + context.getAdminToken())
                    .contentType(ContentType.JSON)
                    .body(clientConfig)
                .when()
                    .post(baseUrl + "/admin/realms/" + realmName + "/clients");
            
            if (response.statusCode() == 201) {
                String location = response.header("Location");
                if (location != null) {
                    return location.substring(location.lastIndexOf('/') + 1);
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Failed to create client: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if realm exists
     */
    public boolean realmExists(String realmName) {
        try {
            Response response = RestAssured
                .given()
                    .header("Authorization", "Bearer " + context.getAdminToken())
                .when()
                    .get(baseUrl + "/admin/realms/" + realmName);
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get session count for a realm
     */
    public int getSessionCount(String realmName) {
        try {
            Response response = RestAssured
                .given()
                    .header("Authorization", "Bearer " + context.getAdminToken())
                .when()
                    .get(baseUrl + "/admin/realms/" + realmName + "/client-session-stats");
            
            if (response.statusCode() == 200) {
                // This is an approximation - would need to sum up active sessions
                return response.jsonPath().getList("$").size();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
