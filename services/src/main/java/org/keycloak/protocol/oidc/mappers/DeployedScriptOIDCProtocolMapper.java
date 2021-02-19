/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.mappers;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.provider.ScriptProviderMetadata;

public class DeployedScriptOIDCProtocolMapper extends ScriptBasedOIDCProtocolMapper {

    private List<ProviderConfigProperty> configProperties;

    protected ScriptProviderMetadata metadata;

    public DeployedScriptOIDCProtocolMapper(ScriptProviderMetadata metadata) {
        this.metadata = metadata;
    }

    public DeployedScriptOIDCProtocolMapper() {
        // for reflection
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public String getDisplayType() {
        return metadata.getName();
    }

    @Override
    public String getHelpText() {
        return metadata.getDescription();
    }

    @Override
    protected String getScriptCode(ProtocolMapperModel mapperModel) {
        return metadata.getCode();
    }

    @Override
    public void init(Config.Scope config) {
        configProperties = ProviderConfigurationBuilder.create()
                .property()
                .name(ProtocolMapperUtils.MULTIVALUED)
                .label(ProtocolMapperUtils.MULTIVALUED_LABEL)
                .helpText(ProtocolMapperUtils.MULTIVALUED_HELP_TEXT)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .build();

        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, UserPropertyMapper.class);
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.SCRIPTS);
    }

    public void setMetadata(ScriptProviderMetadata metadata) {
        this.metadata = metadata;
    }

    public ScriptProviderMetadata getMetadata() {
        return metadata;
    }
}
