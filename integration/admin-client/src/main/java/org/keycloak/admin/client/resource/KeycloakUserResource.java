package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.UserService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.representations.idm.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakUserResource {

    private final String username;
    private final UserService userService;

    public KeycloakUserResource(Config config, TokenManager tokenManager, String username){
        this.username = username;
        this.userService = new UserService(config, tokenManager);
    }

    public UserRepresentation getRepresentation(){
        return userService.find(username);
    }

    public void logoutFromAllSessions(){
        userService.logoutFromAllSessions(username);
    }

    public void removeTotp(){
        userService.removeTotp(username);
    }

    public void resetPassword(String newPassword){
        userService.resetPassword(username, newPassword);
    }

    public void resetPassword(CredentialRepresentation credentialRepresentation){
        userService.resetPassword(username, credentialRepresentation);
    }

    public void resetPasswordEmail(){
        userService.resetPasswordEmail(username);
    }

    public Map<String, UserStats> getSessionStats(){
        return userService.getSessionStats(username);
    }

    public List<UserSessionRepresentation> getSessions(){
        return userService.getSessions(username);
    }

    public List<SocialLinkRepresentation> getSocialLinks(){
        return userService.getSocialLinks(username);
    }

    public MappingsRepresentation getAllRoleMappings(){
        return userService.getAllRoleMappings(username);
    }

    public List<RoleRepresentation> getAvailableApplicationRoles(String appName){
        return userService.getAvailableApplicationRoles(username, appName);
    }

    public List<RoleRepresentation> getEffectiveApplicationRole(String appName){
        return userService.getEffectiveApplicationRole(username, appName);
    }

    public List<String> getApplicationRoleNames(String appName){
        return userService.getApplicationRoleNames(username, appName);
    }

    public List<RoleRepresentation> getApplicationRoles(String appName) {
        return userService.getApplicationRoles(username, appName);
    }

    public void grantApplicationRole(String appName, RoleRepresentation role){
        grantApplicationRoles(appName, Arrays.asList(role));
    }

    public void grantApplicationRoles(String appName, List<RoleRepresentation> roles){
        userService.grantApplicationRoles(username, appName, roles);
    }

    public void removeApplicationRoles(String appName, List<RoleRepresentation> rolesToDelete){
        userService.removeApplicationRoles(username, appName, rolesToDelete);
    }

    public List<RoleRepresentation> getAvailableRealmRoles(){
        return userService.getAvailableRealmRoles(username);
    }

    public List<RoleRepresentation> getEffectiveRealmRoles(){
        return userService.getEffectiveRealmRoles(username);
    }

    public List<String> getRealmRoleNames(){
        return userService.getRealmRoleNames(username);
    }

    public List<RoleRepresentation> getRealmRoles(){
        return userService.getRealmRoles(username);
    }

    public void grantRealmRole(RoleRepresentation role){
        grantRealmRoles(Arrays.asList(role));
    }

    public void grantRealmRoles(List<RoleRepresentation> roles){
        userService.grantRealmRoles(username, roles);
    }

    public void removeRealmRoles(List<RoleRepresentation> rolesToDelete){
        userService.removeRealmRoles(username, rolesToDelete);
    }

}
