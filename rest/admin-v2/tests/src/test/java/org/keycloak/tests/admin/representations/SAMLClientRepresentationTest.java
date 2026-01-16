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
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class SAMLClientRepresentationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void testProtocolConstant() {
        assertThat(SAMLClientRepresentation.PROTOCOL, is("saml"));
    }

    @Test
    void testGetProtocol() {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        
        assertThat(rep.getProtocol(), is("saml"));
    }

    @Test
    void testDefaultConstructor() {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        
        assertThat(rep.getClientId(), nullValue());
        assertThat(rep.getDisplayName(), nullValue());
        assertThat(rep.getDescription(), nullValue());
        assertThat(rep.getEnabled(), nullValue());
        assertThat(rep.getAppUrl(), nullValue());
    }

    @Test
    void testInheritsBaseClientFields() {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setClientId("saml-client");
        rep.setDisplayName("SAML Client");
        rep.setDescription("A SAML client");
        rep.setEnabled(true);
        rep.setAppUrl("http://localhost:8080/saml");
        rep.setRedirectUris(Set.of("http://localhost:8080/saml/callback"));
        rep.setRoles(Set.of("role1", "role2"));
        
        assertThat(rep.getClientId(), is("saml-client"));
        assertThat(rep.getDisplayName(), is("SAML Client"));
        assertThat(rep.getDescription(), is("A SAML client"));
        assertThat(rep.getEnabled(), is(true));
        assertThat(rep.getAppUrl(), is("http://localhost:8080/saml"));
        assertThat(rep.getRedirectUris().size(), is(1));
        assertThat(rep.getRoles().size(), is(2));
    }

    @Test
    void testJsonSerialization() throws JsonProcessingException {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setClientId("saml-client");
        rep.setDisplayName("SAML Client");
        rep.setEnabled(true);
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        assertThat(node.get("protocol").asText(), is("saml"));
        assertThat(node.get("clientId").asText(), is("saml-client"));
        assertThat(node.get("displayName").asText(), is("SAML Client"));
        assertThat(node.get("enabled").asBoolean(), is(true));
    }

    @Test
    void testJsonDeserialization() throws JsonProcessingException {
        String json = """
            {
                "protocol": "saml",
                "clientId": "my-saml-client",
                "displayName": "My SAML Client",
                "description": "A SAML-based client",
                "enabled": true,
                "appUrl": "http://localhost:8080/saml",
                "redirectUris": ["http://localhost:8080/saml/callback"],
                "roles": ["admin", "user"]
            }
            """;
        
        SAMLClientRepresentation rep = MAPPER.readValue(json, SAMLClientRepresentation.class);
        
        assertThat(rep.getProtocol(), is("saml"));
        assertThat(rep.getClientId(), is("my-saml-client"));
        assertThat(rep.getDisplayName(), is("My SAML Client"));
        assertThat(rep.getDescription(), is("A SAML-based client"));
        assertThat(rep.getEnabled(), is(true));
        assertThat(rep.getAppUrl(), is("http://localhost:8080/saml"));
        assertThat(rep.getRedirectUris().size(), is(1));
        assertThat(rep.getRoles().size(), is(2));
    }

    @Test
    void testPolymorphicDeserializationFromBaseClass() throws JsonProcessingException {
        String json = """
            {
                "protocol": "saml",
                "clientId": "polymorphic-saml"
            }
            """;
        
        BaseClientRepresentation rep = MAPPER.readValue(json, BaseClientRepresentation.class);
        
        assertThat(rep, instanceOf(SAMLClientRepresentation.class));
        assertThat(rep.getClientId(), is("polymorphic-saml"));
        assertThat(rep.getProtocol(), is("saml"));
    }

    @Test
    void testJsonSerializationOmitsNullFields() throws JsonProcessingException {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setClientId("minimal-saml");
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        // Null fields should be omitted
        assertThat(node.has("displayName"), is(false));
        assertThat(node.has("description"), is(false));
        assertThat(node.has("enabled"), is(false));
        assertThat(node.has("appUrl"), is(false));
    }

    @Test
    void testJsonSerializationOmitsEmptyCollections() throws JsonProcessingException {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setClientId("empty-collections");
        rep.setRedirectUris(Set.of());
        rep.setRoles(Set.of());
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        // Empty collections should be omitted
        assertThat(node.has("redirectUris"), is(false));
        assertThat(node.has("roles"), is(false));
    }

    @Test
    void testAdditionalFieldsSupport() throws JsonProcessingException {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setClientId("saml-with-extras");
        rep.setAdditionalField("samlSpecificField", "value");
        rep.setAdditionalField("anotherField", 123);
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        assertThat(node.get("samlSpecificField").asText(), is("value"));
        assertThat(node.get("anotherField").asInt(), is(123));
    }

    @Test
    void testDeserializationWithUnknownFields() throws JsonProcessingException {
        String json = """
            {
                "protocol": "saml",
                "clientId": "unknown-fields-client",
                "unknownField1": "value1",
                "unknownField2": 42
            }
            """;
        
        SAMLClientRepresentation rep = MAPPER.readValue(json, SAMLClientRepresentation.class);
        
        assertThat(rep.getClientId(), is("unknown-fields-client"));
        // Unknown fields should be captured in additionalFields
        assertThat(rep.getAdditionalFields().get("unknownField1"), is("value1"));
        assertThat(rep.getAdditionalFields().get("unknownField2"), is(42));
    }
}
