package org.keycloak.admin.client.service;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.KeycloakException;
import org.keycloak.admin.client.URI;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.RoleRepresentation;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.keycloak.admin.client.json.JsonSerialization.writeValueAsString;
import static org.keycloak.admin.client.utils.JsonUtils.getTypedList;
import static org.keycloak.admin.client.utils.JsonUtils.getTypedSet;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class RoleService extends KeycloakService {

    public RoleService(Config config, TokenManager tokenManager){
        super(config, tokenManager);
    }

    public void createRole(RoleRepresentation roleRepresentation){
        String uri = URI.ROLES.build(config.getServerUrl(), config.getRealm());
        try {
            http.post(uri).withBody(writeValueAsString(roleRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void updateRole(RoleRepresentation roleRepresentation){
        String uri = URI.ROLE.build(config.getServerUrl(), config.getRealm(), roleRepresentation.getName());
        try {
            http.put(uri).withBody(writeValueAsString(roleRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeRole(String roleName){
        String uri = URI.ROLE.build(config.getServerUrl(), config.getRealm(), roleName);
        http.delete(uri).execute();
    }


    public Set<RoleRepresentation> getChildren(RoleRepresentation role){
        String uri = URI.ROLE_COMPOSITE.build(config.getServerUrl(), config.getRealm(), role.getName());
        return http.get(uri).getTypedResponse(getTypedSet(RoleRepresentation.class));
    }

    public void addChildren(RoleRepresentation compositeRole, List<RoleRepresentation> rolesToAdd){
        String uri = URI.ROLE_COMPOSITE.build(config.getServerUrl(), config.getRealm(), compositeRole.getName());
        try {
            http.post(uri).withBody(writeValueAsString(rolesToAdd)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeChildren(RoleRepresentation compositeRole, List<RoleRepresentation> rolesToRemove){
        String uri = URI.ROLE_COMPOSITE.build(config.getServerUrl(), config.getRealm(), compositeRole.getName());
        try {
            http.delete(uri).withBody(writeValueAsString(rolesToRemove)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public RoleRepresentation findById(String roleId){
        String uri = URI.ROLE_BY_ID.build(config.getServerUrl(), config.getRealm(), roleId);
        return http.get(uri).getTypedResponse(RoleRepresentation.class);
    }

    public void updateById(String roleId, RoleRepresentation roleRepresentation){
        String uri = URI.ROLE_BY_ID.build(config.getServerUrl(), config.getRealm(), roleId);
        try {
            http.put(uri).withBody(writeValueAsString(roleRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeById(String roleId){
        String uri = URI.ROLE_BY_ID.build(config.getServerUrl(), config.getRealm(), roleId);
        http.delete(uri).execute();
    }

    public Set<RoleRepresentation> getChildrenById(String roleId){
        String uri = URI.ROLE_BY_ID_COMPOSITE.build(config.getServerUrl(), config.getRealm(), roleId);
        return http.get(uri).getTypedResponse(getTypedSet(RoleRepresentation.class));
    }

    public void addChildrenById(String roleId, List<RoleRepresentation> rolesToAdd){
        String uri = URI.ROLE_BY_ID_COMPOSITE.build(config.getServerUrl(), config.getRealm(), roleId);
        try {
            http.post(uri).withBody(writeValueAsString(rolesToAdd)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeChildrenById(String roleId, List<RoleRepresentation> rolesToRemove){
        String uri = URI.ROLE_BY_ID_COMPOSITE.build(config.getServerUrl(), config.getRealm(), roleId);
        try {
            http.delete(uri).withBody(writeValueAsString(rolesToRemove)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public Set<RoleRepresentation> getApplicationLevelChildren(RoleRepresentation role, String appName){
        String uri = URI.ROLE_APP_LEVEL_COMPOSITE.build(config.getServerUrl(), config.getRealm(), role.getName(), appName);
        return http.get(uri).getTypedResponse(getTypedSet(RoleRepresentation.class));
    }

    public void addApplicationLevelChildren(RoleRepresentation compositeRole, List<RoleRepresentation> rolesToAdd, String appName){
        String uri = URI.ROLE_APP_LEVEL_COMPOSITE.build(config.getServerUrl(), config.getRealm(), compositeRole.getName(), appName);
        try {
            http.post(uri).withBody(writeValueAsString(rolesToAdd)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeApplicationLevelChildren(RoleRepresentation compositeRole, List<RoleRepresentation> rolesToRemove, String appName){
        String uri = URI.ROLE_APP_LEVEL_COMPOSITE.build(config.getServerUrl(), config.getRealm(), compositeRole.getName(), appName);
        try {
            http.delete(uri).withBody(writeValueAsString(rolesToRemove)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public Set<RoleRepresentation> getApplicationLevelRoleById(String roleId, String appName){
        String uri = URI.ROLE_BY_ID_APP_LEVEL_COMPOSITE.build(config.getServerUrl(), config.getRealm(), roleId, appName);
        return http.get(uri).getTypedResponse(getTypedSet(RoleRepresentation.class));
    }

    public RoleRepresentation findRealmRoleByName(String roleName){
        String uri = URI.ROLE.build(config.getServerUrl(), config.getRealm(), roleName);
        return http.get(uri).getTypedResponse(RoleRepresentation.class);
    }

    public List<RoleRepresentation> findAllRealmRoles(){
        String uri = URI.ROLES.build(config.getServerUrl(), config.getRealm());
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public Set<RoleRepresentation> getRealmLevelChildren(RoleRepresentation role){
        String uri = URI.ROLE_REALM_LEVEL_COMPOSITE.build(config.getServerUrl(), config.getRealm(), role.getName());
        return http.get(uri).getTypedResponse(getTypedSet(RoleRepresentation.class));
    }

    public void addRealmLevelChildren(RoleRepresentation compositeRole, List<RoleRepresentation> rolesToAdd){
        String uri = URI.ROLE_REALM_LEVEL_COMPOSITE.build(config.getServerUrl(), config.getRealm(), compositeRole.getName());
        try {
            http.post(uri).withBody(writeValueAsString(rolesToAdd)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeRealmLevelChildren(RoleRepresentation compositeRole, List<RoleRepresentation> rolesToRemove){
        String uri = URI.ROLE_REALM_LEVEL_COMPOSITE.build(config.getServerUrl(), config.getRealm(), compositeRole.getName());
        try {
            http.delete(uri).withBody(writeValueAsString(rolesToRemove)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public Set<RoleRepresentation> getRealmLevelChildrenById(String roleId){
        String uri = URI.ROLE_BY_ID_REALM_LEVEL_COMPOSITE.build(config.getServerUrl(), config.getRealm(), roleId);
        return http.get(uri).getTypedResponse(getTypedSet(RoleRepresentation.class));
    }



}
