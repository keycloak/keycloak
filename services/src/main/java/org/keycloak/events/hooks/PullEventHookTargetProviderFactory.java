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

    private static final List<ProviderConfigProperty> CONFIG = ProviderConfigurationBuilder.create()
            .property()
            .name("pullSecret")
            .label("eventHookTargetPullSecret")
            .helpText("eventHookTargetPullSecretHelp")
            .type(ProviderConfigProperty.PASSWORD)
            .secret(true)
            .add()
            .build();

    @Override
    public EventHookTargetProvider create(KeycloakSession session) {
        return new PullEventHookTargetProvider();
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
        if (!"consume".equals(endpointName)) {
            return null;
        }

        return new PullEventHookTargetEndpointResource(session, target);
    }

    @Override
    public void validateConfig(KeycloakSession session, Map<String, Object> settings) {
    }

    @Override
    public EventHookDeliveryResult test(KeycloakSession session, RealmModel realm, EventHookTargetModel target) {
        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(true);
        result.setRetryable(false);
        result.setStatusCode("PULL_READY");
        result.setDetails(getDisplayInfo(target));
        result.setDurationMillis(0);
        return result;
    }

    static String consumePath(EventHookTargetModel target) {
        String realmId = target == null ? null : target.getRealmId();
        String targetId = target == null ? null : target.getId();

        if (realmId == null || realmId.isBlank()) {
            realmId = "{realm}";
        }
        if (targetId == null || targetId.isBlank()) {
            targetId = "{targetId}";
        }

        return CONSUME_PATH_TEMPLATE
                .replace("{realm}", realmId)
                .replace("{targetId}", targetId);
    }
}
