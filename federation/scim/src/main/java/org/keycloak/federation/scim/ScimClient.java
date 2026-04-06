/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.federation.scim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.component.ComponentModel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * SCIM 2.0 HTTP client using native Java HTTP client (no 3rd party SDK).
 * Handles communication with SCIM service providers.
 */
public class ScimClient {

    private final String baseUrl;
    private final String authType;
    private final String authToken;
    private final String username;
    private final String password;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ScimClient(ComponentModel model) {
        this.baseUrl = model.get("scimBaseUrl");
        this.authType = model.get("scimAuthType");
        this.authToken = model.get("scimAuthToken");
        this.username = model.get("scimUsername");
        this.password = model.get("scimPassword");
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper();
    }

    private HttpRequest.Builder addAuth(HttpRequest.Builder builder) {
        switch (authType.toLowerCase()) {
            case "basic":
                String basicAuth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                return builder.header("Authorization", "Basic " + basicAuth);
            case "bearer":
            case "oauth":
                return builder.header("Authorization", "Bearer " + authToken);
            default:
                return builder;
        }
    }

    public ScimUser getUser(String id) {
        try {
            HttpRequest request = addAuth(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/Users/" + id))
                    .header("Accept", "application/scim+json")
                    .GET())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseUser(objectMapper.readTree(response.body()));
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get user from SCIM", e);
        }
    }

    public ScimUser getUserByUsername(String username) {
        List<ScimUser> users = searchUsers("userName eq \"" + username + "\"", 0, 1);
        return users.isEmpty() ? null : users.get(0);
    }

    public ScimUser getUserByEmail(String email) {
        List<ScimUser> users = searchUsers("emails eq \"" + email + "\"", 0, 1);
        return users.isEmpty() ? null : users.get(0);
    }

    public List<ScimUser> searchUsers(String filter, Integer startIndex, Integer count) {
        try {
            String url = baseUrl + "/Users?filter=" + encode(filter);
            if (startIndex != null) url += "&startIndex=" + startIndex;
            if (count != null) url += "&count=" + count;

            HttpRequest request = addAuth(HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/scim+json")
                    .GET())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseUserList(objectMapper.readTree(response.body()));
            }
            return new ArrayList<>();
        } catch (Exception e) {
            throw new RuntimeException("Failed to search users from SCIM", e);
        }
    }

    public int getUsersCount() {
        try {
            HttpRequest request = addAuth(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/Users"))
                    .header("Accept", "application/scim+json")
                    .GET())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("totalResults").asInt();
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get users count from SCIM", e);
        }
    }

    public List<ScimUser> searchUsersByAttribute(String attrName, String attrValue, Integer startIndex, Integer maxResults) {
        // SCIM doesn't support custom attributes directly, filter by userName or email
        if ("username".equalsIgnoreCase(attrName) || "userName".equalsIgnoreCase(attrName)) {
            return searchUsers("userName eq \"" + attrValue + "\"", startIndex, maxResults);
        } else if ("email".equalsIgnoreCase(attrName)) {
            return searchUsers("emails eq \"" + attrValue + "\"", startIndex, maxResults);
        }
        return new ArrayList<>();
    }

    public ScimUser createUser(ScimUser user) {
        try {
            ObjectNode userJson = toJson(user);
            HttpRequest request = addAuth(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/Users"))
                    .header("Content-Type", "application/scim+json")
                    .header("Accept", "application/scim+json")
                    .POST(HttpRequest.BodyPublishers.ofString(userJson.toString())))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                return parseUser(objectMapper.readTree(response.body()));
            }
            throw new RuntimeException("Failed to create user: " + response.body());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user in SCIM", e);
        }
    }

    public ScimUser updateUser(String id, ScimUser user) {
        try {
            ObjectNode userJson = toJson(user);
            HttpRequest request = addAuth(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/Users/" + id))
                    .header("Content-Type", "application/scim+json")
                    .header("Accept", "application/scim+json")
                    .method("PUT", HttpRequest.BodyPublishers.ofString(userJson.toString())))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseUser(objectMapper.readTree(response.body()));
            }
            throw new RuntimeException("Failed to update user: " + response.body());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user in SCIM", e);
        }
    }

    public boolean deleteUser(String id) {
        try {
            HttpRequest request = addAuth(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/Users/" + id))
                    .DELETE())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 204;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete user in SCIM", e);
        }
    }

    private ScimUser parseUser(JsonNode node) {
        ScimUser user = new ScimUser();
        user.setId(node.path("id").asText());
        user.setUserName(node.path("userName").asText());
        user.setEmail(getPrimaryEmail(node.path("emails")));
        user.setGivenName(node.path("name").path("givenName").asText());
        user.setFamilyName(node.path("name").path("familyName").asText());
        user.setActive(node.path("active").asBoolean(true));
        return user;
    }

    private List<ScimUser> parseUserList(JsonNode root) {
        List<ScimUser> users = new ArrayList<>();
        JsonNode resources = root.path("Resources");
        if (resources.isArray()) {
            for (JsonNode node : resources) {
                users.add(parseUser(node));
            }
        }
        return users;
    }

    private String getPrimaryEmail(JsonNode emailsNode) {
        if (emailsNode.isArray()) {
            for (JsonNode email : emailsNode) {
                if (email.path("primary").asBoolean(false) || email.path("type").asText().equals("work")) {
                    return email.path("value").asText();
                }
            }
            if (emailsNode.size() > 0) {
                return emailsNode.get(0).path("value").asText();
            }
        }
        return null;
    }

    private ObjectNode toJson(ScimUser user) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("userName", user.getUserName());
        node.put("active", user.isActive());
        
        ObjectNode nameNode = objectMapper.createObjectNode();
        nameNode.put("givenName", user.getGivenName());
        nameNode.put("familyName", user.getFamilyName());
        node.set("name", nameNode);
        
        if (user.getEmail() != null) {
            ArrayNode emails = objectMapper.createArrayNode();
            ObjectNode emailNode = objectMapper.createObjectNode();
            emailNode.put("value", user.getEmail());
            emailNode.put("type", "work");
            emailNode.put("primary", true);
            emails.add(emailNode);
            node.set("emails", emails);
        }
        
        return node;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
