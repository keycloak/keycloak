/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.client.policies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientProtocolCondition;
import org.keycloak.services.clientpolicy.condition.ClientProtocolConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.AllowedProtocolMappersExecutor;
import org.keycloak.services.clientpolicy.executor.AllowedProtocolMappersExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.providers.client.policies.TrackEventsClientPolicyExecutor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = AdminProtocolMapperClientPolicyTest.CustomProvidersServerConfig.class)
public class AdminProtocolMapperClientPolicyTest extends AbstractClientPoliciesTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests.client.policies", "org.keycloak.tests.providers.client.policies"})
    RunOnServerClient runOnServer;

    @Test
    public void rejectProtocolMapperCreateUpdateAndDelete() throws Exception {
        ProtocolMappersResource mappers = createClient("policy-mapper-client").getProtocolMappers();
        ProtocolMapperRepresentation existingMapper = mapper("existing-mapper", "email");
        String existingMapperId = createMapper(mappers, existingMapper);

        setupRejectingUpdaterPolicy();

        try (Response response = mappers.createMapper(mapper("created-mapper", "firstName"))) {
            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
        assertMapperAbsent(mappers, "created-mapper");

        ProtocolMapperRepresentation updatedMapper = mapper("updated-mapper", "lastName");
        updatedMapper.setId(existingMapperId);
        Assertions.assertThrows(BadRequestException.class, () -> mappers.update(existingMapperId, updatedMapper));
        Assertions.assertEquals("existing-mapper", mappers.getMapperById(existingMapperId).getName());

        Assertions.assertThrows(BadRequestException.class, () -> mappers.delete(existingMapperId));
        Assertions.assertNotNull(mappers.getMapperById(existingMapperId));
    }

    @Test
    public void rejectProtocolMapperBulkCreate() throws Exception {
        ProtocolMappersResource mappers = createClient("policy-mapper-bulk-client").getProtocolMappers();
        setupRejectingPolicy();

        List<ProtocolMapperRepresentation> proposedMappers = List.of(
                mapper("bulk-mapper-one", "firstName"),
                mapper("bulk-mapper-two", "lastName"));

        Assertions.assertThrows(BadRequestException.class, () -> mappers.createMapper(proposedMappers));
        assertMapperAbsent(mappers, "bulk-mapper-one");
        assertMapperAbsent(mappers, "bulk-mapper-two");
    }

    @Test
    public void allowConfiguredProtocolMapperTypesAndRejectOtherTypes() throws Exception {
        ProtocolMappersResource mappers = createClient("allowed-mapper-client").getProtocolMappers();
        setupAllowedProtocolMappersPolicy(List.of(UserAttributeMapper.PROVIDER_ID));

        String allowedMapperId = createMapper(mappers, mapper("allowed-mapper", "email"));
        ProtocolMapperRepresentation allowedUpdate = mappers.getMapperById(allowedMapperId);
        allowedUpdate.getConfig().put("user.attribute", "username");
        Assertions.assertDoesNotThrow(() -> mappers.update(allowedMapperId, allowedUpdate));

        try (Response response = mappers.createMapper(audienceMapper("restricted-mapper"))) {
            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
        assertMapperAbsent(mappers, "restricted-mapper");

        List<ProtocolMapperRepresentation> bulk = List.of(
                mapper("bulk-allowed-mapper", "firstName"),
                audienceMapper("bulk-restricted-mapper"));
        Assertions.assertThrows(BadRequestException.class, () -> mappers.createMapper(bulk));
        assertMapperAbsent(mappers, "bulk-allowed-mapper");
        assertMapperAbsent(mappers, "bulk-restricted-mapper");
    }

    @Test
    public void preserveUnlistedProtocolMapperOnUnchangedUpdateAndAllowDeletion() throws Exception {
        ProtocolMappersResource mappers = createClient("legacy-mapper-client").getProtocolMappers();
        String mapperId = createMapper(mappers, mapper("legacy-mapper", "email"));

        setupAllowedProtocolMappersPolicy(List.of(AudienceProtocolMapper.PROVIDER_ID));

        ProtocolMapperRepresentation unchanged = mapper("legacy-mapper", "email");
        unchanged.setId(mapperId);
        unchanged.setName("legacy-mapper-renamed");
        Assertions.assertDoesNotThrow(() -> mappers.update(mapperId, unchanged));

        ProtocolMapperRepresentation changed = mapper("legacy-mapper", "username");
        changed.setId(mapperId);
        Assertions.assertThrows(BadRequestException.class, () -> mappers.update(mapperId, changed));
        Assertions.assertEquals("email", mappers.getMapperById(mapperId).getConfig().get("user.attribute"));

        Assertions.assertDoesNotThrow(() -> mappers.delete(mapperId));
        assertMapperAbsent(mappers, "legacy-mapper-renamed");
    }

    @Test
    public void rejectUnlistedProtocolMapperAdjustedByEarlierExecutor() throws Exception {
        ProtocolMappersResource mappers = createClient("adjusted-unlisted-mapper-client").getProtocolMappers();
        String mapperId = createMapper(mappers, mapper("adjusted-unlisted-mapper", "email"));
        setupAdjustingAllowedProtocolMappersPolicy();

        ProtocolMapperRepresentation unchanged = mappers.getMapperById(mapperId);
        Assertions.assertThrows(BadRequestException.class, () -> mappers.update(mapperId, unchanged));
        Assertions.assertNull(mappers.getMapperById(mapperId).getConfig().get(TrackEventsClientPolicyExecutor.POLICY_ADJUSTED));
    }

    @Test
    public void replaceProtocolMapperWhoseProviderIsMissing() {
        ClientResource client = createClient("missing-provider-mapper-client");
        String clientId = client.toRepresentation().getId();
        ProtocolMappersResource mappers = client.getProtocolMappers();
        String mapperId = createMapper(mappers, mapper("missing-provider-mapper", "email"));

        runOnServer.run(new SetProtocolMapperProvider(clientId, mapperId, "missing-protocol-mapper-provider"));

        ProtocolMapperRepresentation replacement = mapper("replacement-mapper", "username");
        replacement.setId(mapperId);
        Assertions.assertDoesNotThrow(() -> mappers.update(mapperId, replacement));
        Assertions.assertEquals(UserAttributeMapper.PROVIDER_ID, mappers.getMapperById(mapperId).getProtocolMapper());
    }

    @Test
    public void rejectProtocolMapperUpdateWithMismatchedPathAndBodyIds() throws Exception {
        ProtocolMappersResource mappers = createClient("mismatched-mapper-client").getProtocolMappers();
        String firstMapperId = createMapper(mappers, mapper("first-mapper", "email"));
        String secondMapperId = createMapper(mappers, mapper("second-mapper", "username"));

        setupAllowedProtocolMappersPolicy(List.of(AudienceProtocolMapper.PROVIDER_ID));

        ProtocolMapperRepresentation update = mappers.getMapperById(firstMapperId);
        update.setId(secondMapperId);
        Assertions.assertThrows(BadRequestException.class, () -> mappers.update(firstMapperId, update));
        Assertions.assertEquals("first-mapper", mappers.getMapperById(firstMapperId).getName());
        Assertions.assertEquals("second-mapper", mappers.getMapperById(secondMapperId).getName());
    }

    @Test
    public void doNotApplyClientPolicyToClientScopeProtocolMapperCrud() throws Exception {
        ProtocolMappersResource mappers = createClientScope("scope-mapper-crud").getProtocolMappers();
        setupRejectingPolicy();

        String existingMapperId = createMapper(mappers, mapper("scope-existing-mapper", "email"));

        try (Response response = mappers.createMapper(mapper("scope-created-mapper", "firstName"))) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        List<ProtocolMapperRepresentation> bulk = List.of(
                mapper("scope-bulk-one", "lastName"),
                mapper("scope-bulk-two", "username"));
        Assertions.assertDoesNotThrow(() -> mappers.createMapper(bulk));

        ProtocolMapperRepresentation updated = mappers.getMapperById(existingMapperId);
        updated.setName("scope-updated-mapper");
        updated.getConfig().put("user.attribute", "username");
        Assertions.assertDoesNotThrow(() -> mappers.update(existingMapperId, updated));
        Assertions.assertEquals("username", mappers.getMapperById(existingMapperId).getConfig().get("user.attribute"));

        Assertions.assertDoesNotThrow(() -> mappers.delete(existingMapperId));
    }

    @Test
    public void doNotApplyAllowedProtocolMappersPolicyToClientScope() throws Exception {
        ProtocolMappersResource mappers = createClientScope("allowed-scope-mapper-crud").getProtocolMappers();
        setupAllowedProtocolMappersPolicy(List.of());

        String existingMapperId = createMapper(mappers, mapper("allowed-scope-existing-mapper", "email"));
        try (Response response = mappers.createMapper(mapper("allowed-scope-created-mapper", "firstName"))) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        ProtocolMapperRepresentation bulkMapper = mapper("allowed-scope-bulk-mapper", "lastName");
        Assertions.assertDoesNotThrow(() -> mappers.createMapper(List.of(bulkMapper)));

        ProtocolMapperRepresentation updated = mappers.getMapperById(existingMapperId);
        updated.setName("allowed-scope-updated-mapper");
        Assertions.assertDoesNotThrow(() -> mappers.update(existingMapperId, updated));
        Assertions.assertDoesNotThrow(() -> mappers.delete(existingMapperId));
    }

    @Test
    public void applyTargetClientProtocolConditionWithClientScopeSemantics() throws Exception {
        ProtocolMappersResource oidcMappers = createClient("target-oidc-mapper-client", OIDCLoginProtocol.LOGIN_PROTOCOL)
                .getProtocolMappers();
        ProtocolMappersResource samlMappers = createClient("target-saml-mapper-client", SamlProtocol.LOGIN_PROTOCOL)
                .getProtocolMappers();
        ProtocolMappersResource scopeMappers = createClientScope("target-mapper-scope").getProtocolMappers();

        setupRejectingProtocolPolicy(OIDCLoginProtocol.LOGIN_PROTOCOL);

        try (Response response = oidcMappers.createMapper(mapper("rejected-target-mapper", "email"))) {
            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
        assertMapperAbsent(oidcMappers, "rejected-target-mapper");

        try (Response response = samlMappers.createMapper(mapper("allowed-saml-target-mapper", "email"))) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        try (Response response = scopeMappers.createMapper(mapper("allowed-scope-target-mapper", "email"))) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void protocolMapperCrudTriggersCorrectEvents() throws Exception {
        ProtocolMappersResource mappers = createClient("event-mapper-client").getProtocolMappers();
        setupAdjustingPolicy();

        String mapperId = createMapper(mappers, mapper("event-mapper", "email"));
        assertAndClearEvents(ClientPolicyEvent.REGISTER_PROTOCOL_MAPPER);

        ProtocolMapperRepresentation update = mappers.getMapperById(mapperId);
        update.setName("updated-event-mapper");
        mappers.update(mapperId, update);
        assertAndClearEvents(ClientPolicyEvent.UPDATE_PROTOCOL_MAPPER);

        mappers.delete(mapperId);
        assertAndClearEvents(ClientPolicyEvent.UNREGISTER_PROTOCOL_MAPPER);
    }

    @Test
    public void persistPolicyAdjustedMapperRepresentations() throws Exception {
        ProtocolMappersResource mappers = createClient("adjusted-mapper-client").getProtocolMappers();
        setupAdjustingPolicy();

        ProtocolMapperRepresentation single = mapper("adjusted-single-mapper", "email");
        String singleId = createMapper(mappers, single);
        assertPolicyAdjustment(mappers.getMapperById(singleId));

        List<ProtocolMapperRepresentation> bulk = List.of(
                mapper("adjusted-bulk-one", "firstName"),
                mapper("adjusted-bulk-two", "lastName"));
        mappers.createMapper(bulk);
        bulk.forEach(expected -> assertPolicyAdjustment(mappers.getMappers().stream()
                .filter(actual -> expected.getName().equals(actual.getName()))
                .findFirst()
                .orElseThrow()));

        ProtocolMapperRepresentation update = mappers.getMapperById(singleId);
        update.getConfig().remove(TrackEventsClientPolicyExecutor.POLICY_ADJUSTED);
        update.getConfig().put("user.attribute", "username");
        mappers.update(singleId, update);
        assertPolicyAdjustment(mappers.getMapperById(singleId));
    }

    private ClientResource createClient(String clientId) {
        return createClient(clientId, OIDCLoginProtocol.LOGIN_PROTOCOL);
    }

    private ClientResource createClient(String clientId, String protocol) {
        ClientRepresentation client = ClientBuilder.create(generateSuffixedName(clientId))
                .protocol(protocol)
                .publicClient()
                .build();

        try (Response response = realm.admin().clients().create(client)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.clients().delete(id));
            return realm.admin().clients().get(id);
        }
    }

    private ClientScopeResource createClientScope(String name) {
        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(generateSuffixedName(name));
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        try (Response response = realm.admin().clientScopes().create(scope)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.clientScopes().get(id).remove());
            return realm.admin().clientScopes().get(id);
        }
    }

    private String createMapper(ProtocolMappersResource mappers, ProtocolMapperRepresentation mapper) {
        try (Response response = mappers.createMapper(mapper)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            return ApiUtil.getCreatedId(response);
        }
    }

    private ProtocolMapperRepresentation mapper(String name, String userAttribute) {
        Map<String, String> config = new HashMap<>();
        config.put("user.attribute", userAttribute);
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, name);
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        mapper.setConfig(config);
        return mapper;
    }

    private ProtocolMapperRepresentation audienceMapper(String name) {
        Map<String, String> config = new HashMap<>();
        config.put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, name);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);
        mapper.setConfig(config);
        return mapper;
    }

    private void assertMapperAbsent(ProtocolMappersResource mappers, String name) {
        Assertions.assertTrue(mappers.getMappers().stream().noneMatch(mapper -> name.equals(mapper.getName())));
    }

    private void assertPolicyAdjustment(ProtocolMapperRepresentation mapper) {
        Assertions.assertEquals(Boolean.TRUE.toString(),
                mapper.getConfig().get(TrackEventsClientPolicyExecutor.POLICY_ADJUSTED));
    }

    private void assertAndClearEvents(ClientPolicyEvent event) {
        Assertions.assertEquals(List.of(event), snapshotAndClearEvents());
    }

    private List<ClientPolicyEvent> snapshotAndClearEvents() {
        return List.of(runOnServer.fetch(new TrackEventsSnapshot(), ClientPolicyEvent[].class));
    }

    private void setupRejectingPolicy() throws Exception {
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }

    private void setupRejectingProtocolPolicy(String protocol) throws Exception {
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                ClientProtocolConditionFactory.PROVIDER_ID, new ClientProtocolCondition.Configuration(protocol));
    }

    private void setupAllowedProtocolMappersPolicy(List<String> allowedMapperTypes) throws Exception {
        AllowedProtocolMappersExecutor.Configuration configuration = new AllowedProtocolMappersExecutor.Configuration();
        configuration.setAllowedProtocolMapperTypes(allowedMapperTypes);
        setupPolicy(realm, AllowedProtocolMappersExecutorFactory.PROVIDER_ID, configuration,
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }

    private void setupAdjustingAllowedProtocolMappersPolicy() throws Exception {
        AllowedProtocolMappersExecutor.Configuration configuration = new AllowedProtocolMappersExecutor.Configuration();
        configuration.setAllowedProtocolMapperTypes(List.of(AudienceProtocolMapper.PROVIDER_ID));
        realm.updateWithCleanup(r -> {
            r.resetClientProfiles().clientProfile(ClientProfileBuilder.create()
                    .name("executor")
                    .executor(TrackEventsClientPolicyExecutor.PROVIDER_ID, new TrackEventsClientPolicyExecutor.Configuration())
                    .executor(AllowedProtocolMappersExecutorFactory.PROVIDER_ID, configuration)
                    .build());
            r.resetClientPolicies().clientPolicy(ClientPolicyBuilder.create()
                    .name("policy")
                    .condition(AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation())
                    .profile("executor")
                    .build());
            return r;
        });
    }

    private void setupRejectingUpdaterPolicy() throws Exception {
        ClientUpdaterContextCondition.Configuration configuration = new ClientUpdaterContextCondition.Configuration();
        configuration.setUpdateClientSource(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER));
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                ClientUpdaterContextConditionFactory.PROVIDER_ID, configuration);
    }

    private void setupAdjustingPolicy() throws Exception {
        snapshotAndClearEvents();
        setupPolicy(realm, TrackEventsClientPolicyExecutor.PROVIDER_ID,
                new TrackEventsClientPolicyExecutor.Configuration(), AnyClientConditionFactory.PROVIDER_ID,
                new ClientPolicyConditionConfigurationRepresentation());
    }

    public static class CustomProvidersServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
