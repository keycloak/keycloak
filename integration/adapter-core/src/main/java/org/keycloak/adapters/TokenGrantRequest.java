package org.keycloak.adapters;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.StreamUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenGrantRequest {

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

    public static AccessTokenResponse invoke(RealmConfiguration config, String code, String redirectUri) throws HttpFailure, IOException {
        String codeUrl = config.getCodeUrl();
        String client_id = config.getMetadata().getResourceName();
        Map<String,String> credentials = config.getResourceCredentials();
        HttpClient client = config.getClient();

        return invoke(client, code, codeUrl, redirectUri, client_id, credentials);
    }

    public static AccessTokenResponse invoke(HttpClient client, String code, String codeUrl, String redirectUri, String client_id, Map<String, String> credentials) throws IOException, HttpFailure {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        redirectUri = stripOauthParametersFromRedirect(redirectUri);
        String password = credentials.get("password");
        formparams.add(new BasicNameValuePair("grant_type", "authorization_code"));
        formparams.add(new BasicNameValuePair("code", code));
        formparams.add(new BasicNameValuePair("client_id", client_id));
        formparams.add(new BasicNameValuePair(CredentialRepresentation.PASSWORD, password));
        formparams.add(new BasicNameValuePair("redirect_uri", redirectUri));
        HttpResponse response = null;
        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        HttpPost post = new HttpPost(codeUrl);
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


    protected static void error(int status, HttpEntity entity) throws HttpFailure, IOException {
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
                .replaceQueryParam("code", null)
                .replaceQueryParam("state", null);
        return builder.build().toString();
    }



}
