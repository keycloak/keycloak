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

import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClient;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

class BaseClientRepresentationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testSetAndGetClientId() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("my-client-id");
        
        assertThat(rep.getClientId(), is("my-client-id"));
    }

    @Test
    void testSetAndGetDisplayName() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setDisplayName("My Display Name");
        
        assertThat(rep.getDisplayName(), is("My Display Name"));
    }

    @Test
    void testSetAndGetDescription() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setDescription("My Description");
        
        assertThat(rep.getDescription(), is("My Description"));
    }

    @Test
    void testSetAndGetEnabled() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        
        rep.setEnabled(true);
        assertThat(rep.getEnabled(), is(true));
        
        rep.setEnabled(false);
        assertThat(rep.getEnabled(), is(false));
        
        rep.setEnabled(null);
        assertThat(rep.getEnabled(), nullValue());
    }

    @Test
    void testSetAndGetAppUrl() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setAppUrl("http://localhost:8080");
        
        assertThat(rep.getAppUrl(), is("http://localhost:8080"));
    }

    @Test
    void testSetAndGetRedirectUris() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        Set<String> uris = Set.of("http://localhost:8080/callback", "http://localhost:8081/callback");
        
        rep.setRedirectUris(uris);
        
        assertThat(rep.getRedirectUris(), containsInAnyOrder(
            "http://localhost:8080/callback", 
            "http://localhost:8081/callback"
        ));
    }

    @Test
    void testSetAndGetRoles() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        Set<String> roles = Set.of("admin", "user", "guest");
        
        rep.setRoles(roles);
        
        assertThat(rep.getRoles(), containsInAnyOrder("admin", "user", "guest"));
    }

    @Test
    void testAdditionalFields() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        
        rep.setAdditionalField("customField1", "value1");
        rep.setAdditionalField("customField2", 42);
        rep.setAdditionalField("customField3", true);
        
        Map<String, Object> fields = rep.getAdditionalFields();
        assertThat(fields.get("customField1"), is("value1"));
        assertThat(fields.get("customField2"), is(42));
        assertThat(fields.get("customField3"), is(true));
    }

    @Test
    void testSetAdditionalFields() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        Map<String, Object> fields = Map.of("field1", "value1", "field2", "value2");
        
        rep.setAdditionalFields(fields);
        
        assertThat(rep.getAdditionalFields(), is(fields));
    }

    @Test
    void testEqualsWithSameValues() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        
        assertThat(rep1, equalTo(rep2));
    }

    @Test
    void testEqualsReturnsFalseForDifferentClientId() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        rep2.setClientId("different-id");
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentDisplayName() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        rep2.setDisplayName("Different Name");
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentDescription() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        rep2.setDescription("Different Description");
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentEnabled() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        rep2.setEnabled(false);
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentAppUrl() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        rep2.setAppUrl("http://different.com");
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentRedirectUris() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        rep2.setRedirectUris(Set.of("http://different.com/callback"));
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentRoles() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        rep2.setRoles(Set.of("different-role"));
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testEqualsReturnsFalseForDifferentAdditionalFields() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        rep2.setAdditionalField("extra", "value");
        
        assertThat(rep1, not(equalTo(rep2)));
    }

    @Test
    void testHashCodeConsistency() {
        OIDCClientRepresentation rep1 = createRepWithAllFields();
        OIDCClientRepresentation rep2 = createRepWithAllFields();
        
        assertThat(rep1.hashCode(), is(rep2.hashCode()));
    }

    // Validation Tests

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testClientIdValidation_BlankNotAllowedInCreateGroup(String clientId) {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId(clientId);
        
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep, CreateClient.class);
        
        assertThat(violations, hasSize(1));
        assertThat(violations.iterator().next().getPropertyPath().toString(), is("clientId"));
    }

    @Test
    void testClientIdValidation_ValidClientIdPassesCreateGroup() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("valid-client-id");
        
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep, CreateClient.class);
        
        assertThat(violations, empty());
    }

    @Test
    void testClientIdValidation_BlankAllowedInDefaultGroup() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId(null);
        
        // Default group should not trigger @NotBlank on clientId
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep);
        
        // No violation for clientId in default group
        boolean hasClientIdViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("clientId"));
        assertThat(hasClientIdViolation, is(false));
    }

    @Test
    void testAppUrlValidation_InvalidUrl() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("valid-client");
        rep.setAppUrl("not-a-valid-url");
        
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep);
        
        assertThat(violations, hasSize(1));
        assertThat(violations.iterator().next().getPropertyPath().toString(), is("appUrl"));
    }

    @Test
    void testAppUrlValidation_ValidUrl() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("valid-client");
        rep.setAppUrl("http://localhost:8080");
        
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep);
        
        assertThat(violations, empty());
    }

    @Test
    void testRedirectUrisValidation_InvalidUrl() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("valid-client");
        rep.setRedirectUris(Set.of("not-a-valid-url"));
        
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep);
        
        assertThat(violations, hasSize(greaterThan(0)));
    }

    @Test
    void testRedirectUrisValidation_BlankNotAllowed() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("valid-client");
        rep.setRedirectUris(Set.of(""));
        
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep);
        
        assertThat(violations, hasSize(greaterThan(0)));
    }

    @Test
    void testRedirectUrisValidation_ValidUrls() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("valid-client");
        rep.setRedirectUris(Set.of("http://localhost:8080/callback", "https://example.com/oauth"));
        
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep);
        
        assertThat(violations, empty());
    }

    @Test
    void testRolesValidation_BlankNotAllowed() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("valid-client");
        rep.setRoles(Set.of(""));
        
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep);
        
        assertThat(violations, hasSize(1));
    }

    @Test
    void testCreateClientDefaultGroupSequence() {
        // CreateClientDefault should validate both CreateClient and Default groups
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId(null); // Should fail CreateClient
        rep.setAppUrl("invalid"); // Should fail Default
        
        Set<ConstraintViolation<OIDCClientRepresentation>> violations = 
            validator.validate(rep, CreateClientDefault.class);
        
        // CreateClient group is first in sequence, so clientId violation should be caught first
        assertThat(violations, hasSize(greaterThan(0)));
    }

    // JSON Polymorphism Tests

    @Test
    void testPolymorphicDeserializationToOIDC() throws JsonProcessingException {
        String json = """
            {
                "protocol": "openid-connect",
                "clientId": "oidc-client"
            }
            """;
        
        BaseClientRepresentation rep = MAPPER.readValue(json, BaseClientRepresentation.class);
        
        assertThat(rep, instanceOf(OIDCClientRepresentation.class));
        assertThat(rep.getClientId(), is("oidc-client"));
        assertThat(rep.getProtocol(), is("openid-connect"));
    }

    @Test
    void testPolymorphicDeserializationToSAML() throws JsonProcessingException {
        String json = """
            {
                "protocol": "saml",
                "clientId": "saml-client"
            }
            """;
        
        BaseClientRepresentation rep = MAPPER.readValue(json, BaseClientRepresentation.class);
        
        assertThat(rep, instanceOf(SAMLClientRepresentation.class));
        assertThat(rep.getClientId(), is("saml-client"));
        assertThat(rep.getProtocol(), is("saml"));
    }

    @Test
    void testJsonSerializationIncludesProtocol() throws JsonProcessingException {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-client");
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        assertThat(node.get("protocol").asText(), is("openid-connect"));
    }

    @Test
    void testJsonSerializationOmitsNullFields() throws JsonProcessingException {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-client");
        // Leave other fields null
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        assertThat(node.has("displayName"), is(false));
        assertThat(node.has("description"), is(false));
        assertThat(node.has("enabled"), is(false));
        assertThat(node.has("appUrl"), is(false));
    }

    @Test
    void testJsonSerializationOmitsEmptyCollections() throws JsonProcessingException {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-client");
        rep.setRedirectUris(Set.of());
        rep.setRoles(Set.of());
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        // Empty collections should be omitted due to @JsonInclude(NON_EMPTY)
        assertThat(node.has("redirectUris"), is(false));
        assertThat(node.has("roles"), is(false));
    }

    @Test
    void testJsonSerializationIncludesNonEmptyCollections() throws JsonProcessingException {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-client");
        rep.setRedirectUris(Set.of("http://localhost:8080"));
        rep.setRoles(Set.of("role1"));
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        assertThat(node.has("redirectUris"), is(true));
        assertThat(node.get("redirectUris").isArray(), is(true));
        assertThat(node.has("roles"), is(true));
        assertThat(node.get("roles").isArray(), is(true));
    }

    @Test
    void testAdditionalFieldsSerializedAtTopLevel() throws JsonProcessingException {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-client");
        rep.setAdditionalField("customKey", "customValue");
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        // Additional fields should be serialized at the top level, not nested
        assertThat(node.get("customKey").asText(), is("customValue"));
    }

    @Test
    void testDiscriminatorFieldConstant() {
        assertThat(BaseClientRepresentation.DISCRIMINATOR_FIELD, is("protocol"));
    }

    private OIDCClientRepresentation createRepWithAllFields() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("test-client");
        rep.setDisplayName("Test Client");
        rep.setDescription("A test client");
        rep.setEnabled(true);
        rep.setAppUrl("http://localhost:8080");
        rep.setRedirectUris(Set.of("http://localhost:8080/callback"));
        rep.setRoles(Set.of("role1", "role2"));
        return rep;
    }
}
