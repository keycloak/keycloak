/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.representations;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OIDCClientRepresentationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void testDefaultConstructor() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        
        assertThat(rep.getClientId(), nullValue());
        assertThat(rep.getLoginFlows(), notNullValue());
        assertThat(rep.getLoginFlows(), empty());
        assertThat(rep.getWebOrigins(), notNullValue());
        assertThat(rep.getWebOrigins(), empty());
        assertThat(rep.getServiceAccountRoles(), notNullValue());
        assertThat(rep.getServiceAccountRoles(), empty());
        assertThat(rep.getAuth(), nullValue());
    }

    @Test
    void testClientIdConstructor() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation("my-client");
        
        assertThat(rep.getClientId(), is("my-client"));
    }

    @Test
    void testProtocolReturnsOIDC() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        
        assertThat(rep.getProtocol(), is("openid-connect"));
        assertThat(rep.getProtocol(), is(OIDCClientRepresentation.PROTOCOL));
    }

    @Test
    void testLoginFlowsEnum() {
        // Verify all expected flows exist
        assertThat(OIDCClientRepresentation.Flow.values().length, is(7));
        assertThat(OIDCClientRepresentation.Flow.STANDARD.name(), is("STANDARD"));
        assertThat(OIDCClientRepresentation.Flow.IMPLICIT.name(), is("IMPLICIT"));
        assertThat(OIDCClientRepresentation.Flow.DIRECT_GRANT.name(), is("DIRECT_GRANT"));
        assertThat(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT.name(), is("SERVICE_ACCOUNT"));
        assertThat(OIDCClientRepresentation.Flow.TOKEN_EXCHANGE.name(), is("TOKEN_EXCHANGE"));
        assertThat(OIDCClientRepresentation.Flow.DEVICE.name(), is("DEVICE"));
        assertThat(OIDCClientRepresentation.Flow.CIBA.name(), is("CIBA"));
    }

    @Test
    void testSetAndGetLoginFlows() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        Set<OIDCClientRepresentation.Flow> flows = Set.of(
            OIDCClientRepresentation.Flow.STANDARD,
            OIDCClientRepresentation.Flow.SERVICE_ACCOUNT
        );
        
        rep.setLoginFlows(flows);
        
        assertThat(rep.getLoginFlows(), containsInAnyOrder(
            OIDCClientRepresentation.Flow.STANDARD,
            OIDCClientRepresentation.Flow.SERVICE_ACCOUNT
        ));
    }

    @Test
    void testSetAndGetAuth() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-secret");
        auth.setSecret("super-secret");
        auth.setCertificate("cert-data");
        
        rep.setAuth(auth);
        
        assertThat(rep.getAuth(), notNullValue());
        assertThat(rep.getAuth().getMethod(), is("client-secret"));
        assertThat(rep.getAuth().getSecret(), is("super-secret"));
        assertThat(rep.getAuth().getCertificate(), is("cert-data"));
    }

    @Test
    void testSetAndGetWebOrigins() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        Set<String> origins = Set.of("http://localhost:3000", "https://example.com");
        
        rep.setWebOrigins(origins);
        
        assertThat(rep.getWebOrigins(), containsInAnyOrder("http://localhost:3000", "https://example.com"));
    }

    @Test
    void testSetAndGetServiceAccountRoles() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        Set<String> roles = Set.of("role1", "role2", "admin");
        
        rep.setServiceAccountRoles(roles);
        
        assertThat(rep.getServiceAccountRoles(), containsInAnyOrder("role1", "role2", "admin"));
    }

    @Test
    void testEqualsAndHashCode() {
        OIDCClientRepresentation rep1 = createFullRepresentation();
        OIDCClientRepresentation rep2 = createFullRepresentation();
        
        assertThat(rep1, equalTo(rep2));
        assertThat(rep1.hashCode(), is(rep2.hashCode()));
    }

    @Test
    void testEqualsReturnsFalseForDifferentClientId() {
        OIDCClientRepresentation rep1 = createFullRepresentation();
        OIDCClientRepresentation rep2 = createFullRepresentation();
        rep2.setClientId("different-client");
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentLoginFlows() {
        OIDCClientRepresentation rep1 = createFullRepresentation();
        OIDCClientRepresentation rep2 = createFullRepresentation();
        rep2.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.IMPLICIT));
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentAuth() {
        OIDCClientRepresentation rep1 = createFullRepresentation();
        OIDCClientRepresentation rep2 = createFullRepresentation();
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("different-method");
        rep2.setAuth(auth);
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentWebOrigins() {
        OIDCClientRepresentation rep1 = createFullRepresentation();
        OIDCClientRepresentation rep2 = createFullRepresentation();
        rep2.setWebOrigins(Set.of("http://different.com"));
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentServiceAccountRoles() {
        OIDCClientRepresentation rep1 = createFullRepresentation();
        OIDCClientRepresentation rep2 = createFullRepresentation();
        rep2.setServiceAccountRoles(Set.of("different-role"));
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testAuthEqualsAndHashCode() {
        OIDCClientRepresentation.Auth auth1 = new OIDCClientRepresentation.Auth();
        auth1.setMethod("client-secret");
        auth1.setSecret("secret");
        auth1.setCertificate("cert");
        
        OIDCClientRepresentation.Auth auth2 = new OIDCClientRepresentation.Auth();
        auth2.setMethod("client-secret");
        auth2.setSecret("secret");
        auth2.setCertificate("cert");
        
        assertThat(auth1, equalTo(auth2));
        assertThat(auth1.hashCode(), is(auth2.hashCode()));
    }

    @Test
    void testAuthEqualsReturnsFalseForDifferentMethod() {
        OIDCClientRepresentation.Auth auth1 = new OIDCClientRepresentation.Auth();
        auth1.setMethod("client-secret");
        
        OIDCClientRepresentation.Auth auth2 = new OIDCClientRepresentation.Auth();
        auth2.setMethod("client-jwt");
        
        assertThat(auth1, not(equalTo(auth2)));
    }

    @Test
    void testAuthEqualsReturnsFalseForDifferentSecret() {
        OIDCClientRepresentation.Auth auth1 = new OIDCClientRepresentation.Auth();
        auth1.setSecret("secret1");
        
        OIDCClientRepresentation.Auth auth2 = new OIDCClientRepresentation.Auth();
        auth2.setSecret("secret2");
        
        assertThat(auth1, not(equalTo(auth2)));
    }

    @Test
    void testAuthEqualsReturnsFalseForDifferentCertificate() {
        OIDCClientRepresentation.Auth auth1 = new OIDCClientRepresentation.Auth();
        auth1.setCertificate("cert1");
        
        OIDCClientRepresentation.Auth auth2 = new OIDCClientRepresentation.Auth();
        auth2.setCertificate("cert2");
        
        assertThat(auth1, not(equalTo(auth2)));
    }

    @Test
    void testJsonSerialization() throws JsonProcessingException {
        OIDCClientRepresentation rep = createFullRepresentation();
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        assertThat(node.get("protocol").asText(), is("openid-connect"));
        assertThat(node.get("clientId").asText(), is("test-client"));
        assertThat(node.get("displayName").asText(), is("Test Client"));
        assertThat(node.get("enabled").asBoolean(), is(true));
        assertThat(node.get("auth").get("method").asText(), is("client-secret"));
        assertTrue(node.get("loginFlows").isArray());
        assertTrue(node.get("webOrigins").isArray());
    }

    @Test
    void testJsonDeserialization() throws JsonProcessingException {
        String json = """
            {
                "protocol": "openid-connect",
                "clientId": "my-client",
                "displayName": "My Client",
                "enabled": true,
                "appUrl": "http://localhost:8080",
                "redirectUris": ["http://localhost:8080/callback"],
                "loginFlows": ["STANDARD", "SERVICE_ACCOUNT"],
                "auth": {
                    "method": "client-secret",
                    "secret": "my-secret"
                },
                "webOrigins": ["http://localhost:3000"],
                "serviceAccountRoles": ["admin", "user"]
            }
            """;
        
        OIDCClientRepresentation rep = MAPPER.readValue(json, OIDCClientRepresentation.class);
        
        assertThat(rep.getClientId(), is("my-client"));
        assertThat(rep.getDisplayName(), is("My Client"));
        assertThat(rep.getEnabled(), is(true));
        assertThat(rep.getAppUrl(), is("http://localhost:8080"));
        assertThat(rep.getRedirectUris(), hasSize(1));
        assertThat(rep.getLoginFlows(), containsInAnyOrder(
            OIDCClientRepresentation.Flow.STANDARD,
            OIDCClientRepresentation.Flow.SERVICE_ACCOUNT
        ));
        assertThat(rep.getAuth().getMethod(), is("client-secret"));
        assertThat(rep.getAuth().getSecret(), is("my-secret"));
        assertThat(rep.getWebOrigins(), hasSize(1));
        assertThat(rep.getServiceAccountRoles(), containsInAnyOrder("admin", "user"));
    }

    @Test
    void testJsonSerializationOmitsEmptyCollections() throws JsonProcessingException {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("minimal-client");
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        // Empty collections should not be included due to @JsonInclude(JsonInclude.Include.NON_EMPTY)
        assertFalse(node.has("loginFlows") && !node.get("loginFlows").isEmpty());
        assertFalse(node.has("webOrigins") && !node.get("webOrigins").isEmpty());
        assertFalse(node.has("serviceAccountRoles") && !node.get("serviceAccountRoles").isEmpty());
    }

    @Test
    void testJsonSerializationOmitsNullAuth() throws JsonProcessingException {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("client-without-auth");
        rep.setAuth(null);
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        // Null auth should not be serialized
        assertThat(node.has("auth"), is(false));
    }

    @Test
    void testPolymorphicDeserialization() throws JsonProcessingException {
        String json = """
            {
                "protocol": "openid-connect",
                "clientId": "polymorphic-client"
            }
            """;
        
        // Should deserialize to OIDCClientRepresentation based on protocol discriminator
        OIDCClientRepresentation rep = MAPPER.readValue(json, OIDCClientRepresentation.class);
        assertThat(rep.getClientId(), is("polymorphic-client"));
        assertThat(rep.getProtocol(), is("openid-connect"));
    }

    private OIDCClientRepresentation createFullRepresentation() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-client");
        rep.setDisplayName("Test Client");
        rep.setDescription("A test client");
        rep.setEnabled(true);
        rep.setAppUrl("http://localhost:8080");
        rep.setRedirectUris(Set.of("http://localhost:8080/callback"));
        rep.setRoles(Set.of("role1", "role2"));
        rep.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD));
        
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-secret");
        auth.setSecret("secret");
        rep.setAuth(auth);
        
        rep.setWebOrigins(Set.of("http://localhost:3000"));
        rep.setServiceAccountRoles(Set.of("sa-role1"));
        
        return rep;
    }
}
