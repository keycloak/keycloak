package org.keycloak.models;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GrantedConsentModel {

    private final String clientId;
    private Set<String> protocolMapperIds = new HashSet<String>();
    private Set<String> roleIds = new HashSet<String>();

    public GrantedConsentModel(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void addGrantedRole(String roleId) {
        roleIds.add(roleId);
    }

    public Set<String> getGrantedRoles() {
        return roleIds;
    }

    public boolean isRoleGranted(String roleId) {
        return roleIds.contains(roleId);
    }

    public void addGrantedProtocolMapper(String protocolMapperId) {
        protocolMapperIds.add(protocolMapperId);
    }

    public Set<String> getGrantedProtocolMappers() {
        return protocolMapperIds;
    }

    public boolean isProtocolMapperGranted(String protocolMapperId) {
        return protocolMapperIds.contains(protocolMapperId);
    }

}
