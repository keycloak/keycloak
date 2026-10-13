/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.tests.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.client.clienttype.ClientTypeManager;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTypeRepresentation;
import org.keycloak.representations.idm.ClientTypesRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.services.clienttype.impl.DefaultClientTypeProviderFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.in;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ClientTypesTest.ClientTypesServerConfig.class)
public class ClientTypesTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @BeforeEach
    public void cleanupRealmClientTypes() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();
        if (!clientTypes.getRealmClientTypes().isEmpty()) {
            clientTypes.setRealmClientTypes(new ArrayList<>());
            managedRealm.admin().clientTypes().updateClientTypes(clientTypes);
        }
    }

    @Test
    public void testFeatureWorksWhenEnabled() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();
        assertTrue(clientTypes.getRealmClientTypes().isEmpty());
    }

    @Test
    public void testCreateClientWithClientType() {
        ClientRepresentation clientRep = createClientWithType("foo", ClientTypeManager.SERVICE_ACCOUNT);
        assertEquals("foo", clientRep.getClientId());
        assertEquals(ClientTypeManager.SERVICE_ACCOUNT, clientRep.getType());
        assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep.getProtocol());
        assertFalse(clientRep.isStandardFlowEnabled());
        assertFalse(clientRep.isImplicitFlowEnabled());
        assertFalse(clientRep.isDirectAccessGrantsEnabled());
        assertTrue(clientRep.isServiceAccountsEnabled());
        assertFalse(clientRep.isPublicClient());
        assertFalse(clientRep.isBearerOnly());

        assertFalse(clientRep.getAttributes().containsKey(ClientModel.TYPE));
    }

    @Test
    public void testThatCreateClientWithWrongClientTypeFails() {
        ClientRepresentation clientRep = ClientBuilder.create()
                .clientId("client-type-does-not-exist-request")
                .type("DNE")
                .build();

        try(Response response = managedRealm.admin().clients().create(clientRep)) {
            assertEquals(Response.Status.BAD_REQUEST, response.getStatusInfo());
        }
    }

    @Test
    public void testUpdateClientWithClientType() {
        ClientRepresentation clientRep = createClientWithType("foo-update", ClientTypeManager.SERVICE_ACCOUNT);

        clientRep.setType(ClientTypeManager.STANDARD);
        try {
            managedRealm.admin().clients().get(clientRep.getId()).update(clientRep);
            fail("Not expected to update client");
        } catch (BadRequestException bre) {
            assertErrorContainsMessage(bre, ClientTypeException.Message.CANNOT_CHANGE_CLIENT_TYPE);
        }

        clientRep.setType(ClientTypeManager.SERVICE_ACCOUNT);
        clientRep.setServiceAccountsEnabled(false);
        try {
            managedRealm.admin().clients().get(clientRep.getId()).update(clientRep);
            fail("Not expected to update client");
        } catch (BadRequestException bre) {
            assertErrorResponseContainsParams(bre.getResponse(), "serviceAccountsEnabled");
        }

        clientRep.setServiceAccountsEnabled(true);

        clientRep.getAttributes().put(ClientModel.LOGO_URI, "https://foo");
        managedRealm.admin().clients().get(clientRep.getId()).update(clientRep);
        assertNull(managedRealm.admin().clients().get(clientRep.getId()).toRepresentation().getAttributes().get(ClientModel.LOGO_URI));

        clientRep.getAttributes().remove(ClientModel.LOGO_URI);
        clientRep.setRootUrl("https://foo");
        managedRealm.admin().clients().get(clientRep.getId()).update(clientRep);
    }

    @Test
    public void testCreateClientFailsWithMultipleInvalidClientTypeOverrides() {
        ClientRepresentation clientRep = ClientBuilder.create()
                .clientId("service-account-client-type-required-to-be-confidential-and-service-accounts-enabled")
                .type(ClientTypeManager.SERVICE_ACCOUNT)
                .serviceAccountsEnabled(false)
                .publicClient()
                .build();

        Response response = managedRealm.admin().clients().create(clientRep);
        assertErrorResponseContainsParams(response, "publicClient", "serviceAccountsEnabled");
    }

    @Test
    public void testClientTypesAdminRestAPI_globalTypes() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        assertEquals(0, clientTypes.getRealmClientTypes().size());

        List<ClientTypeRepresentation> globalClientTypeNames = new ArrayList<>(clientTypes.getGlobalClientTypes());
        assertNames(globalClientTypeNames, "sla", "service-account");

        ClientTypeRepresentation serviceAccountType = clientTypes.getGlobalClientTypes().stream()
                .filter(clientType -> "service-account".equals(clientType.getName()))
                .findFirst()
                .get();
        assertEquals("default", serviceAccountType.getProvider());

        ClientTypeRepresentation.PropertyConfig cfg = serviceAccountType.getConfig().get("standardFlowEnabled");
        assertPropertyConfig("standardFlowEnabled", cfg, false, null);

        cfg = serviceAccountType.getConfig().get("serviceAccountsEnabled");
        assertPropertyConfig("serviceAccountsEnabled", cfg, true, true);

        cfg = serviceAccountType.getConfig().get("tosUri");
        assertPropertyConfig("tosUri", cfg, false, null);
    }

    @Test
    public void testClientTypesAdminRestAPI_realmTypes() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        ClientTypeRepresentation clientType = new ClientTypeRepresentation();
        try {
            clientType.setName("sla1");
            clientType.setProvider("non-existent");
            clientType.setConfig(new HashMap<>());
            clientTypes.setRealmClientTypes(List.of(clientType));
            managedRealm.admin().clientTypes().updateClientTypes(clientTypes);
            fail("Not expected to update client types");
        } catch (BadRequestException bre) {
            assertErrorContainsMessage(bre, ClientTypeException.Message.INVALID_CLIENT_TYPE_PROVIDER);
        }

        try {
            clientType.setProvider(DefaultClientTypeProviderFactory.PROVIDER_ID);
            ClientTypeRepresentation.PropertyConfig cfg = new ClientTypeRepresentation.PropertyConfig();
            clientType.getConfig().put("standardFlowEnabled", cfg);
            managedRealm.admin().clientTypes().updateClientTypes(clientTypes);
            fail("Not expected to update client types");
        } catch (BadRequestException bre) {
            assertErrorContainsMessage(bre, ClientTypeException.Message.CLIENT_TYPE_FIELD_NOT_APPLICABLE);
        }

        try {
            ClientTypeRepresentation.PropertyConfig cfg = clientType.getConfig().get("standardFlowEnabled");
            cfg.setApplicable(false);
            cfg.setValue(true);
            managedRealm.admin().clientTypes().updateClientTypes(clientTypes);
            fail("Not expected to update client types");
        } catch (BadRequestException bre) {
            assertErrorContainsMessage(bre, ClientTypeException.Message.INVALID_CLIENT_TYPE_CONFIGURATION);
        }

        ClientTypeRepresentation.PropertyConfig cfg = clientType.getConfig().get("standardFlowEnabled");
        cfg.setApplicable(true);
        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        ClientTypeRepresentation clientType2 = new ClientTypeRepresentation();
        try {
            clientTypes = managedRealm.admin().clientTypes().getClientTypes();
            clientType2 = new ClientTypeRepresentation();
            clientType2.setName("sla1");
            clientType2.setProvider(DefaultClientTypeProviderFactory.PROVIDER_ID);
            clientType2.setConfig(new HashMap<>());
            clientTypes.getRealmClientTypes().add(clientType2);
            managedRealm.admin().clientTypes().updateClientTypes(clientTypes);
            fail("Not expected to update client types");
        } catch (BadRequestException bre) {
            assertErrorContainsMessage(bre, ClientTypeException.Message.DUPLICATE_CLIENT_TYPE);
        }

        try {
            clientType2.setName("service-account");
            managedRealm.admin().clientTypes().updateClientTypes(clientTypes);
            fail("Not expected to update client types");
        } catch (BadRequestException bre) {
            assertErrorContainsMessage(bre, ClientTypeException.Message.DUPLICATE_CLIENT_TYPE);
        }

        clientType2.setName("different");
        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        clientTypes = managedRealm.admin().clientTypes().getClientTypes();
        assertNames(clientTypes.getRealmClientTypes(), "sla1", "different");
        assertNames(clientTypes.getGlobalClientTypes(), "sla", "service-account");

        clientType2.setName("moreDifferent");
        clientTypes.getGlobalClientTypes().add(clientType2);
        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        clientTypes = managedRealm.admin().clientTypes().getClientTypes();
        assertNames(clientTypes.getRealmClientTypes(), "sla1", "different");
        assertNames(clientTypes.getGlobalClientTypes(), "sla", "service-account");
    }

    @Test
    public void testClientTypesInheritFromParent() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        ClientTypeRepresentation.PropertyConfig applicableAndTrue = new ClientTypeRepresentation.PropertyConfig();
        applicableAndTrue.setApplicable(true);
        applicableAndTrue.setValue(true);

        ClientTypeRepresentation childClientType = new ClientTypeRepresentation();
        childClientType.setName("child");
        childClientType.setProvider("default");
        childClientType.setParent("oidc");
        childClientType.setConfig(Map.of("standardFlowEnabled", applicableAndTrue));

        ClientTypeRepresentation subClientType = new ClientTypeRepresentation();
        subClientType.setName("subClientType");
        subClientType.setProvider("default");
        subClientType.setParent("child");
        subClientType.setConfig(Map.of("consentRequired", applicableAndTrue));

        List<ClientTypeRepresentation> realmClientTypes = clientTypes.getRealmClientTypes();
        realmClientTypes.add(childClientType);
        realmClientTypes.add(subClientType);
        clientTypes.setRealmClientTypes(realmClientTypes);

        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        ClientRepresentation childClient = createClientWithType("child-client", childClientType.getName());
        ClientRepresentation subClient = createClientWithType("sub-client", subClientType.getName());

        assertEquals( "openid-connect", childClient.getProtocol());
        assertEquals(true, childClient.isStandardFlowEnabled());
        assertEquals(false, childClient.isConsentRequired());

        assertEquals("openid-connect", subClient.getProtocol());
        assertEquals(true, subClient.isStandardFlowEnabled());
        assertEquals(true, subClient.isConsentRequired());
    }

    @Test
    public void testCreateClientFailsWithFullScopeAllowedOverride() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        ClientTypeRepresentation.PropertyConfig fullScopeConfig = new ClientTypeRepresentation.PropertyConfig();
        fullScopeConfig.setApplicable(true);
        fullScopeConfig.setValue(false);

        ClientTypeRepresentation customType = new ClientTypeRepresentation();
        customType.setName("no-full-scope");
        customType.setProvider(DefaultClientTypeProviderFactory.PROVIDER_ID);
        customType.setParent("oidc");
        customType.setConfig(Map.of("fullScopeAllowed", fullScopeConfig));

        List<ClientTypeRepresentation> realmClientTypes = clientTypes.getRealmClientTypes();
        realmClientTypes.add(customType);
        clientTypes.setRealmClientTypes(realmClientTypes);

        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        ClientRepresentation clientRep = ClientBuilder.create()
                .clientId("full-scope-override-test")
                .type("no-full-scope")
                .fullScopeEnabled(true)
                .build();

        Response response = managedRealm.admin().clients().create(clientRep);
        assertErrorResponseContainsParams(response, "fullScopeAllowed");
    }

    @Test
    public void testCreateClientFailsWithNodeReRegistrationTimeoutOverride() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        ClientTypeRepresentation.PropertyConfig timeoutConfig = new ClientTypeRepresentation.PropertyConfig();
        timeoutConfig.setApplicable(true);
        timeoutConfig.setValue(-1);

        ClientTypeRepresentation customType = new ClientTypeRepresentation();
        customType.setName("fixed-timeout");
        customType.setProvider(DefaultClientTypeProviderFactory.PROVIDER_ID);
        customType.setParent("oidc");
        customType.setConfig(Map.of("nodeReRegistrationTimeout", timeoutConfig));

        List<ClientTypeRepresentation> realmClientTypes = clientTypes.getRealmClientTypes();
        realmClientTypes.add(customType);
        clientTypes.setRealmClientTypes(realmClientTypes);

        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        ClientRepresentation clientRep = ClientBuilder.create()
                .clientId("timeout-override-test")
                .type("fixed-timeout")
                .build();
        clientRep.setNodeReRegistrationTimeout(300);

        Response response = managedRealm.admin().clients().create(clientRep);
        assertErrorResponseContainsParams(response, "nodeReRegistrationTimeout");
    }

    @Test
    public void testCreateClientSucceedsWithOmittedFullScopeAllowed() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        ClientTypeRepresentation.PropertyConfig fullScopeConfig = new ClientTypeRepresentation.PropertyConfig();
        fullScopeConfig.setApplicable(true);
        fullScopeConfig.setValue(false);

        ClientTypeRepresentation customType = new ClientTypeRepresentation();
        customType.setName("no-full-scope-omit");
        customType.setProvider(DefaultClientTypeProviderFactory.PROVIDER_ID);
        customType.setParent("oidc");
        customType.setConfig(Map.of("fullScopeAllowed", fullScopeConfig));

        List<ClientTypeRepresentation> realmClientTypes = clientTypes.getRealmClientTypes();
        realmClientTypes.add(customType);
        clientTypes.setRealmClientTypes(realmClientTypes);

        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        ClientRepresentation clientRep = createClientWithType("full-scope-omit-test", "no-full-scope-omit");
        assertFalse(clientRep.isFullScopeAllowed());
    }

    @Test
    public void testUpdateClientFailsWithFullScopeAllowedOverride() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        ClientTypeRepresentation.PropertyConfig fullScopeConfig = new ClientTypeRepresentation.PropertyConfig();
        fullScopeConfig.setApplicable(true);
        fullScopeConfig.setValue(false);

        ClientTypeRepresentation customType = new ClientTypeRepresentation();
        customType.setName("no-full-scope-update");
        customType.setProvider(DefaultClientTypeProviderFactory.PROVIDER_ID);
        customType.setParent("oidc");
        customType.setConfig(Map.of("fullScopeAllowed", fullScopeConfig));

        List<ClientTypeRepresentation> realmClientTypes = clientTypes.getRealmClientTypes();
        realmClientTypes.add(customType);
        clientTypes.setRealmClientTypes(realmClientTypes);

        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        ClientRepresentation clientRep = createClientWithType("full-scope-update-test", "no-full-scope-update");
        assertFalse(clientRep.isFullScopeAllowed());

        clientRep.setFullScopeAllowed(true);
        try {
            managedRealm.admin().clients().get(clientRep.getId()).update(clientRep);
            fail("Not expected to update client");
        } catch (BadRequestException bre) {
            assertErrorResponseContainsParams(bre.getResponse(), "fullScopeAllowed");
        }
    }

    @Test
    public void testUpdateClientFailsWithNodeReRegistrationTimeoutOverride() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        ClientTypeRepresentation.PropertyConfig timeoutConfig = new ClientTypeRepresentation.PropertyConfig();
        timeoutConfig.setApplicable(true);
        timeoutConfig.setValue(-1);

        ClientTypeRepresentation customType = new ClientTypeRepresentation();
        customType.setName("fixed-timeout-update");
        customType.setProvider(DefaultClientTypeProviderFactory.PROVIDER_ID);
        customType.setParent("oidc");
        customType.setConfig(Map.of("nodeReRegistrationTimeout", timeoutConfig));

        List<ClientTypeRepresentation> realmClientTypes = clientTypes.getRealmClientTypes();
        realmClientTypes.add(customType);
        clientTypes.setRealmClientTypes(realmClientTypes);

        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        ClientRepresentation clientRep = createClientWithType("timeout-update-test", "fixed-timeout-update");
        assertEquals(-1, clientRep.getNodeReRegistrationTimeout());

        clientRep.setNodeReRegistrationTimeout(300);
        try {
            managedRealm.admin().clients().get(clientRep.getId()).update(clientRep);
            fail("Not expected to update client");
        } catch (BadRequestException bre) {
            assertErrorResponseContainsParams(bre.getResponse(), "nodeReRegistrationTimeout");
        }
    }

    @Test
    public void testCreateClientFailsWithAuthorizationServicesEnabledOverride() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        ClientTypeRepresentation.PropertyConfig authzConfig = new ClientTypeRepresentation.PropertyConfig();
        authzConfig.setApplicable(true);
        authzConfig.setValue(false);

        ClientTypeRepresentation customType = new ClientTypeRepresentation();
        customType.setName("no-authz");
        customType.setProvider(DefaultClientTypeProviderFactory.PROVIDER_ID);
        customType.setParent("oidc");
        customType.setConfig(Map.of("authorizationServicesEnabled", authzConfig));

        List<ClientTypeRepresentation> realmClientTypes = clientTypes.getRealmClientTypes();
        realmClientTypes.add(customType);
        clientTypes.setRealmClientTypes(realmClientTypes);

        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        ClientRepresentation clientRep = ClientBuilder.create()
                .clientId("authz-override-test")
                .type("no-authz")
                .authorizationServicesEnabled(true)
                .build();

        Response response = managedRealm.admin().clients().create(clientRep);
        assertErrorResponseContainsParams(response, "authorizationServicesEnabled");
    }

    @Test
    public void testUpdateClientFailsWithAuthorizationServicesEnabledOverride() {
        ClientTypesRepresentation clientTypes = managedRealm.admin().clientTypes().getClientTypes();

        ClientTypeRepresentation.PropertyConfig authzConfig = new ClientTypeRepresentation.PropertyConfig();
        authzConfig.setApplicable(true);
        authzConfig.setValue(false);

        ClientTypeRepresentation customType = new ClientTypeRepresentation();
        customType.setName("no-authz-update");
        customType.setProvider(DefaultClientTypeProviderFactory.PROVIDER_ID);
        customType.setParent("oidc");
        customType.setConfig(Map.of("authorizationServicesEnabled", authzConfig));

        List<ClientTypeRepresentation> realmClientTypes = clientTypes.getRealmClientTypes();
        realmClientTypes.add(customType);
        clientTypes.setRealmClientTypes(realmClientTypes);

        managedRealm.admin().clientTypes().updateClientTypes(clientTypes);

        ClientRepresentation clientRep = createClientWithType("authz-update-test", "no-authz-update");

        clientRep.setAuthorizationServicesEnabled(true);
        try {
            managedRealm.admin().clients().get(clientRep.getId()).update(clientRep);
            fail("Not expected to update client");
        } catch (BadRequestException bre) {
            assertErrorResponseContainsParams(bre.getResponse(), "authorizationServicesEnabled");
        }
    }

    private void assertErrorResponseContainsParams(Response response, String... items) {
        assertEquals(Response.Status.BAD_REQUEST, response.getStatusInfo());
        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        assertThat(
                List.of(items),
                everyItem(in(errorRepresentation.getParams())));
    }

    private void assertErrorContainsMessage(BadRequestException bre, ClientTypeException.Message expectedException) {
        ErrorRepresentation errorRepresentation = bre.getResponse().readEntity(ErrorRepresentation.class);
        assertNotNull(errorRepresentation);
        assertEquals(expectedException.getMessage(), errorRepresentation.getErrorMessage());
    }

    private void assertNames(List<ClientTypeRepresentation> clientTypes, String... expectedNames) {
        List<String> names = clientTypes.stream()
                .map(ClientTypeRepresentation::getName)
                .collect(Collectors.toList());
        assertThat(names, hasItems(expectedNames));
    }

    private void assertPropertyConfig(String propertyName, ClientTypeRepresentation.PropertyConfig cfg, Boolean expectedApplicable, Object expectedValue) {
        assertEquals(expectedApplicable, cfg.getApplicable(), "'applicable' for property " + propertyName + " not equal");
        assertEquals(expectedValue, cfg.getValue(), "'value' for property " + propertyName + " not equal");
    }

    private ClientRepresentation createClientWithType(String clientId, String clientType) {
        ClientRepresentation clientRep = ClientBuilder.create()
                .clientId(clientId)
                .type(clientType)
                .build();
        Response response = managedRealm.admin().clients().create(clientRep);
        String clientUUID = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(clientUUID).remove());

        return managedRealm.admin().clients().get(clientUUID).toRepresentation();
    }

    public static class ClientTypesServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_TYPES);
        }
    }
}
