package org.keycloak.tests.admin.client;

import java.util.HashMap;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.utils.matchers.Matchers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest(config = ParameterizedClientScopeTest.ParameterizedClientScopeServerConfig.class)
public class ParameterizedClientScopeTest extends AbstractClientScopeTest {

    @Test
    public void testCreateValidParameterizedScope() {
        String scopeDefId = createClientScopeWithCleanup(parameterizedScopeRep("dynamic-scope-def", "string"));

        // Assert updated attributes
        ClientScopeRepresentation scopeRep = clientScopes().get(scopeDefId).toRepresentation();
        Assertions.assertEquals("dynamic-scope-def", scopeRep.getName());
        Assertions.assertEquals("true", scopeRep.getAttributes().get(ClientScopeModel.IS_PARAMETERIZED_SCOPE));
        Assertions.assertEquals("string", scopeRep.getAttributes().get(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE));
    }

    @Test
    public void testCreateNonParameterizedScopeWithFeatureEnabled() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("non-dynamic-scope-def");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "false");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "");
        }});
        String scopeDefId = createClientScopeWithCleanup(scopeRep);

        // Assert updated attributes
        scopeRep = clientScopes().get(scopeDefId).toRepresentation();
        Assertions.assertEquals("non-dynamic-scope-def", scopeRep.getName());
        Assertions.assertEquals("false", scopeRep.getAttributes().get(ClientScopeModel.IS_PARAMETERIZED_SCOPE));
        assertThat(scopeRep.getAttributes().get(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP), anyOf(nullValue(), equalTo("")));
    }

    @Test
    public void testCreateParameterizedScopeWithoutTypeIsRejected() {
        handleExpectedCreateFailure(parameterizedScopeRep("dynamic-scope-def4", null, "dynamic-scope-def:*"),
                400, "Parameterized scope must have a parameter type");
    }

    @Test
    public void updateAssignedDefaultClientScopeToParameterizedScope() {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("dyn-scope-client-update");
        clientRep.setProtocol("openid-connect");
        final String clientUuid = createClientWithCleanup(clientRep);

        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("dynamic-scope-def-update");
        scopeRep.setProtocol("openid-connect");
        String scopeDefId = createClientScopeWithCleanup(scopeRep);

        managedRealm.admin().clients().get(clientUuid).addDefaultClientScope(scopeDefId);

        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "string");
        }});

        try {
            clientScopes().get(scopeDefId).update(scopeRep);
            Assertions.fail("This update should fail");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }
    }

    @Test
    public void parameterizedClientScopeCannotBeAssignedAsDefaultClientScope() {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("dyn-scope-client-default");
        clientRep.setProtocol("openid-connect");
        final String clientUuid = createClientWithCleanup(clientRep);

        String optionalClientScopeId = createClientScopeWithCleanup(parameterizedScopeRep("optional-dynamic-client-scope", "string"));

        try {
            ClientResource clientResource = managedRealm.admin().clients().get(clientUuid);
            clientResource.addDefaultClientScope(optionalClientScopeId);
            Assertions.fail("A Parameterized Scope shouldn't be assigned as a default scope to a client");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }

    }

    @Test
    public void parameterizedClientScopeCannotBeAssignedAsRealmDefaultClientScope() {
        String parameterizedScopeId = createClientScopeWithCleanup(parameterizedScopeRep("dynamic-scope-for-realm-default", "string"));

        try {
            managedRealm.admin().addDefaultDefaultClientScope(parameterizedScopeId);
            Assertions.fail("A Parameterized Scope should not be assigned as a realm default scope");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }
    }

    @Test
    public void parameterizedClientScopeCanBeAssignedAsRealmOptionalClientScope() {
        String parameterizedScopeId = createClientScope(parameterizedScopeRep("dynamic-scope-for-realm-optional", "string"));

        managedRealm.admin().addDefaultOptionalClientScope(parameterizedScopeId);
        managedRealm.cleanup().add(r -> r.removeDefaultOptionalClientScope(parameterizedScopeId));
        managedRealm.cleanup().add(r -> r.clientScopes().get(parameterizedScopeId).remove());
    }

    @Test
    public void realmDefaultClientScopeCannotBeMadeParameterized() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-for-realm-default-update");
        scopeRep.setProtocol("openid-connect");
        String scopeId = createClientScope(scopeRep);

        managedRealm.admin().addDefaultDefaultClientScope(scopeId);
        managedRealm.cleanup().add(r -> r.removeDefaultDefaultClientScope(scopeId));
        managedRealm.cleanup().add(r -> r.clientScopes().get(scopeId).remove());

        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "string");
        }});

        try {
            clientScopes().get(scopeId).update(scopeRep);
            Assertions.fail("A Realm Default Scope should not be made parameterized");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }
    }

    @Test
    public void testCreateParameterizedScopeWithBuiltInParameterizedType() {
        String scopeId = createClientScopeWithCleanup(parameterizedScopeRep("parameterized-scope-integer", "integer"));

        ClientScopeRepresentation scopeRep = clientScopes().get(scopeId).toRepresentation();
        Assertions.assertEquals("integer", scopeRep.getAttributes().get(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE));
    }

    @Test
    public void testCreateParameterizedScopeWithCustomTypeAndValidRegex() {
        String scopeId = createClientScopeWithCleanup(parameterizedScopeRep("parameterized-scope-custom", "custom", "[a-z]+"));

        ClientScopeRepresentation scopeRep = clientScopes().get(scopeId).toRepresentation();
        Assertions.assertEquals("custom", scopeRep.getAttributes().get(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE));
        Assertions.assertEquals("[a-z]+", scopeRep.getAttributes().get(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP));
    }

    @Test
    public void testCreateParameterizedScopeWithCustomTypeAndInvalidRegex() {
        handleExpectedCreateFailure(parameterizedScopeRep("parameterized-scope-custom-invalid", "custom", "([invalid"),
                400, "Invalid regex for the Parameterized Scope regexp ([invalid");
    }

    @Test
    public void testCreateParameterizedScopeWithCustomTypeAndMissingRegex() {
        handleExpectedCreateFailure(parameterizedScopeRep("parameterized-scope-custom-no-regex", "custom"),
                400, "Custom parameterized scope type requires a regex pattern");
    }

    @Test
    public void testCreateParameterizedScopeWithInvalidRegex() {
        handleExpectedCreateFailure(parameterizedScopeRep("parameterized-scope-invalid-regex", "string", "([invalid"),
                400, "Invalid regex for the Parameterized Scope regexp ([invalid");
    }

    @Test
    public void testCreateParameterizedScopeWithInvalidParameterType() {
        handleExpectedCreateFailure(parameterizedScopeRep("parameterized-scope-bad-type", "nonexistent"),
                400, "Invalid parameter type 'nonexistent'");
    }

    @Test
    public void testCreateParameterizedScopeWithBuiltInTypeNoRegexpRequired() {
        String scopeId = createClientScopeWithCleanup(parameterizedScopeRep("parameterized-scope-boolean", "boolean"));

        ClientScopeRepresentation scopeRep = clientScopes().get(scopeId).toRepresentation();
        Assertions.assertEquals("boolean", scopeRep.getAttributes().get(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE));
    }

    private ClientScopeRepresentation parameterizedScopeRep(String name, String type) {
        return parameterizedScopeRep(name, type, null);
    }

    private ClientScopeRepresentation parameterizedScopeRep(String name, String type, String regexp) {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName(name);
        scopeRep.setProtocol("openid-connect");
        HashMap<String, String> attrs = new HashMap<>();
        attrs.put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
        if (type != null) {
            attrs.put(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, type);
        }
        if (regexp != null) {
            attrs.put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, regexp);
        }
        scopeRep.setAttributes(attrs);
        return scopeRep;
    }

    public static class ParameterizedClientScopeServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES);
        }
    }
}
