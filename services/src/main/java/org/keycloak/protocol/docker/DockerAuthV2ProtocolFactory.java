package org.keycloak.protocol.docker;

import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.docker.mapper.AllowAllDockerProtocolMapper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerAuthV2ProtocolFactory extends AbstractLoginProtocolFactory implements EnvironmentDependentProviderFactory {

    static Map<String, ProtocolMapperModel> builtins = new HashMap<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {
        final ProtocolMapperModel addAllRequestedScopeMapper = new ProtocolMapperModel();
        addAllRequestedScopeMapper.setName(AllowAllDockerProtocolMapper.PROVIDER_ID);
        addAllRequestedScopeMapper.setProtocolMapper(AllowAllDockerProtocolMapper.PROVIDER_ID);
        addAllRequestedScopeMapper.setProtocol(DockerAuthV2Protocol.LOGIN_PROTOCOL);
        addAllRequestedScopeMapper.setConfig(Collections.emptyMap());
        builtins.put(AllowAllDockerProtocolMapper.PROVIDER_ID, addAllRequestedScopeMapper);
        defaultBuiltins.add(addAllRequestedScopeMapper);
    }

    @Override
    protected void createDefaultClientScopesImpl(RealmModel newRealm) {
        // no-op
    }

    @Override
    protected void addDefaults(final ClientModel client) {
        defaultBuiltins.forEach(builtinMapper -> client.addProtocolMapper(builtinMapper));
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
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
