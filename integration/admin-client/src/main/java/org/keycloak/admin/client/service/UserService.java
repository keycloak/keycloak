package org.keycloak.admin.client.service;

import org.codehaus.jackson.type.TypeReference;
import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.KeycloakException;
import org.keycloak.admin.client.URI;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.representations.idm.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.keycloak.admin.client.json.JsonSerialization.writeValueAsString;
import static org.keycloak.admin.client.utils.JsonUtils.getTypedList;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class UserService extends KeycloakService {

    public UserService(Config config, TokenManager tokenManager){
        super(config, tokenManager);
    }

    public List<UserRepresentation> findAll(){
        String uri = URI.USERS.build(config.getServerUrl(), config.getRealm());
        return http.get(uri).getTypedResponse(getTypedList(UserRepresentation.class));
    }

    public UserRepresentation find(String username){
        String uri = URI.USER.build(config.getServerUrl(), config.getRealm(), username);
        return http.get(uri).getTypedResponse(UserRepresentation.class);
    }

    public UserRepresentation findByEmail(String email){
        String uri = URI.USERS.build(config.getServerUrl(), config.getRealm());
        uri += "?search=" + email;

        List<UserRepresentation> usersList = http.get(uri).getTypedResponse(getTypedList(UserRepresentation.class));
        return usersList.isEmpty() ? null : usersList.get(0);
    }

    public void update(UserRepresentation userRepresentation){
        String uri = URI.USER.build(config.getServerUrl(), config.getRealm(), userRepresentation.getUsername());
        try {
            http.put(uri).withBody(writeValueAsString(userRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void remove(UserRepresentation userRepresentation){
        String uri = URI.USER.build(config.getServerUrl(), config.getRealm(), userRepresentation.getUsername());
        try {
            http.delete(uri).withBody(writeValueAsString(userRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void create(UserRepresentation userRepresentation){
        String uri = URI.USERS.build(config.getServerUrl(), config.getRealm());
        try {
            http.post(uri).withBody(writeValueAsString(userRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void logoutFromAllSessions(String username){
        String uri = URI.USER_LOGOUT.build(config.getServerUrl(), config.getRealm(), username);
        http.post(uri).execute();
    }

    public void removeTotp(String username){
        String uri = URI.USER_REMOVE_TOTP.build(config.getServerUrl(), config.getRealm(), username);
        http.put(uri).execute();
    }

    public void resetPassword(String username, String newPassword){
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        resetPassword(username, credential);
    }

    public void resetPassword(String username, CredentialRepresentation credentialRepresentation){
        String uri = URI.USER_RESET_PASSWORD.build(config.getServerUrl(), config.getRealm(), username);
        try {
            http.put(uri).withBody(writeValueAsString(credentialRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void resetPasswordEmail(String username){
        String uri = URI.USER_RESET_PASSWORD_EMAIL.build(config.getServerUrl(), config.getRealm(), username);
        http.put(uri).execute();
    }

    public Map<String, UserStats> getSessionStats(String username){
        String uri = URI.USER_SESSION_STATS.build(config.getServerUrl(), config.getRealm(), username);
        return http.get(uri).getTypedResponse(new TypeReference<Map<String, UserStats>>() {
        });
    }

    public List<UserSessionRepresentation> getSessions(String username){
        String uri = URI.USER_SESSIONS.build(config.getServerUrl(), config.getRealm(), username);
        return http.get(uri).getTypedResponse(getTypedList(UserSessionRepresentation.class));
    }

    public List<SocialLinkRepresentation> getSocialLinks(String username){
        String uri = URI.USER_SOCIAL_LINKS.build(config.getServerUrl(), config.getRealm(), username);
        return http.get(uri).getTypedResponse(getTypedList(SocialLinkRepresentation.class));
    }

    public MappingsRepresentation getAllRoleMappings(String username){
        String uri = URI.USER_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username);
        return http.get(uri).getTypedResponse(MappingsRepresentation.class);
    }

    public List<RoleRepresentation> getAvailableApplicationRoles(String username, String appName){
        String uri = URI.USER_AVAILABLE_APP_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username, appName);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public List<RoleRepresentation> getEffectiveApplicationRole(String username, String appName){
        String uri = URI.USER_EFFECTIVE_APP_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username, appName);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public List<String> getApplicationRoleNames(String username, String appName){
        return asStringList(getApplicationRoles(username, appName));
    }

    private List<String> asStringList(List<RoleRepresentation> roles) {
        List<String> roleNames = new ArrayList<String>();
        for(RoleRepresentation role : roles){
            roleNames.add(role.getName());
        }
        return roleNames;
    }

    public List<RoleRepresentation> getApplicationRoles(String username, String appName) {
        String uri = URI.USER_APP_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username, appName);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public void grantApplicationRole(String username, String appName, RoleRepresentation role){
        grantApplicationRoles(username, appName, Arrays.asList(role));
    }

    public void grantApplicationRoles(String username, String appName, List<RoleRepresentation> roles){
        String uri = URI.USER_APP_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username, appName);
        try {
            http.post(uri).withBody(writeValueAsString(roles)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeApplicationRoles(String username, String appName, List<RoleRepresentation> rolesToDelete){
        String uri = URI.USER_APP_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username, appName);
        try {
            http.delete(uri).withBody(writeValueAsString(rolesToDelete)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public List<RoleRepresentation> getAvailableRealmRoles(String username){
        String uri = URI.USER_AVAILABLE_REALM_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public List<RoleRepresentation> getEffectiveRealmRoles(String username){
        String uri = URI.USER_EFFECTIVE_REALM_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public List<String> getRealmRoleNames(String username){
        return asStringList(getRealmRoles(username));
    }

    public List<RoleRepresentation> getRealmRoles(String username){
        String uri = URI.USER_REALM_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public void grantRealmRole(String username, RoleRepresentation role){
        grantRealmRoles(username, Arrays.asList(role));
    }

    public void grantRealmRoles(String username, List<RoleRepresentation> roles){
        String uri = URI.USER_REALM_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username);
        try {
            http.post(uri).withBody(writeValueAsString(roles)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeRealmRoles(String username, List<RoleRepresentation> rolesToDelete){
        String uri = URI.USER_REALM_LEVEL_ROLE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), username);
        try {
            http.delete(uri).withBody(writeValueAsString(rolesToDelete)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

}
