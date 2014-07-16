package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.OAuthClientService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.OAuthClientRepresentation;

import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakOAuthClientsResource {

    private final OAuthClientService oAuthClientService;

    public KeycloakOAuthClientsResource(Config config, TokenManager tokenManager){
        oAuthClientService = new OAuthClientService(config, tokenManager);
    }

    public List<OAuthClientRepresentation> findAll(){
        return oAuthClientService.findAll();
    }

    public void create(OAuthClientRepresentation oAuthClientRepresentation){
        oAuthClientService.create(oAuthClientRepresentation);
    }

    public OAuthClientRepresentation find(String oAuthClientId){
        return oAuthClientService.find(oAuthClientId);
    }

    public void update(OAuthClientRepresentation oAuthClientRepresentation){
        oAuthClientService.update(oAuthClientRepresentation);
    }

    public void remove(String oAuthClientId){
        oAuthClientService.remove(oAuthClientId);
    }

}
