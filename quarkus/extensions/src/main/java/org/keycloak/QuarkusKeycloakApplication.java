package org.keycloak;

import javax.ws.rs.ApplicationPath;

import org.keycloak.services.resources.KeycloakApplication;

@ApplicationPath("/")
public class QuarkusKeycloakApplication extends KeycloakApplication {

}
