package org.keycloak.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.ServiceUrlConstants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdminClient {

    static class TypedList extends ArrayList<RoleRepresentation> {
    }

    public static class Failure extends Exception {
        private int status;

        public Failure(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    public static AccessTokenResponse getToken() throws IOException {

        HttpClient client = new HttpClientBuilder()
                .disableTrustManager().build();


        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri("http://localhost:8080/auth")
                    .path(ServiceUrlConstants.TOKEN_SERVICE_DIRECT_GRANT_PATH).build("demo"));
            List <NameValuePair> formparams = new ArrayList <NameValuePair>();
            formparams.add(new BasicNameValuePair("username", "admin"));
            formparams.add(new BasicNameValuePair("password", "password"));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "admin-client"));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);

            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                throw new IOException("Bad status: " + status);
            }
            if (entity == null) {
                throw new IOException("No Entity");
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
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public static void logout(AccessTokenResponse res) throws IOException {

        HttpClient client = new HttpClientBuilder()
                .disableTrustManager().build();


        try {
            HttpGet get = new HttpGet(KeycloakUriBuilder.fromUri("http://localhost:8080/auth")
                    .path(ServiceUrlConstants.TOKEN_SERVICE_LOGIN_PATH)
                    .queryParam("session-state", res.getSessionState())
                    .build("demo"));
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return;
            }
            InputStream is = entity.getContent();
            if (is != null) is.close();
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public static List<RoleRepresentation> getRealmRoles(AccessTokenResponse res) throws Failure {

        HttpClient client = new HttpClientBuilder()
                .disableTrustManager().build();
        try {
            HttpGet get = new HttpGet("http://localhost:8080/auth/admin/realms/demo/roles");
            get.addHeader("Authorization", "Bearer " + res.getToken());
            try {
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new Failure(response.getStatusLine().getStatusCode());
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    return JsonSerialization.readValue(is, TypedList.class);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
