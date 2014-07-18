package org.keycloak.admin.client.service;

import org.apache.http.HttpResponse;
import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.KeycloakException;
import org.keycloak.admin.client.URI;
import org.keycloak.admin.client.json.JsonSerialization;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.keycloak.admin.client.json.JsonSerialization.writeValueAsString;
import static org.keycloak.admin.client.utils.JsonUtils.getTypedList;
import static org.keycloak.admin.client.utils.JsonUtils.getTypedSet;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class ApplicationService extends KeycloakService{

    public ApplicationService(Config config, TokenManager tokenManager){
        super(config, tokenManager);
    }

    public void create(ApplicationRepresentation applicationRepresentation){
        String uri = URI.APPS.build(config.getServerUrl(), config.getRealm());
        try {
            http.post(uri).withBody(writeValueAsString(applicationRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public List<ApplicationRepresentation> findAll(){
        String uri = URI.APPS.build(config.getServerUrl(), config.getRealm());
        return http.get(uri).getTypedResponse(getTypedList(ApplicationRepresentation.class));
    }

    public ApplicationRepresentation find(String appName){
        String uri = URI.APP.build(config.getServerUrl(), config.getRealm(), appName);
        return http.get(uri).getTypedResponse(ApplicationRepresentation.class);
    }

    public void update(ApplicationRepresentation applicationRepresentation){
        String uri = URI.APP.build(config.getServerUrl(), config.getRealm(), applicationRepresentation.getName());
        try {
            http.put(uri).withBody(writeValueAsString(applicationRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void remove(String appName){
        String uri = URI.APP.build(config.getServerUrl(), config.getRealm(), appName);
        http.delete(uri).execute();
    }

    public Set<String> getAllowedOrigins(String appName){
        String uri = URI.APP_ALLOWED_ORIGINS.build(config.getServerUrl(), config.getRealm(), appName);
        return http.get(uri).getTypedResponse(Set.class);
    }

    public void updateAllowedOrigins(String appName, Set<String> newAllowedOrigins){
        String uri = URI.APP_ALLOWED_ORIGINS.build(config.getServerUrl(), config.getRealm(), appName);
        try {
            http.put(uri).withBody(writeValueAsString(newAllowedOrigins)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeAllowedOrigins(String appName, Set<String> originsToDelete){
        String uri = URI.APP_ALLOWED_ORIGINS.build(config.getServerUrl(), config.getRealm(), appName);
        try {
            http.delete(uri).withBody(writeValueAsString(originsToDelete)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public ClaimRepresentation getClaims(String appName){
        String uri = URI.APP_CLAIMS.build(config.getServerUrl(), config.getRealm(), appName);
        return http.get(uri).getTypedResponse(ClaimRepresentation.class);
    }

    public void updateClaims(String appName, ClaimRepresentation claimRepresentation){
        String uri = URI.APP_CLAIMS.build(config.getServerUrl(), config.getRealm(), appName);
        try {
            http.put(uri).withBody(writeValueAsString(claimRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public CredentialRepresentation generateNewSecret(String appName){
        String uri = URI.APP_SECRET.build(config.getServerUrl(), config.getRealm(), appName);
        HttpResponse response = http.post(uri).execute();
        try {
            return JsonSerialization.readValue(response.getEntity().getContent(), CredentialRepresentation.class);
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public CredentialRepresentation getSecret(String appName){
        String uri = URI.APP_SECRET.build(config.getServerUrl(), config.getRealm(), appName);
        return http.get(uri).getTypedResponse(CredentialRepresentation.class);
    }

    public String getInstallationJbossXml(String appName){
        String uri = URI.APP_INSTALLATION_JBOSS.build(config.getServerUrl(), config.getRealm(), appName);
        return http.get(uri).getStringResponse();
    }

    public String getInstallationJson(String appName){
        String uri = URI.APP_INSTALLATION_JSON.build(config.getServerUrl(), config.getRealm(), appName);
        return http.get(uri).getStringResponse();
    }

    public void logoutAllUsers(String appName){
        String uri = URI.APP_LOGOUT_ALL.build(config.getServerUrl(), config.getRealm(), appName);
        http.post(uri).execute();
    }

    public void logoutUser(String appName, String username){
        String uri = URI.APP_LOGOUT_USER.build(config.getServerUrl(), config.getRealm(), appName, username);
        http.post(uri).execute();
    }

    public void pushRevocation(String appName){
        String uri = URI.APP_PUSH_REVOCATION.build(config.getServerUrl(), config.getRealm(), appName);
        http.post(uri).execute();
    }

    public List<RoleRepresentation> getRoles(String appName){
        String uri = URI.APP_ROLES.build(config.getServerUrl(), config.getRealm(), appName);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public RoleRepresentation getRole(String appName, String roleName){
        String uri = URI.APP_ROLE.build(config.getServerUrl(), config.getRealm(), appName, roleName);
        return http.get(uri).getTypedResponse(RoleRepresentation.class);
    }

    public void createRole(String appName, RoleRepresentation roleRepresentation){
        String uri = URI.APP_ROLES.build(config.getServerUrl(), config.getRealm(), appName);
        try {
            http.post(uri).withBody(writeValueAsString(roleRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void updateRole(String appName, RoleRepresentation roleRepresentation){
        String uri = URI.APP_ROLE.build(config.getServerUrl(), config.getRealm(), appName, roleRepresentation.getName());
        try {
            http.put(uri).withBody(writeValueAsString(roleRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeRole(String appName, String roleName){
        String uri = URI.APP_ROLE.build(config.getServerUrl(), config.getRealm(), appName, roleName);
        http.delete(uri).execute();
    }

    public Set<RoleRepresentation> getRoleChildren(String appName, RoleRepresentation role){
        String uri = URI.APP_ROLE_COMPOSITE.build(config.getServerUrl(), config.getRealm(), appName, role.getName());
        return http.get(uri).getTypedResponse(getTypedSet(RoleRepresentation.class));
    }

    public void addChildrenToRole(String appName, RoleRepresentation compositeRole, List<RoleRepresentation> rolesToAdd){
        String uri = URI.APP_ROLE_COMPOSITE.build(config.getServerUrl(), config.getRealm(), appName, compositeRole.getName());
        try {
            http.post(uri).withBody(writeValueAsString(rolesToAdd)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeChildrenFromRole(String appName, RoleRepresentation compositeRole, List<RoleRepresentation> rolesToRemove){
        String uri = URI.APP_ROLE_COMPOSITE.build(config.getServerUrl(), config.getRealm(), appName, compositeRole.getName());
        try {
            http.delete(uri).withBody(writeValueAsString(rolesToRemove)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public Set<RoleRepresentation> getApplicationLevelRolesFromRoleComposite(String appName, RoleRepresentation roleComposite, String otherAppName){
        String uri = URI.APP_ROLE_COMPOSITE_APP.build(config.getServerUrl(), config.getRealm(), appName, roleComposite.getName(), otherAppName);
        return http.get(uri).getTypedResponse(getTypedSet(RoleRepresentation.class));
    }

    public Set<RoleRepresentation> getRealmLevelRolesFromRoleComposite(String appName, RoleRepresentation roleComposite){
        String uri = URI.APP_ROLE_COMPOSITE_REALM.build(config.getServerUrl(), config.getRealm(), appName, roleComposite.getName());
        return http.get(uri).getTypedResponse(getTypedSet(RoleRepresentation.class));
    }

    public MappingsRepresentation getScopeMappings(String appName){
        String uri = URI.APP_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), appName);
        return http.get(uri).getTypedResponse(MappingsRepresentation.class);
    }


}
