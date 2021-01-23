package org.keycloak.testsuite.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.OAuthClient;

//OIDC Financial API Read Only Profile : scope MUST be returned in the response from Token Endpoint
public class OAuthScopeInTokenResponseTest extends AbstractKeycloakTest {

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void specifyNoScopeTest() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String expectedScope = "openid profile email";
    	
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }
    
    @Test
    public void specifyEmptyScopeTest() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "";
    	String expectedScope = "openid profile email";
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }

    @Test
    public void failCodeNotExistingScope() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";

        ClientsResource clients = realmsResouce().realm("test").clients();
        ClientRepresentation clientRep = clients.findByClientId(oauth.getClientId()).get(0);
        ClientResource client = clients.get(clientRep.getId());
        List<ClientScopeRepresentation> scopes = client.getDefaultClientScopes();

        for (ClientScopeRepresentation scope : scopes) {
            client.removeDefaultClientScope(scope.getId());                        
        }

        oauth.openid(false);
        
        oauth.scope("user openid phone");
        oauth.openLoginForm();
        MultivaluedHashMap<String, String> queryParams = UriUtils.decodeQueryString(new URL(driver.getCurrentUrl()).getQuery());
        assertEquals("invalid_scope", queryParams.getFirst("error"));
        assertTrue(queryParams.getFirst("error_description").startsWith("Invalid scopes"));

        oauth.scope("user");
        oauth.openLoginForm();
        queryParams = UriUtils.decodeQueryString(new URL(driver.getCurrentUrl()).getQuery());
        assertEquals("invalid_scope", queryParams.getFirst("error"));
        assertTrue(queryParams.getFirst("error_description").startsWith("Invalid scopes"));

        oauth.scope("phone");
        oauth.doLogin(loginUser, loginPassword);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        expectSuccessfulResponseFromTokenEndpoint(code, "phone", clientSecret);

        oauth.openLogout();
        oauth.scope(null);
        oauth.doLogin(loginUser, loginPassword);
        code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        expectSuccessfulResponseFromTokenEndpoint(code, "", clientSecret);

        for (ClientScopeRepresentation scope : scopes) {
            client.addDefaultClientScope(scope.getId());
        }
    }

    @Test
    public void failTokenNotExistingScope() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";

        ClientsResource clients = realmsResouce().realm("test").clients();
        ClientRepresentation clientRep = clients.findByClientId(oauth.getClientId()).get(0);
        clientRep.setDirectAccessGrantsEnabled(true);
        ClientResource client = clients.get(clientRep.getId());
        client.update(clientRep);

        List<ClientScopeRepresentation> scopes = client.getDefaultClientScopes();

        for (ClientScopeRepresentation scope : scopes) {
            client.removeDefaultClientScope(scope.getId());
        }

        oauth.openid(false);
        oauth.scope("user phone");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest(clientSecret, loginUser, loginPassword);
        
        assertNotNull(response.getError());
        assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());

        oauth.scope("user");
        response = oauth.doGrantAccessTokenRequest(clientSecret, loginUser, loginPassword);

        assertNotNull(response.getError());
        assertEquals(OAuthErrorException.INVALID_SCOPE, response.getError());

        oauth.scope(null);
        response = oauth.doGrantAccessTokenRequest(clientSecret, loginUser, loginPassword);

        assertNotNull(response.getAccessToken());

        for (ClientScopeRepresentation scope : scopes) {
            client.addDefaultClientScope(scope.getId());
        }
    }
    
    @Test
    public void specifyMultipleScopeTest() throws Exception {
        String loginUser = "rich.roles@redhat.com";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "address";
    	String expectedScope = "openid profile email address";
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }

    @Test
    public void specifyMultipleExistingScopesTest() throws Exception {
        // Create client scope and add it as optional scope
        ClientScopeRepresentation userScope = new ClientScopeRepresentation();
        userScope.setName("user");
        userScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = realmsResouce().realm("test").clientScopes().create(userScope);
        String userScopeId = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(userScopeId);

        ApiUtil.findClientResourceByClientId(realmsResouce().realm("test"), "test-app").addOptionalClientScope(userScopeId);


        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";

        // Login without 'user' scope
    	String requestedScope = "address phone";
    	String expectedScope = "openid profile email address phone";
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);

        // Login with 'user' scope
        requestedScope = "user address phone";
        expectedScope = "openid profile email user address phone";

        oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);

        // Cleanup
        ApiUtil.findClientResourceByClientId(realmsResouce().realm("test"), "test-app").removeOptionalClientScope(userScopeId);
    }
    
    private void expectSuccessfulResponseFromTokenEndpoint(String code, String expectedScope, String clientSecret) throws Exception {
    	OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, response.getStatusCode());
        log.info("expectedScopes = " + expectedScope);
        log.info("receivedScopes = " + response.getScope());
    	Collection<String> expectedScopes = Arrays.asList(expectedScope.split(" "));
    	Collection<String> receivedScopes = Arrays.asList(response.getScope().split(" "));
    	Assert.assertTrue(expectedScopes.containsAll(receivedScopes) && receivedScopes.containsAll(expectedScopes));

        oauth.doLogout(response.getRefreshToken(), clientSecret);
    }
}
