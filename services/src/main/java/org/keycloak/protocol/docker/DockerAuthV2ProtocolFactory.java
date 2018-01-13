package org.keycloak.protocol.docker;

import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.docker.mapper.AllowAllDockerProtocolMapper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DockerAuthV2ProtocolFactory extends AbstractLoginProtocolFactory implements EnvironmentDependentProviderFactory {

    static List<ProtocolMapperModel> builtins = new ArrayList<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {
        final ProtocolMapperModel addAllRequestedScopeMapper = new ProtocolMapperModel();
        addAllRequestedScopeMapper.setName(AllowAllDockerProtocolMapper.PROVIDER_ID);
        addAllRequestedScopeMapper.setProtocolMapper(AllowAllDockerProtocolMapper.PROVIDER_ID);
        addAllRequestedScopeMapper.setProtocol(DockerAuthV2Protocol.LOGIN_PROTOCOL);
        addAllRequestedScopeMapper.setConsentRequired(false);
        addAllRequestedScopeMapper.setConfig(Collections.EMPTY_MAP);
        builtins.add(addAllRequestedScopeMapper);
        defaultBuiltins.add(addAllRequestedScopeMapper);
    }

    @Override
    protected void addDefaults(final ClientModel client) {
        defaultBuiltins.forEach(builtinMapper -> client.addProtocolMapper(builtinMapper));
    }

    @Override
    public List<ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    @Override
    public List<ProtocolMapperModel> getDefaultBuiltinMappers() {
        return defaultBuiltins;
    }

    @Override
    public Object createProtocolEndpoint(final RealmModel realm, final EventBuilder event) {
        return new DockerV2LoginProtocolService(realm, event);
    }

    @Override
    public void setupClientDefaults(final ClientRepresentation rep, final ClientModel newClient) {
        // no-op
    }

    @Override
    public void setupTemplateDefaults(final ClientTemplateRepresentation clientRep, final ClientTemplateModel newClient) {
        // no-op
    }

    @Override
    public LoginProtocol create(final KeycloakSession session) {
        return new DockerAuthV2Protocol().setSession(session);
    }

    @Override
    public String getId() {
        return DockerAuthV2Protocol.LOGIN_PROTOCOL;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.DOCKER);
    }

    @Override
    public int order() {
        return -100;
    }
}
