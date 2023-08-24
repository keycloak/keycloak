package org.keycloak.broker.provider;

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.representations.provider.ScriptProviderMetadata;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.provider.ProviderConfigProperty;

public class DeployedIdPScriptMapper extends ScriptMapper{
    protected ScriptProviderMetadata metadata;

    public DeployedIdPScriptMapper(ScriptProviderMetadata metadata) {
        this.metadata = metadata;
    }

    public DeployedIdPScriptMapper() {
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
    protected String getScriptCode(IdentityProviderMapperModel mapperModel) {
        return metadata.getCode();
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return super.getConfigProperties().stream()
                .filter(providerConfigProperty -> !ProviderConfigProperty.SCRIPT_TYPE.equals(providerConfigProperty.getName())) // filter "script" property
                .collect(Collectors.toList());
    }

    public void setMetadata(ScriptProviderMetadata metadata) {
        this.metadata = metadata;
    }

    public ScriptProviderMetadata getMetadata() {
        return metadata;
    }
}
