package org.keycloak.protocol.saml.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.provider.ScriptProviderMetadata;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DeployedScriptSAMLProtocolMapper extends ScriptBasedMapper {

    protected ScriptProviderMetadata metadata;

    public DeployedScriptSAMLProtocolMapper(ScriptProviderMetadata metadata) {
        this.metadata = metadata;
    }

    public DeployedScriptSAMLProtocolMapper() {
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
