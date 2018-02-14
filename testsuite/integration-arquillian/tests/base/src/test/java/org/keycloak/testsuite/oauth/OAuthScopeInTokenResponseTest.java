package org.keycloak.testsuite.oauth;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
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
    public void specifySingleNotExistingScopeTest() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "user";
    	String expectedScope = "openid profile email";
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }
    
    @Test
    public void specifyMultipleScopeTest() throws Exception {
        String loginUser = "rich.roles@redhat.com";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "user address";
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
