/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.oidc.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TestOIDCProtocolMapper implements ProtocolMapper {

    public static final String ID = "test-oidc-mapper";

    @Override
    public String getProtocol() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayCategory() {
        return "My Display Category";
    }

    @Override
    public String getDisplayType() {
        return "My Display Type";
    }

    @Override
    public String getHelpText() {
        return "My Help Text";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = new ArrayList<>();
        ProviderConfigProperty dynamicProperty = new ProviderConfigProperty();

        dynamicProperty.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        dynamicProperty.setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        dynamicProperty.setHelpText(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_HELP_TEXT);
        dynamicProperty.setTypeResolver(session -> {
            RealmModel realm = session.getContext().getRealm();

            if (realm.getAttribute("is-dynamic-options", Boolean.FALSE)) {
                return ProviderConfigProperty.LIST_TYPE;
            }

            return ProviderConfigProperty.STRING_TYPE;
        });

        dynamicProperty.setOptionsResolver(session -> {
            RealmModel realm = session.getContext().getRealm();

            if (realm.getAttribute("is-dynamic-options", Boolean.FALSE)) {
                return Collections.singleton("dynamic-value");
            }

            return Collections.emptySet();
        });

        properties.add(dynamicProperty);

        return properties;
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        return new TestOIDCProtocolMapper();
    }

    @Override
    public void init(Scope config) {

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
}
