package org.keycloak.adapters;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;

import java.util.List;

/**
 * Basic auth request authenticator.
 */
public class BasicAuthRequestAuthenticator extends BearerTokenRequestAuthenticator {
    protected Logger log = Logger.getLogger(BasicAuthRequestAuthenticator.class);
    
    public BasicAuthRequestAuthenticator(KeycloakDeployment deployment) {
    	super(deployment);
    }

    public AuthOutcome authenticate(HttpFacade exchange)  {
        List<String> authHeaders = exchange.getRequest().getHeaders("Authorization");
        if (authHeaders == null || authHeaders.size() == 0) {
            challenge = challengeResponse(exchange, null, null);
            return AuthOutcome.NOT_ATTEMPTED;
        }

        tokenString = null;
        for (String authHeader : authHeaders) {
            String[] split = authHeader.trim().split("\\s+");
            if (split == null || split.length != 2) continue;
            if (!split[0].equalsIgnoreCase("Basic")) continue;
            tokenString = split[1];
        }

        if (tokenString == null) {
            challenge = challengeResponse(exchange, null, null);
            return AuthOutcome.NOT_ATTEMPTED;
        }

        AccessTokenResponse atr=null;        
        try {
            String userpw=new String(net.iharder.Base64.decode(tokenString));
            String[] parts=userpw.split(":");
            
            atr = getToken(parts[0], parts[1]);
        } catch (Exception e) {
            log.debug("Failed to obtain token", e);
            challenge = challengeResponse(exchange, "no_token", e.getMessage());
            return AuthOutcome.FAILED;
        }

        return authenticateToken(exchange, atr.getToken());
    }
    
    private AccessTokenResponse getToken(String username, String password) throws Exception {
    	AccessTokenResponse tokenResponse=null;
    	HttpClient client = new HttpClientBuilder().disableTrustManager().build();

    	try {
    	    HttpPost post = new HttpPost(
    	            KeycloakUriBuilder.fromUri(deployment.getAuthServerBaseUrl())
    	            .path(ServiceUrlConstants.TOKEN_PATH).build(deployment.getRealm()));
    	    java.util.List <NameValuePair> formparams = new java.util.ArrayList <NameValuePair>();
    	    formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD));
    	    formparams.add(new BasicNameValuePair("username", username));
    	    formparams.add(new BasicNameValuePair("password", password));

    	    if (deployment.isPublicClient()) {
    	        formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, deployment.getResourceName()));
    	    } else {
    	        String authorization = BasicAuthHelper.createHeader(deployment.getResourceName(),
    	                deployment.getResourceCredentials().get("secret"));
    	        post.setHeader("Authorization", authorization);
    	    }

    	    UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
    	    post.setEntity(form);

    	    HttpResponse response = client.execute(post);
    	    int status = response.getStatusLine().getStatusCode();
    	    HttpEntity entity = response.getEntity();
    	    if (status != 200) {
    	        throw new java.io.IOException("Bad status: " + status);
    	    }
    	    if (entity == null) {
    	        throw new java.io.IOException("No Entity");
    	    }
    	    java.io.InputStream is = entity.getContent();
    	    try {
    	        tokenResponse = JsonSerialization.readValue(is, AccessTokenResponse.class);
    	    } finally {
    	        try {
    	            is.close();
    	        } catch (java.io.IOException ignored) { }
    	    }
    	} finally {
    	    client.getConnectionManager().shutdown();
    	}
    	
    	return (tokenResponse);
    }

}
