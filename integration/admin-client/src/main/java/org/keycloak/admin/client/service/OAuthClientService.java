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

import static org.keycloak.admin.client.json.JsonSerialization.writeValueAsString;
import static org.keycloak.admin.client.utils.JsonUtils.getTypedList;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class OAuthClientService extends KeycloakService {

    public OAuthClientService(Config config, TokenManager tokenManager){
        super(config, tokenManager);
    }

    public List<OAuthClientRepresentation> findAll(){
        String uri = URI.OAUTH_CLIENTS.build(config.getServerUrl(), config.getRealm());
        return http.get(uri).getTypedResponse(getTypedList(OAuthClientRepresentation.class));
    }

    public OAuthClientRepresentation find(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        return http.get(uri).getTypedResponse(OAuthClientRepresentation.class);
    }

    public void create(OAuthClientRepresentation oAuthClientRepresentation){
        String uri = URI.OAUTH_CLIENTS.build(config.getServerUrl(), config.getRealm());
        try {
            http.post(uri).withBody(writeValueAsString(oAuthClientRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void update(OAuthClientRepresentation oAuthClientRepresentation){
        String uri = URI.OAUTH_CLIENT.build(config.getServerUrl(), config.getRealm(), oAuthClientRepresentation.getId());
        try {
            http.put(uri).withBody(writeValueAsString(oAuthClientRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void remove(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        http.delete(uri).execute();
    }

    public ClaimRepresentation getClaims(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT_CLAIMS.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        return http.get(uri).getTypedResponse(ClaimRepresentation.class);
    }

    public void updateClaims(String oAuthClientId, ClaimRepresentation claimRepresentation){
        String uri = URI.OAUTH_CLIENT_CLAIMS.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        try {
            http.put(uri).withBody(writeValueAsString(claimRepresentation)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public CredentialRepresentation generateNewSecret(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT_SECRET.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        HttpResponse response = http.post(uri).execute();
        try {
            return JsonSerialization.readValue(response.getEntity().getContent(), CredentialRepresentation.class);
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public CredentialRepresentation getSecret(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT_SECRET.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        return http.get(uri).getTypedResponse(CredentialRepresentation.class);
    }

    public String getInstallationJson(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT_INSTALLATION_JSON.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        return http.get(uri).getStringResponse();
    }

    public MappingsRepresentation getScopeMappings(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        return http.get(uri).getTypedResponse(MappingsRepresentation.class);
    }

    public List<RoleRepresentation> getApplicationLevelRoles(String oAuthClientId, String appName){
        String uri = URI.OAUTH_CLIENT_APP_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId, appName);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public void addApplicationLevelRoles(String oAuthClientId, String appName, List<RoleRepresentation> rolesToAdd){
        String uri = URI.OAUTH_CLIENT_APP_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId, appName);
        try {
            http.post(uri).withBody(writeValueAsString(rolesToAdd)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeApplicationLevelRoles(String oAuthClientId, String appName, List<RoleRepresentation> rolesToRemove){
        String uri = URI.OAUTH_CLIENT_APP_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId, appName);
        try {
            http.delete(uri).withBody(writeValueAsString(rolesToRemove)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public List<RoleRepresentation> getAvailableApplicationLevelRoles(String oAuthClientId, String appName){
        String uri = URI.OAUTH_CLIENT_AVAILABLE_APP_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId, appName);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public List<RoleRepresentation> getEffectiveApplicationLevelRoles(String oAuthClientId, String appName){
        String uri = URI.OAUTH_CLIENT_EFFECTIVE_APP_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId, appName);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public List<RoleRepresentation> getRealmLevelRoles(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT_REALM_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public void addRealmLevelRoles(String oAuthClientId, List<RoleRepresentation> rolesToAdd){
        String uri = URI.OAUTH_CLIENT_REALM_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        try {
            http.post(uri).withBody(writeValueAsString(rolesToAdd)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public void removeRealmLevelRoles(String oAuthClientId, List<RoleRepresentation> rolesToRemove){
        String uri = URI.OAUTH_CLIENT_REALM_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        try {
            http.delete(uri).withBody(writeValueAsString(rolesToRemove)).execute();
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
    }

    public List<RoleRepresentation> getAvailableRealmLevelRoles(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT_AVAILABLE_REALM_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }

    public List<RoleRepresentation> getEffectiveRealmLevelRoles(String oAuthClientId){
        String uri = URI.OAUTH_CLIENT_EFFECTIVE_REALM_LEVEL_SCOPE_MAPPINGS.build(config.getServerUrl(), config.getRealm(), oAuthClientId);
        return http.get(uri).getTypedResponse(getTypedList(RoleRepresentation.class));
    }


}
