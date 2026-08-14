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

package org.keycloak.events.hooks;

import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class PullEventHookTargetProviderFactory implements EventHookTargetProviderFactory {

    public static final String ID = "pull";
    public static final String CONSUME_PATH_TEMPLATE = "/realms/{realm}/event-hooks/{targetId}/consume";
    public static final String TEST_PATH_TEMPLATE = "/realms/{realm}/event-hooks/{targetId}/test";

    private static final List<ProviderConfigProperty> CONFIG = ProviderConfigurationBuilder.create()
            .property()
            .name("pullSecret")
            .label("eventHookTargetPullSecret")
            .helpText("eventHookTargetPullSecretHelp")
            .type(ProviderConfigProperty.PASSWORD)
            .secret(true)
            .add()
            .property()
            .name(EventHookBodyMappingSupport.CUSTOM_BODY_MAPPING_TEMPLATE)
            .label("eventHookTargetCustomBodyMappingTemplate")
            .helpText("eventHookTargetCustomBodyMappingTemplateHelp")
            .type(ProviderConfigProperty.TEXT_TYPE)
            .add()
            .build();

    @Override
    public EventHookTargetProvider create(KeycloakSession session) {
        return new PullEventHookTargetProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return CONFIG;
    }

    @Override
    public boolean supportsBatch() {
        return true;
    }

    @Override
    public boolean supportsPush() {
        return false;
    }

    @Override
    public boolean supportsPull() {
        return true;
    }

    @Override
    public boolean supportsRetry() {
        return false;
    }

    @Override
    public String getDisplayInfo(EventHookTargetModel target) {
        return "PULL: " + consumePath(target);
    }

    @Override
    public Object getTargetEndpointResource(KeycloakSession session, RealmModel realm, EventHookTargetModel target, String endpointName) {
        if (realm != null && target != null) {
            target.setRealmName(realm.getName());
        }

        if ("consume".equals(endpointName)) {
            return new PullEventHookTargetEndpointResource(session, target, false);
        }

        if ("test".equals(endpointName)) {
            return new PullEventHookTargetEndpointResource(session, target, true);
        }

        return null;
    }

    @Override
    public void validateConfig(KeycloakSession session, Map<String, Object> settings) {
        EventHookBodyMappingSupport.validateConfig(settings);
    }

    @Override
    public EventHookDeliveryResult test(KeycloakSession session, RealmModel realm, EventHookTargetModel target) {
        return test(session, realm, target, (String) null);
    }

    @Override
    public EventHookDeliveryResult test(KeycloakSession session, RealmModel realm, EventHookTargetModel target, String exampleId) {
        return test(session, realm, target, createTestMessagesUnchecked(session, realm, target, exampleId));
    }

    @Override
    public EventHookDeliveryResult test(KeycloakSession session, RealmModel realm, EventHookTargetModel target, List<EventHookMessageModel> messages) {
        if (realm != null && target != null) {
            target.setRealmName(realm.getName());
        }

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(true);
        result.setRetryable(false);
        result.setStatusCode("PULL_TEST_READY");
        result.setDetails(testPath(target));
        result.setDurationMillis(0);
        return result;
    }

    static String consumePath(EventHookTargetModel target) {
        return path(CONSUME_PATH_TEMPLATE, target);
    }

    static String testPath(EventHookTargetModel target) {
        return path(TEST_PATH_TEMPLATE, target);
    }

    private List<EventHookMessageModel> createTestMessagesUnchecked(KeycloakSession session, RealmModel realm, EventHookTargetModel target,
            String exampleId) {
        try {
            return createTestMessages(session, realm, target, exampleId);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create pull test message", exception);
        }
    }

    private static String path(String template, EventHookTargetModel target) {
        String realmName = target == null ? null : target.getRealmName();
        String realmId = target == null ? null : target.getRealmId();
        String targetId = target == null ? null : target.getId();

        String realm = realmName != null && !realmName.isBlank() ? realmName : realmId;
        if (realm == null || realm.isBlank()) {
            realm = "{realm}";
        }
        if (targetId == null || targetId.isBlank()) {
            targetId = "{targetId}";
        }

        return template
                .replace("{realm}", realm)
                .replace("{targetId}", targetId);
    }
}
