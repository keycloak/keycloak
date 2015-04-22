package org.keycloak.models;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentModel {

    private final RealmModel realm;
    private final ClientModel client;
    private Set<ProtocolMapperModel> protocolMappers = new HashSet<ProtocolMapperModel>();
    private Set<RoleModel> roles = new HashSet<RoleModel>();

    public UserConsentModel(RealmModel realm, String clientId) {
        this.realm = realm;
        this.client = realm.getClientById(clientId);

        if (client == null) {
            throw new ModelException("Client with id [" + clientId + "] is not available");
        }
    }

    public ClientModel getClient() {
        return client;
    }

    public void addGrantedRole(String roleId) {
        RoleModel role = realm.getRoleById(roleId);

        // Chance that role was already deleted by other transaction and is not available anymore
        if (role != null) {
            roles.add(role);
        }
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

    public void addGrantedProtocolMapper(String protocolMapperId) {
        ProtocolMapperModel protocolMapper = client.getProtocolMapperById(protocolMapperId);

        // Chance that protocolMapper was already deleted by other transaction and is not available anymore
        if (protocolMapper != null) {
            protocolMappers.add(protocolMapper);
        }
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
