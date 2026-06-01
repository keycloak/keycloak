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
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class EmailEventHookTargetProviderFactory implements EventHookTargetProviderFactory {

    public static final String ID = EventHookEmailTemplateSupport.ID;

    private static final List<ProviderConfigProperty> CONFIG = ProviderConfigurationBuilder.create()
            .property()
            .name(EventHookEmailTemplateSupport.RECIPIENT_TEMPLATE)
            .label("eventHookTargetEmailRecipientTemplate")
            .helpText("eventHookTargetEmailRecipientTemplateHelp")
            .type(ProviderConfigProperty.STRING_TYPE)
            .required(true)
            .add()
            .property()
            .name(EventHookEmailTemplateSupport.LOCALE_TEMPLATE)
            .label("eventHookTargetEmailLocaleTemplate")
            .helpText("eventHookTargetEmailLocaleTemplateHelp")
            .type(ProviderConfigProperty.STRING_TYPE)
            .add()
            .property()
            .name(EventHookEmailTemplateSupport.SUBJECT_TEMPLATE)
            .label("eventHookTargetEmailSubjectTemplate")
            .helpText("eventHookTargetEmailSubjectTemplateHelp")
            .type(ProviderConfigProperty.TEXT_TYPE)
            .required(true)
            .add()
            .property()
            .name(EventHookEmailTemplateSupport.TEXT_BODY_TEMPLATE)
            .label("eventHookTargetEmailTextBodyTemplate")
            .helpText("eventHookTargetEmailTextBodyTemplateHelp")
            .type(ProviderConfigProperty.TEXT_TYPE)
            .add()
            .property()
            .name(EventHookEmailTemplateSupport.HTML_BODY_TEMPLATE)
            .label("eventHookTargetEmailHtmlBodyTemplate")
            .helpText("eventHookTargetEmailHtmlBodyTemplateHelp")
            .type(ProviderConfigProperty.TEXT_TYPE)
            .add()
            .build();

    @Override
    public EventHookTargetProvider create(KeycloakSession session) {
        return new EmailEventHookTargetProvider(session);
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
    public boolean supportsAggregation() {
        return true;
    }

    @Override
    public void validateConfig(KeycloakSession session, Map<String, Object> settings) {
        EventHookEmailTemplateSupport.validateConfig(settings);
    }

    @Override
    public String getDisplayInfo(EventHookTargetModel target) {
        String recipientTemplate = target == null || target.getSettings() == null
                ? null
                : target.getSettings().get(EventHookEmailTemplateSupport.RECIPIENT_TEMPLATE) == null
                        ? null
                        : target.getSettings().get(EventHookEmailTemplateSupport.RECIPIENT_TEMPLATE).toString().trim();
        return recipientTemplate == null || recipientTemplate.isEmpty() ? "EMAIL" : "EMAIL: " + recipientTemplate;
    }
}
