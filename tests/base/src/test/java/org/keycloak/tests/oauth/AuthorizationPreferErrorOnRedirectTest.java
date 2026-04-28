package org.keycloak.tests.oauth;

import java.util.Map;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.LoginUrlBuilder;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.keycloak.OAuth2Constants.REDIRECT_URI;
import static org.keycloak.OAuthErrorException.INVALID_REQUEST;
import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.AUTHORIZATION_PREFER_ERROR_ON_REDIRECT;
import static org.keycloak.tests.common.BasicRealmWithUserConfig.PASSWORD;
import static org.keycloak.tests.common.BasicRealmWithUserConfig.USERNAME;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class AuthorizationPreferErrorOnRedirectTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectRealm(config = BasicRealmWithUserConfig.class)
    ManagedRealm realm;

    @TestSetup
    public void configureTestRealm() {
        RealmResource realmResource = realm.admin();
        RealmRepresentation realmRep = realmResource.toRepresentation();
        Map<String, String> attributes = realmRep.getAttributes();
        attributes.put(AUTHORIZATION_PREFER_ERROR_ON_REDIRECT, "true");
        realmResource.update(realmRep);
    }

    @Test
    public void testCodeFlowHappyPath() {
        var authResponse = oauth.doLogin(USERNAME, PASSWORD);
        assertNotNull(authResponse.getCode(), "No auth code");
    }

    @Test
    public void testCodeFlowInvalidRedirectUri() {
        LoginUrlBuilder loginForm = oauth.loginForm();
        loginForm.param(REDIRECT_URI, "http://invalid");
        loginForm.open();
        String errorJson = oauth.getDriver().getPageSource();
        var errorResponse = JsonSerialization.valueFromString(errorJson, OAuth2ErrorRepresentation.class);
        assertEquals(INVALID_REQUEST, errorResponse.getError());
        assertEquals("Invalid parameter: redirect_uri", errorResponse.getErrorDescription());
    }
}
