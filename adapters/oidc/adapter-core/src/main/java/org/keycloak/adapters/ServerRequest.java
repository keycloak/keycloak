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
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.authentication.ClientCredentialsProviderUtils;
import org.keycloak.common.util.HostUtils;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServerRequest {

	private static Logger logger = Logger.getLogger(ServerRequest.class);

    public static class HttpFailure extends Exception {
        private int status;
        private String error;

        public HttpFailure(int status, String error) {
            this.status = status;
            this.error = error;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }
    }

    public static void invokeLogout(KeycloakDeployment deployment, String refreshToken) throws IOException, HttpFailure {
        HttpClient client = deployment.getClient();
        URI uri = deployment.getLogoutUrl().clone().build();
        List<NameValuePair> formparams = new ArrayList<>();

        formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        HttpPost post = new HttpPost(uri);
        ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);
        HttpResponse response = client.execute(post);
        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (status != 204) {
            error(status, entity);
        }
        if (entity == null) {
            return;
        }
        InputStream is = entity.getContent();
        if (is != null) is.close();
    }

    public static AccessTokenResponse invokeAccessCodeToToken(KeycloakDeployment deployment, String code, String redirectUri, String sessionId) throws IOException, HttpFailure {
        List<NameValuePair> formparams = new ArrayList<>();
        redirectUri = stripOauthParametersFromRedirect(redirectUri);
        formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "authorization_code"));
        formparams.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        formparams.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, redirectUri));
        if (sessionId != null) {
            formparams.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_STATE, sessionId));
            formparams.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_HOST, HostUtils.getHostName()));
        }

        HttpPost post = new HttpPost(deployment.getTokenUrl());
        ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);
        HttpResponse response = deployment.getClient().execute(post);
        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (status != 200) {
            error(status, entity);
        }
        if (entity == null) {
            throw new HttpFailure(status, null);
        }
        InputStream is = entity.getContent();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            String json = new String(bytes);
            try {
                return JsonSerialization.readValue(json, AccessTokenResponse.class);
            } catch (IOException e) {
                throw new IOException(json, e);
            }
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {

            }
        }
    }

    // https://tools.ietf.org/html/rfc7636#section-4
    public static AccessTokenResponse invokeAccessCodeToToken(KeycloakDeployment deployment, String code, String redirectUri, String sessionId, String codeVerifier) throws IOException, HttpFailure {
        List<NameValuePair> formparams = new ArrayList<>();
        redirectUri = stripOauthParametersFromRedirect(redirectUri);
        formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "authorization_code"));
        formparams.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        formparams.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, redirectUri));
        if (sessionId != null) {
            formparams.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_STATE, sessionId));
            formparams.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_HOST, HostUtils.getHostName()));
        }
        // https://tools.ietf.org/html/rfc7636#section-4
        if (codeVerifier != null) {
            logger.debugf("add to POST parameters of Token Request, codeVerifier = %s", codeVerifier);
            formparams.add(new BasicNameValuePair(OAuth2Constants.CODE_VERIFIER, codeVerifier));
        } else {
            logger.debug("add to POST parameters of Token Request without codeVerifier");
        }

        HttpPost post = new HttpPost(deployment.getTokenUrl());
        ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);
        HttpResponse response = deployment.getClient().execute(post);
        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (status != 200) {
            error(status, entity);
        }
        if (entity == null) {
            throw new HttpFailure(status, null);
        }
        InputStream is = entity.getContent();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            String json = new String(bytes);
            try {
                return JsonSerialization.readValue(json, AccessTokenResponse.class);
            } catch (IOException e) {
                throw new IOException(json, e);
            }
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {

            }
        }
    }

    public static AccessTokenResponse invokeRefresh(KeycloakDeployment deployment, String refreshToken) throws IOException, HttpFailure {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));

        HttpPost post = new HttpPost(deployment.getTokenUrl());
        ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);
        HttpResponse response = deployment.getClient().execute(post);
        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (status != 200) {
            error(status, entity);
        }
        if (entity == null) {
            throw new HttpFailure(status, null);
        }
        InputStream is = entity.getContent();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            String json = new String(bytes);
            try {
                return JsonSerialization.readValue(json, AccessTokenResponse.class);
            } catch (IOException e) {
                throw new IOException(json, e);
            }
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {

            }
        }
    }

    public static void invokeRegisterNode(KeycloakDeployment deployment, String host) throws HttpFailure, IOException {
        String registerNodeUrl = deployment.getRegisterNodeUrl();
        invokeClientManagementRequest(deployment, host, registerNodeUrl);
    }

    public static void invokeUnregisterNode(KeycloakDeployment deployment, String host) throws HttpFailure, IOException {
        String unregisterNodeUrl = deployment.getUnregisterNodeUrl();
        invokeClientManagementRequest(deployment, host, unregisterNodeUrl);
    }

    public static void invokeClientManagementRequest(KeycloakDeployment deployment, String host, String endpointUrl) throws HttpFailure, IOException {
        if (endpointUrl == null) {
            throw new IOException("You need to configure URI for register/unregister node for application " + deployment.getResourceName());
        }

        List<NameValuePair> formparams = new ArrayList<>();
        formparams.add(new BasicNameValuePair(AdapterConstants.CLIENT_CLUSTER_HOST, host));

        HttpPost post = new HttpPost(endpointUrl);
        ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);
        HttpResponse response = deployment.getClient().execute(post);
        int status = response.getStatusLine().getStatusCode();
        if (status != 204) {
            HttpEntity entity = response.getEntity();
            error(status, entity);
        }
    }

    public static void error(int status, HttpEntity entity) throws HttpFailure, IOException {
        String body = null;
        if (entity != null) {
            InputStream is = entity.getContent();
            try {
                body = StreamUtil.readString(is);
            } catch (IOException e) {

            } finally {
                try {
                    is.close();
                } catch (IOException ignored) {

                }
            }
        }
        throw new HttpFailure(status, body);
    }

    protected static String stripOauthParametersFromRedirect(String uri) {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(uri)
                .replaceQueryParam(OAuth2Constants.CODE, null)
                .replaceQueryParam(OAuth2Constants.STATE, null);
        return builder.buildAsString();
    }


}
