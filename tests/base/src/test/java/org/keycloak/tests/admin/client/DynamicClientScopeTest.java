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

@KeycloakIntegrationTest(config = DynamicClientScopeTest.DynamicClientScopeServerConfig.class)
public class DynamicClientScopeTest extends AbstractClientScopeTest {

    @Test
    public void testCreateValidDynamicScope() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("dynamic-scope-def");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-def:*");
        }});
        String scopeDefId = createClientScopeWithCleanup(scopeRep);

        // Assert updated attributes
        scopeRep = clientScopes().get(scopeDefId).toRepresentation();
        Assertions.assertEquals("dynamic-scope-def", scopeRep.getName());
        Assertions.assertEquals("true", scopeRep.getAttributes().get(ClientScopeModel.IS_DYNAMIC_SCOPE));
        Assertions.assertEquals("dynamic-scope-def:*", scopeRep.getAttributes().get(ClientScopeModel.DYNAMIC_SCOPE_REGEXP));
    }

    @Test
    public void testCreateNonDynamicScopeWithFeatureEnabled() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("non-dynamic-scope-def");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "false");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "");
        }});
        String scopeDefId = createClientScopeWithCleanup(scopeRep);

        // Assert updated attributes
        scopeRep = clientScopes().get(scopeDefId).toRepresentation();
        Assertions.assertEquals("non-dynamic-scope-def", scopeRep.getName());
        Assertions.assertEquals("false", scopeRep.getAttributes().get(ClientScopeModel.IS_DYNAMIC_SCOPE));
        assertThat(scopeRep.getAttributes().get(ClientScopeModel.DYNAMIC_SCOPE_REGEXP), anyOf(nullValue(), equalTo("")));
    }

    @Test
    public void testCreateInvalidRegexpDynamicScope() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("dynamic-scope-def4");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-def:*:*");
        }});
        handleExpectedCreateFailure(scopeRep, 400, "Invalid format for the Dynamic Scope regexp dynamic-scope-def:*:*");
    }

    @Test
    public void updateAssignedDefaultClientScopeToDynamicScope() {
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
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-def:*:*");
        }});

        try {
            clientScopes().get(scopeDefId).update(scopeRep);
            Assertions.fail("This update should fail");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }
    }

    @Test
    public void dynamicClientScopeCannotBeAssignedAsDefaultClientScope() {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("dyn-scope-client-default");
        clientRep.setProtocol("openid-connect");
        final String clientUuid = createClientWithCleanup(clientRep);

        ClientScopeRepresentation optionalClientScope = new ClientScopeRepresentation();
        optionalClientScope.setName("optional-dynamic-client-scope");
        optionalClientScope.setProtocol("openid-connect");
        optionalClientScope.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-def:*");
        }});
        String optionalClientScopeId = createClientScopeWithCleanup(optionalClientScope);

        try {
            ClientResource clientResource = managedRealm.admin().clients().get(clientUuid);
            clientResource.addDefaultClientScope(optionalClientScopeId);
            Assertions.fail("A Dynamic Scope shouldn't not be assigned as a default scope to a client");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }

    }

    @Test
    public void dynamicClientScopeCannotBeAssignedAsRealmDefaultClientScope() {
        ClientScopeRepresentation dynamicScope = new ClientScopeRepresentation();
        dynamicScope.setName("dynamic-scope-for-realm-default");
        dynamicScope.setProtocol("openid-connect");
        dynamicScope.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-for-realm-default:*");
        }});
        String dynamicScopeId = createClientScopeWithCleanup(dynamicScope);

        try {
            managedRealm.admin().addDefaultDefaultClientScope(dynamicScopeId);
            Assertions.fail("A Dynamic Scope should not be assigned as a realm default scope");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }
    }

    @Test
    public void dynamicClientScopeCanBeAssignedAsRealmOptionalClientScope() {
        ClientScopeRepresentation dynamicScope = new ClientScopeRepresentation();
        dynamicScope.setName("dynamic-scope-for-realm-optional");
        dynamicScope.setProtocol("openid-connect");
        dynamicScope.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope-for-realm-optional:*");
        }});
        String dynamicScopeId = createClientScope(dynamicScope);

        managedRealm.admin().addDefaultOptionalClientScope(dynamicScopeId);
        managedRealm.cleanup().add(r -> r.removeDefaultOptionalClientScope(dynamicScopeId));
        managedRealm.cleanup().add(r -> r.clientScopes().get(dynamicScopeId).remove());
    }

    @Test
    public void realmDefaultClientScopeCannotBeMadeDynamic() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-for-realm-default-update");
        scopeRep.setProtocol("openid-connect");
        String scopeId = createClientScope(scopeRep);

        managedRealm.admin().addDefaultDefaultClientScope(scopeId);
        managedRealm.cleanup().add(r -> r.removeDefaultDefaultClientScope(scopeId));
        managedRealm.cleanup().add(r -> r.clientScopes().get(scopeId).remove());

        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "scope-for-realm-default-update:*");
        }});

        try {
            clientScopes().get(scopeId).update(scopeRep);
            Assertions.fail("A Realm Default Scope should not be made dynamic");
        } catch (ClientErrorException ex) {
            assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }
    }

    public static class DynamicClientScopeServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.DYNAMIC_SCOPES);
        }
    }
}
