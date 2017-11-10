package org.keycloak.testsuite.oauth;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
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
        
    	String expectedScope = "";
    	
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }
    
    @Test
    public void specifySingleScopeAsRealmRoleTest() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "user";
    	String expectedScope = requestedScope;
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }
    
    @Test
    public void specifyMultipleScopeAsRealmRoleTest() throws Exception {
        String loginUser = "rich.roles@redhat.com";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "user realm-composite-role";
    	String expectedScope = requestedScope;
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }

    @Test
    public void specifyNotAssignedScopeAsRealmRoleTest() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "user realm-composite-role";
    	String expectedScope = "user";
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }

    @Test
    public void specifySingleScopeAsClientRoleTest() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "test-app/customer-user";
    	String expectedScope = requestedScope;
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }

    @Test
    public void specifyMultipleScopeAsClientRoleTest() throws Exception {
        String loginUser = "rich.roles@redhat.com";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "test-app-scope/test-app-disallowed-by-scope test-app-scope/test-app-allowed-by-scope";
    	String expectedScope = requestedScope;
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }

    @Test
    public void specifyNotAssignedScopeAsClientRoleTest() throws Exception {
        String loginUser = "rich.roles@redhat.com";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "test-app-scope/test-app-unspecified-by-scope test-app-scope/test-app-allowed-by-scope";
    	String expectedScope = "test-app-scope/test-app-allowed-by-scope";
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }

    @Test
    public void specifyMultipleScopeAsRealmAndClientRoleTest() throws Exception {
        String loginUser = "rich.roles@redhat.com";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "test-app-scope/test-app-disallowed-by-scope admin test-app/customer-user test-app-scope/test-app-allowed-by-scope";
    	String expectedScope = requestedScope;
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }

    @Test
    public void specifyNotAssignedScopeAsRealmAndClientRoleTest() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "test-app/customer-user test-app-scope/test-app-disallowed-by-scope admin test-app/customer-user user test-app-scope/test-app-allowed-by-scope";
    	String expectedScope = "user test-app/customer-user";
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }
 
    @Test
    public void specifyDuplicatedScopeAsRealmAndClientRoleTest() throws Exception {
        String loginUser = "john-doh@localhost";
        String loginPassword = "password";
        String clientSecret = "password";
        
    	String requestedScope = "test-app/customer-user user user test-app/customer-user";
    	String expectedScope = "user test-app/customer-user";
    	
    	oauth.scope(requestedScope);
        oauth.doLogin(loginUser, loginPassword);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(code, expectedScope, clientSecret);
    }
    
    private void expectSuccessfulResponseFromTokenEndpoint(String code, String expectedScope, String clientSecret) throws Exception {
    	OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, response.getStatusCode());
        log.info("expectedScopes = " + expectedScope);
        log.info("receivedScopes = " + response.getScope());
    	Collection<String> expectedScopes = Arrays.asList(expectedScope.split(" "));
    	Collection<String> receivedScopes = Arrays.asList(response.getScope().split(" "));
    	Assert.assertTrue(expectedScopes.containsAll(receivedScopes) && receivedScopes.containsAll(expectedScopes));
    }
}
