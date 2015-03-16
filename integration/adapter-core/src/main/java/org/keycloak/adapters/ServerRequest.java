package org.keycloak.adapters;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.HostUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.StreamUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServerRequest {

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
        String client_id = deployment.getResourceName();
        Map<String, String> credentials = deployment.getResourceCredentials();
        HttpClient client = deployment.getClient();
        URI uri = deployment.getLogoutUrl().clone().build();
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : credentials.entrySet()) {
            formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        HttpResponse response = null;
        HttpPost post = new HttpPost(uri);
        if (!deployment.isPublicClient()) {
            String clientSecret = credentials.get(CredentialRepresentation.SECRET);
            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(client_id, clientSecret);
                post.setHeader("Authorization", authorization);
            }
        } else {
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, client_id));
        }

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);
        response = client.execute(post);
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

    public static AccessTokenResponse invokeAccessCodeToToken(KeycloakDeployment deployment, String code, String redirectUri, String sessionId) throws HttpFailure, IOException {
        String tokenUrl = deployment.getTokenUrl();
        String client_id = deployment.getResourceName();
        Map<String, String> credentials = deployment.getResourceCredentials();
        HttpClient client = deployment.getClient();

        return invokeAccessCodeToToken(client, deployment.isPublicClient(), code, tokenUrl, redirectUri, client_id, credentials, sessionId);
    }

    public static AccessTokenResponse invokeAccessCodeToToken(HttpClient client, boolean publicClient, String code, String tokenUrl, String redirectUri, String client_id, Map<String, String> credentials, String sessionId) throws IOException, HttpFailure {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        redirectUri = stripOauthParametersFromRedirect(redirectUri);
        formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "authorization_code"));
        formparams.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        formparams.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, redirectUri));
        if (sessionId != null) {
            formparams.add(new BasicNameValuePair(AdapterConstants.APPLICATION_SESSION_STATE, sessionId));
            formparams.add(new BasicNameValuePair(AdapterConstants.APPLICATION_SESSION_HOST, HostUtils.getHostName()));
        }
        HttpResponse response = null;
        HttpPost post = new HttpPost(tokenUrl);
        if (!publicClient) {
            String clientSecret = credentials.get(CredentialRepresentation.SECRET);
            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(client_id, clientSecret);
                post.setHeader("Authorization", authorization);
            }
        } else {
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, client_id));
        }

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);
        response = client.execute(post);
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
        String tokenUrl = deployment.getTokenUrl();
        String client_id = deployment.getResourceName();
        Map<String, String> credentials = deployment.getResourceCredentials();
        HttpClient client = deployment.getClient();
        return invokeRefresh(client, deployment.isPublicClient(), refreshToken, tokenUrl, client_id, credentials);
    }


    public static AccessTokenResponse invokeRefresh(HttpClient client, boolean publicClient, String refreshToken, String tokenUrl, String client_id, Map<String, String> credentials) throws IOException, HttpFailure {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : credentials.entrySet()) {
            formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        HttpResponse response = null;
        HttpPost post = new HttpPost(tokenUrl);
        if (!publicClient) {
            String clientSecret = credentials.get(CredentialRepresentation.SECRET);
            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(client_id, clientSecret);
                post.setHeader("Authorization", authorization);
            }
        } else {
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, client_id));
        }

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);
        response = client.execute(post);
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
        String client_id = deployment.getResourceName();
        Map<String, String> credentials = deployment.getResourceCredentials();
        HttpClient client = deployment.getClient();

        invokeClientManagementRequest(client, host, registerNodeUrl, client_id, credentials);
    }

    public static void invokeUnregisterNode(KeycloakDeployment deployment, String host) throws HttpFailure, IOException {
        String unregisterNodeUrl = deployment.getUnregisterNodeUrl();
        String client_id = deployment.getResourceName();
        Map<String, String> credentials = deployment.getResourceCredentials();
        HttpClient client = deployment.getClient();

        invokeClientManagementRequest(client, host, unregisterNodeUrl, client_id, credentials);
    }

    public static void invokeClientManagementRequest(HttpClient client, String host, String endpointUrl, String clientId, Map<String, String> credentials) throws HttpFailure, IOException {
        if (endpointUrl == null) {
            throw new IOException("You need to configure URI for register/unregister node for application " + clientId);
        }

        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair(AdapterConstants.APPLICATION_CLUSTER_HOST, host));

        HttpPost post = new HttpPost(endpointUrl);

        String clientSecret = credentials.get(CredentialRepresentation.SECRET);
        if (clientSecret != null) {
            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);
        }  else {
            throw new IOException("You need to configure clientSecret for register/unregister node for application " + clientId);
        }

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);
        HttpResponse response = client.execute(post);
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
        return builder.build().toString();
    }


}
