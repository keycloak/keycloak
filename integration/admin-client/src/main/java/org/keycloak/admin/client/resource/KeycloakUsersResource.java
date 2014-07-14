package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.UserService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakUsersResource {

    private final UserService userService;

    public KeycloakUsersResource(Config config, TokenManager tokenManager){
        userService = new UserService(config, tokenManager);
    }

    public List<UserRepresentation> findAll(){
        return userService.findAll();
    }

    public void create(UserRepresentation userRepresentation){
        userService.create(userRepresentation);
    }

    public UserRepresentation find(String username){
        return userService.find(username);
    }

    public UserRepresentation findByEmail(String email){
        return userService.findByEmail(email);
    }

    public void update(UserRepresentation userRepresentation){
        userService.update(userRepresentation);
    }

    public void remove(UserRepresentation userRepresentation){
        userService.remove(userRepresentation);
    }

}
