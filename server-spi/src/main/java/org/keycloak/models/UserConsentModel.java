package org.keycloak.models;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentModel {

    private final ClientModel client;
    private Set<ProtocolMapperModel> protocolMappers = new HashSet<ProtocolMapperModel>();
    private Set<RoleModel> roles = new HashSet<RoleModel>();

    public UserConsentModel(ClientModel client) {
        this.client = client;
    }

    public ClientModel getClient() {
        return client;
    }

    public void addGrantedRole(RoleModel role) {
        roles.add(role);
    }

    public Set<RoleModel> getGrantedRoles() {
        return roles;
    }

    public boolean isRoleGranted(RoleModel role) {
        for (RoleModel currentRole : roles) {
            if (currentRole.getId().equals(role.getId())) return true;
        }
        return false;
    }

    public void addGrantedProtocolMapper(ProtocolMapperModel protocolMapper) {
        protocolMappers.add(protocolMapper);
    }

    public Set<ProtocolMapperModel> getGrantedProtocolMappers() {
        return protocolMappers;
    }

    public boolean isProtocolMapperGranted(ProtocolMapperModel protocolMapper) {
        for (ProtocolMapperModel currentProtMapper : protocolMappers) {
            if (currentProtMapper.getId().equals(protocolMapper.getId())) return true;
        }
        return false;
    }

}
