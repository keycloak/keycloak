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
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("dynamic-scope-def");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "dynamic-scope-def:*");
        }});
        String scopeDefId = createClientScopeWithCleanup(scopeRep);

        // Assert updated attributes
        scopeRep = clientScopes().get(scopeDefId).toRepresentation();
        Assertions.assertEquals("dynamic-scope-def", scopeRep.getName());
        Assertions.assertEquals("true", scopeRep.getAttributes().get(ClientScopeModel.IS_PARAMETERIZED_SCOPE));
        Assertions.assertEquals("dynamic-scope-def:*", scopeRep.getAttributes().get(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP));
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
    public void testCreateInvalidRegexpParameterizedScope() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("dynamic-scope-def4");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "dynamic-scope-def:*:*");
        }});
        handleExpectedCreateFailure(scopeRep, 400, "Invalid format for the Parameterized Scope regexp dynamic-scope-def:*:*");
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
            put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "dynamic-scope-def:*:*");
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

        ClientScopeRepresentation optionalClientScope = new ClientScopeRepresentation();
        optionalClientScope.setName("optional-dynamic-client-scope");
        optionalClientScope.setProtocol("openid-connect");
        optionalClientScope.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "dynamic-scope-def:*");
        }});
        String optionalClientScopeId = createClientScopeWithCleanup(optionalClientScope);

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
        ClientScopeRepresentation parameterizedScope = new ClientScopeRepresentation();
        parameterizedScope.setName("dynamic-scope-for-realm-default");
        parameterizedScope.setProtocol("openid-connect");
        parameterizedScope.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "dynamic-scope-for-realm-default:*");
        }});
        String parameterizedScopeId = createClientScopeWithCleanup(parameterizedScope);

        try {
            managedRealm.admin().addDefaultDefaultClientScope(parameterizedScopeId);
            Assertions.fail("A Parameterized Scope should not be assigned as a realm default scope");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }
    }

    @Test
    public void parameterizedClientScopeCanBeAssignedAsRealmOptionalClientScope() {
        ClientScopeRepresentation parameterizedScope = new ClientScopeRepresentation();
        parameterizedScope.setName("dynamic-scope-for-realm-optional");
        parameterizedScope.setProtocol("openid-connect");
        parameterizedScope.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "dynamic-scope-for-realm-optional:*");
        }});
        String parameterizedScopeId = createClientScope(parameterizedScope);

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
            put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "scope-for-realm-default-update:*");
        }});

        try {
            clientScopes().get(scopeId).update(scopeRep);
            Assertions.fail("A Realm Default Scope should not be made parameterized");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }
    }

    public static class ParameterizedClientScopeServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES);
        }
    }
}
