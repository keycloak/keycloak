/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.authentication.ClientCredentialsProviderUtils;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

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
        if (authHeaders == null || authHeaders.isEmpty()) {
            challenge = challengeResponse(exchange, OIDCAuthenticationError.Reason.NO_AUTHORIZATION_HEADER, null, null);
            return AuthOutcome.NOT_ATTEMPTED;
        }

        tokenString = null;
        for (String authHeader : authHeaders) {
            String[] split = authHeader.trim().split("\\s+");
            if (split.length != 2) continue;
            if (!split[0].equalsIgnoreCase("Basic")) continue;
            tokenString = split[1];
        }

        if (tokenString == null) {
            challenge = challengeResponse(exchange, OIDCAuthenticationError.Reason.INVALID_TOKEN, null, null);
            return AuthOutcome.NOT_ATTEMPTED;
        }

        AccessTokenResponse atr=null;        
        try {
            String userpw=new String(Base64.decode(tokenString));
            int seperatorIndex = userpw.indexOf(":");
            String user = userpw.substring(0, seperatorIndex);
            String pw = userpw.substring(seperatorIndex + 1);
            atr = getToken(user, pw);
            tokenString = atr.getToken();
        } catch (Exception e) {
            log.debug("Failed to obtain token", e);
            challenge = challengeResponse(exchange, OIDCAuthenticationError.Reason.INVALID_TOKEN, "no_token", e.getMessage());
            return AuthOutcome.FAILED;
        }

        return authenticateToken(exchange, atr.getToken());
    } 
 
    protected AccessTokenResponse getToken(String username, String password) throws Exception {
    	AccessTokenResponse tokenResponse=null;
    	HttpClient client = deployment.getClient();

        HttpPost post = new HttpPost(deployment.getTokenUrl());
        java.util.List <NameValuePair> formparams = new java.util.ArrayList <NameValuePair>();
        formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD));
        formparams.add(new BasicNameValuePair("username", username));
        formparams.add(new BasicNameValuePair("password", password));

        ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);

        HttpResponse response = client.execute(post);
        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (status != 200) {
            EntityUtils.consumeQuietly(entity);
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
    	
    	return (tokenResponse);
    }

}
