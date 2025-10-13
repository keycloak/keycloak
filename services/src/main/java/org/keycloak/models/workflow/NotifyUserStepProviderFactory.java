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

package org.keycloak.models.workflow;

import java.util.Arrays;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class NotifyUserStepProviderFactory implements WorkflowStepProviderFactory<NotifyUserStepProvider> {

    public static final String ID = "notify-user";

    @Override
    public NotifyUserStepProvider create(KeycloakSession session, ComponentModel model) {
        return new NotifyUserStepProvider(session, model);
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.USERS;
    }

    @Override
    public String getHelpText() {
        return "Sends email notifications to users based on configurable templates";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(
            new ProviderConfigProperty("reason", "Reason", 
                "Reason for the action (inactivity, workflow violation, compliance requirement)", 
                ProviderConfigProperty.STRING_TYPE, ""),

            new ProviderConfigProperty("custom_subject_key", "Custom Subject Message Key", 
                "Override default subject with custom message property key (optional)", 
                ProviderConfigProperty.STRING_TYPE, ""),

            new ProviderConfigProperty("custom_message", "Custom Message", 
                "Override default message with custom text (optional)", 
                ProviderConfigProperty.TEXT_TYPE, "")
        );
    }
}
